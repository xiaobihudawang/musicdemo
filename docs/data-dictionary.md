# 数据字典

> 数据库：`music_platform` (MySQL 8.0)

---

## 1. user — 用户表

| 字段名 | 类型 | 长度 | 可空 | 默认值 | 主键 | 外键 | 约束 | 说明 |
|--------|------|:----:|:----:|:------:|:----:|:----:|:----:|------|
| id | INT | | NO | | PK | | AUTO_INCREMENT | 用户ID，自增主键 |
| username | VARCHAR | 255 | NO | | | | UNIQUE | 用户名，唯一 |
| password | VARCHAR | 255 | NO | | | | | 密码（BCrypt加密） |
| name | VARCHAR | 255 | YES | NULL | | | | 昵称（显示名称） |
| email | VARCHAR | 255 | YES | NULL | | | | 电子邮箱 |
| role | VARCHAR | 50 | NO | 'user' | | | CHECK (role IN ('user','admin')) | 角色：user=普通用户，admin=管理员 |
| enabled | TINYINT | 1 | YES | 1 | | | CHECK (enabled IN (0,1)) | 是否启用：1=启用，0=禁用 |
| create_time | DATETIME | | YES | CURRENT_TIMESTAMP | | | | 注册时间 |

---

## 2. music — 音乐表

| 字段名 | 类型 | 长度 | 可空 | 默认值 | 主键 | 外键 | 约束 | 说明 |
|--------|------|:----:|:----:|:------:|:----:|:----:|:----:|------|
| id | INT | | NO | | PK | | AUTO_INCREMENT | 音乐ID，自增主键 |
| title | VARCHAR | 255 | NO | | | | | 歌曲标题 |
| artist | VARCHAR | 255 | NO | | | | | 歌手名 |
| description | VARCHAR | 2000 | YES | NULL | | | | 简介描述 |
| file_path | VARCHAR | 500 | NO | | | | | 音频文件存储路径 |
| file_size | BIGINT | | YES | 0 | | | CHECK (file_size >= 0) | 文件大小（字节） |
| cover_path | VARCHAR | 500 | YES | NULL | | | | 封面图片路径 |
| lyrics | TEXT | | YES | NULL | | | | LRC格式歌词文本 |
| user_id | INT | | NO | | | FK → user(id) | | 上传者ID |
| create_time | DATETIME | | YES | CURRENT_TIMESTAMP | | | | 上传时间 |
| like_count | INT | | YES | 0 | | | CHECK (like_count >= 0) | 点赞数（冗余，触发器维护） |
| comment_count | INT | | YES | 0 | | | CHECK (comment_count >= 0) | 评论数（冗余，触发器维护） |
| download_count | INT | | YES | 0 | | | CHECK (download_count >= 0) | 下载数（冗余，触发器维护） |

---

## 3. comment — 评论表

| 字段名 | 类型 | 长度 | 可空 | 默认值 | 主键 | 外键 | 约束 | 说明 |
|--------|------|:----:|:----:|:------:|:----:|:----:|:----:|------|
| id | INT | | NO | | PK | | AUTO_INCREMENT | 评论ID，自增主键 |
| content | VARCHAR | 2000 | NO | | | | | 评论内容 |
| user_id | INT | | NO | | | FK → user(id) | | 评论者ID |
| music_id | INT | | NO | | | FK → music(id) ON DELETE CASCADE | | 被评论音乐ID，删除音乐时级联删除 |
| create_time | DATETIME | | YES | CURRENT_TIMESTAMP | | | | 评论时间 |

---

## 4. like_record — 点赞记录表

| 字段名 | 类型 | 长度 | 可空 | 默认值 | 主键 | 外键 | 约束 | 说明 |
|--------|------|:----:|:----:|:------:|:----:|:----:|:----:|------|
| id | INT | | NO | | PK | | AUTO_INCREMENT | 记录ID，自增主键 |
| user_id | INT | | NO | | | FK → user(id) | | 点赞者ID |
| music_id | INT | | NO | | | FK → music(id) ON DELETE CASCADE | | 被点赞音乐ID，删除音乐时级联删除 |
| create_time | DATETIME | | YES | CURRENT_TIMESTAMP | | | | 点赞时间 |
| | | | | | | | UNIQUE KEY uk_user_music (user_id, music_id) | 防止重复点赞 |

---

## 5. download_record — 下载记录表

| 字段名 | 类型 | 长度 | 可空 | 默认值 | 主键 | 外键 | 约束 | 说明 |
|--------|------|:----:|:----:|:------:|:----:|:----:|:----:|------|
| id | INT | | NO | | PK | | AUTO_INCREMENT | 记录ID，自增主键 |
| user_id | INT | | NO | | | FK → user(id) | | 下载者ID |
| music_id | INT | | NO | | | FK → music(id) ON DELETE CASCADE | | 被下载音乐ID，删除音乐时级联删除 |
| create_time | DATETIME | | YES | CURRENT_TIMESTAMP | | | | 下载时间 |

---

## 6. 索引

| 索引名 | 表 | 字段 | 类型 | 说明 |
|--------|:---:|:----:|:----:|------|
| idx_music_title | music | title | BTREE | 加速标题搜索 |
| idx_music_artist | music | artist | BTREE | 加速歌手搜索 |
| idx_comment_music_id | comment | music_id, create_time | BTREE（复合） | 加速音乐评论查询 |
| idx_like_music_id | like_record | music_id | BTREE | 加速音乐点赞统计 |
| idx_download_music_id | download_record | music_id | BTREE | 加速音乐下载统计 |
| uk_user_music | like_record | user_id, music_id | UNIQUE | 防止重复点赞 |

---

## 7. 视图

| 视图名 | 用途 |
|--------|------|
| v_music_leaderboard | 音乐排行榜视图：含上传者用户名、实时点赞/评论/下载计数 |

---

## 8. 存储过程

| 过程名 | 参数 | 用途 |
|--------|:----:|------|
| sp_batch_delete_user | IN p_user_id INT | 级联删除用户及其所有关联数据（事务保护） |
| sp_get_leaderboard | IN p_type VARCHAR, IN p_limit INT | 按类型(like/comment/download)获取 TopN 排行榜 |
| sp_recalc_all_counts | 无 | 使用游标遍历所有音乐，重新统计并校正点赞/评论/下载计数 |

---

## 9. 触发器

| 触发器名 | 表 | 事件 | 动作 | 用途 |
|----------|:---:|:----:|:----:|------|
| trg_like_insert | like_record | AFTER INSERT | UPDATE music.like_count + 1 | 点赞时增加计数 |
| trg_like_delete | like_record | AFTER DELETE | UPDATE music.like_count - 1 | 取消点赞时减少计数 |
| trg_comment_before_insert | comment | BEFORE INSERT | 校验 content 非空且长度 ≤ 2000 | 评论内容有效性检查 |
| trg_comment_insert | comment | AFTER INSERT | UPDATE music.comment_count + 1 | 评论时增加计数 |
| trg_comment_delete | comment | AFTER DELETE | UPDATE music.comment_count - 1 | 删除评论时减少计数 |
| trg_download_insert | download_record | AFTER INSERT | UPDATE music.download_count + 1 | 下载时增加计数 |
| trg_download_delete | download_record | AFTER DELETE | UPDATE music.download_count - 1 | 删除下载记录时减少计数 |

---

## 10. 事件

| 事件名 | 调度 | 用途 |
|--------|:----:|------|
| e_cleanup_old_downloads | 每月 1 号 02:00 | 清理一年前的下载记录，控制数据膨胀 |

---

## 11. 函数

| 函数名 | 参数 | 返回 | 用途 |
|--------|:----:|:----:|------|
| fn_music_hot_score | p_music_id INT | INT | 按点赞×3+评论×2+下载×1 计算音乐热度分 |
