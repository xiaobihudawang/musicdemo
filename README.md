# 音乐分享平台 (MusicDemo)

一个基于 **Spring Boot 3.3.6 + MyBatis + Spring Security + JWT** 的全栈音乐分享平台。纯 HTML/CSS/JS 前端。

## 课设评分对应

| 评分项 | 分值 | 实现方式 |
|--------|:----:|----------|
| E-R 图 | 5 | `diagrams/er-diagram.puml` / `diagrams/er-diagram.mmd` |
| 数据字典 | 5 | `docs/data-dictionary.md` |
| 完整性约束 | 20 | PK/FK/UNIQUE/NOT NULL/DEFAULT + **CHECK** (role, enabled, file_size, counter) |
| 视图 | 5 | `v_admin_stats` + `v_music_leaderboard` — 管理员统计 + 排行榜视图 |
| 存储过程 | 5 | `sp_batch_delete_user`、`sp_get_leaderboard`、`sp_recalc_all_counts` |
| 触发器 | 5 | 7 个触发器维护 counter 列一致性 + 评论内容校验 |
| 事件 | 5 | `e_cleanup_old_downloads` — 每月清理一年前下载记录 |
| 索引 | 5 | 5 个 BTREE 索引（含 1 个复合索引） |
| 架构 | 5 | Spring Boot + MyBatis 分层架构 |
| 界面 | 3 | 纯 HTML/CSS/JS，纸质感设计 |
| 代码 | 12 | 命名规范，关键逻辑注释 |
| 运行部署 | 5 | 本 README + `docs/blackbox-test.md` |

---

## 数据库高级特性

| 特性 | 数量 | 说明 |
|------|:----:|------|
| CHECK 约束 | 6 | role, enabled, file_size, 3 个 counter 列的域完整性 |
| 索引 | 5 | 含 1 个复合索引 (`comment(music_id, create_time)`) |
| 视图 | 2 | `v_admin_stats` + `v_music_leaderboard` |
| 存储过程 | 3 | `sp_batch_delete_user`(级联删除)、`sp_get_leaderboard`(排行榜)、`sp_recalc_all_counts`(游标校正计数) |
| 触发器 | 7 | 3 对 INSERT/DELETE 维护 counter + `trg_comment_before_insert` 校验评论 |
| 事件 | 1 | `e_cleanup_old_downloads` — 每月清理一年前下载记录 |

详见 `docs/data-dictionary.md`。

## 运行测试

```bash
mvnw.cmd test
```

共 **39** 个单元测试，覆盖全部 5 个 Service 层（User/Music/Comment/Like/Ranking）及敏感词服务。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.3.6, Spring Security 6, MyBatis 3.0.4 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT (jjwt 0.12.6), BCrypt 密码加密 |
| 前端 | 纯 HTML/CSS/JS (无框架), Cormorant Garamond + Noto Serif SC 衬线字体 |
| 外部集成 | Python 子进程 (bilibili_demo.py + netease_search.py), DeepSeek AI |
| 构建 | Maven (mvnw wrapper), Log4j2 日志 |

---

## 快速开始

### 前置条件

- JDK 17+
- MySQL 8.0
- Python 3 (for Bilibili downloader & NetEase search)

### 1. 创建数据库并导入 Schema

```sql
CREATE DATABASE IF NOT EXISTS music_platform DEFAULT CHARACTER SET utf8mb4;
USE music_platform;
SOURCE src/main/resources/schema.sql;   -- 自动建 5 张表 + 索引 + 视图 + 存储过程 + 触发器 + 事件
```

5 张表：`user`, `music`, `comment`, `like_record`, `download_record`。

启用事件调度器（可选，用于自动清理过期下载记录）：

```sql
SET GLOBAL event_scheduler = ON;
```

### 2. 配置 `application.yml`

编辑 `src/main/resources/application.yml`：
- 修改 `spring.datasource` 的 MySQL 连接信息（用户名/密码）
- 确认 `music.file-path`（音乐文件存储路径，默认 `D:/workspace/music/`）
- 可选：配置 `ai.deepseek.api-key`（用于 AI 生成音乐简介，从环境变量 `ANTHROPIC_AUTH_TOKEN` 读取）

### 3. 启动

```bash
mvnw.cmd spring-boot:run
```

访问 `http://localhost:8443`

### 构建 JAR

```bash
mvnw.cmd clean package
java -jar target/musicdemo-0.0.1-SNAPSHOT.jar
```

---

## 项目结构

```
musicdemo/
├── src/main/java/org/example/musicdemo/
│   ├── common/              # 通用工具
│   │   └── Result.java          统一 API 响应格式 {code, message, data}
│   │   └── ResultCode.java      状态码枚举 (200/400/401/403/404/500)
│   ├── config/              # 配置类
│   │   ├── JwtUtils.java        JWT 签发/解析/验签
│   │   ├── JwtAuthenticationFilter.java  JWT 请求过滤
│   │   ├── SecurityConfig.java  Spring Security 规则
│   │   └── WebConfig.java      静态资源映射
│   ├── controller/          # REST 控制器 (8 个)
│   │   ├── AuthController.java       POST /api/auth/login, /register
│   │   ├── MusicController.java      GET/POST/DELETE /api/music/**
│   │   ├── CommentController.java    GET/DELETE /api/music/{id}/comments
│   │   ├── LikeController.java       POST/DELETE /api/like/**
│   │   ├── RankingController.java    GET /api/ranking/**
│   │   ├── AdminController.java      GET/PUT/DELETE /api/admin/**
│   │   ├── BilibiliController.java   POST /api/bilibili/download
│   │   └── AiController.java         POST /api/ai/description
│   ├── entity/              # 实体类 (5 个)
│   │   ├── User.java
│   │   ├── Music.java             含 coverPath 封面字段
│   │   ├── Comment.java
│   │   ├── LikeRecord.java
│   │   └── DownloadRecord.java
│   ├── mapper/              # MyBatis Mapper 接口 (5 个)
│   └── service/             # 服务层 (10 个)
│       ├── UserService.java
│       ├── MusicService.java       上传自动获取封面
│       ├── CommentService.java
│       ├── LikeService.java
│       ├── RankingService.java
│       ├── CoverService.java       多源封面搜索下载
│       ├── LyricsService.java      歌词解析服务
│       ├── SensitiveWordService.java  DFA 敏感词检测
│       ├── BilibiliService.java     Python 子进程
│       └── AiService.java          DeepSeek API 调用
├── src/main/resources/
│   ├── application.yml        主配置
│   ├── schema.sql             数据库 DDL (5 张业务表)
│   ├── mapper/                5 个 Mapper XML
│   └── static/                前端页面
│       ├── index.html             首页 (歌单 + 搜索 + 分页)
│       ├── detail.html            详情 (播放器 + 封面 + 评论)
│       ├── ranking.html           排行榜 (点赞/下载/评论 3 标签)
│       ├── upload.html            上传 (含 AI 描述生成)
│       ├── bilibili.html          B 站音频下载
│       ├── login.html / register.html  登录/注册
│       ├── admin/users.html / music.html  管理后台
│       ├── css/
│       │   ├── base.css           重置 / 变量 / 基础排版
│       │   ├── layout.css         页面级布局（navbar / 容器 / 分页）
│       │   ├── components.css     通用组件（btn / card / form / toast / skeleton / track-list / rank / table / file-upload / comment / tabs）
│       │   └── player.css         详情页播放器（player-* / lyrics-* / btn-player / comments-section）
│       └── js/
│           ├── common.js          认证 / 提示 / 格式化 / navbar / loading
│           ├── api.js             get/post/put/del/postForm
│           ├── app.js             DOMContentLoaded 入口
│           └── pages/             各页面逻辑 (index/login/register/detail/upload/ranking/bilibili/admin)
├── scripts/                 # 辅助脚本
│   ├── bilibili_demo.py         B站音频下载 (Python)
│   └── netease_search.py        网易云搜索/封面/歌词 (Python)
├── docs/                    # 项目文档
│   ├── data-dictionary.md       数据字典
│   ├── data-flow-diagram.md     数据流图（DFD）
│   ├── function-module-diagram.md  系统功能模块图
│   ├── blackbox-test.md         黑盒测试用例
│   ├── performance-test.md      性能测试报告
│   ├── defense-keypoints.md     答辩要点整理
│   ├── 课程设计报告.md            课程设计报告
│   ├── 课程设计日记.md            课程设计日记
│   ├── 答辩记录表.md              答辩记录表
│   ├── backup-and-restore.md    数据备份与恢复
│   └── frontend-tutorial.md     前端开发教学
└── .gitignore
```

---

## API 总览

### 认证 (`/api/auth`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | 公开 |
| GET | `/api/auth/verify` | 验证 Token | 公开 |
| POST | `/api/auth/register` | 注册 | 公开 |

### 音乐 (`/api/music`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/music/list?page=&size=&keyword=` | 分页列表+搜索 | 公开 |
| GET | `/api/music/{id}` | 详情 | 公开 |
| POST | `/api/music/upload` | 上传 (multipart) | 登录 |
| GET | `/api/music/{id}/stream` | 在线播放 | 公开 |
| GET | `/api/music/{id}/download` | 下载+计数 | 登录 |
| GET | `/api/music/{id}/lyrics` | 歌词 | 公开 |
| POST | `/api/music/{id}/lyrics/regenerate` | 重新获取歌词 | 登录 |
| DELETE | `/api/music/{id}` | 删除 (本人或管理员) | 登录 |

### 评论 (`/api/music/{id}/comments`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/music/{id}/comments` | 评论列表 | 登录 |
| POST | `/api/music/{id}/comments` | 发表评论 | 登录 |
| DELETE | `/api/comments/{id}` | 删除评论 | 登录 |

### 点赞 (`/api/music/{id}/like`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/music/{id}/like` | 切换点赞/取消 | 登录 |
| GET | `/api/music/{id}/like/status` | 是否已赞 | 登录 |

### 排行榜 (`/api/ranking`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/ranking/likes` | 点赞周榜 Top10 | 公开 |
| GET | `/api/ranking/downloads` | 下载周榜 Top10 | 公开 |
| GET | `/api/ranking/comments` | 评论周榜 Top10 | 公开 |

### 管理 (`/api/admin`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/stats` | 平台总览统计 | ADMIN |
| GET | `/api/admin/users` | 用户列表 | ADMIN |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用 | ADMIN |
| DELETE | `/api/admin/users/{id}` | 级联删除用户 | ADMIN |
| DELETE | `/api/admin/music/{id}` | 删除任意音乐 | ADMIN |
| DELETE | `/api/admin/comments/{id}` | 删除任意评论 | ADMIN |
| POST | `/api/admin/music/{id}/cover` | 上传/替换封面 | ADMIN |

### 其他
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/bilibili/download` | B站音频下载 | 登录 |
| POST | `/api/ai/description` | AI 生成音乐简介 | 登录 |

---

## 认证与权限

- **认证方式**: JWT Bearer Token, 存储于 `localStorage` key `music_token`
- **密码**: BCrypt 哈希存储
- **角色层级**: 游客 → USER (普通用户) → ADMIN (管理员)
- **安全规则**:
  - `/api/auth/**` — 公开
  - GET `/api/music/**` — 公开 (浏览/播放)
  - GET `/api/ranking/**` — 公开
  - `/api/music/file/**`, `/api/music/cover/**` — 公开 (文件/封面直链)
  - `/api/admin/**` — 仅 `ROLE_ADMIN`
  - 其他 `/api/**` — 需登录

---

## 前端设计

Anthropic 纸质感风格 (答辩简化版，无动画/过渡效果):
- **底色** `#f5f0e6` + SVG 噪点纹理 (`feTurbulence opacity: 0.03`)
- **字体** `Cormorant Garamond` (英文衬线) + `Noto Serif SC` (中文衬线)
- **文字色** `#2c2821` 深棕墨水色
- **强调色** `#8b6914` 琥珀金
- **阴影** 暖棕柔影 `rgba(44,40,33, 0.06~0.10)`
- **布局** 800px 窄版居中，大量留白
- **水印** 右下角固定 `MUSIC` 文字，`opacity: 0.025`
- **卡片** `#faf8f5` 暖白表面，`#e0d8cc` 边框
- **简化** 已移除所有 CSS 动画 (keyframes)、过渡效果 (transition)、骨架屏 shimmer 动画、加载遮罩淡出效果

---

## 外部集成

### 网易云音乐搜索 (NetEase CloudSearch)
- 通过 `netease_search.py` Python 脚本搜索网易云音乐
- 支持搜索、封面获取、歌词获取
- 通过 `ProcessBuilder` 以子进程方式调用

### 封面自动搜索 (CoverService)
- 上传音乐时自动获取封面图
- 优先使用 NetEase CloudSearch (`netease_search.py`)，6 个备用源兜底
- 下载至 `covers/{uuid}.jpg`，路径存入 `music.cover_path`
- WebConfig 将 `/api/music/cover/**` 映射至文件系统

### Bilibili 音频下载
- `scripts/bilibili_demo.py` Python 脚本
- `BilibiliService` 通过 `ProcessBuilder` 调用，下载音频文件

### AI 简介生成
- 对接 DeepSeek API，根据歌名+歌手自动生成音乐简介
- API Key 从环境变量 `ANTHROPIC_AUTH_TOKEN` 读取（`application.yml` 中配置）

---

## 注意事项

- **数据库**: `sql.init.mode=never`，需手动执行 `schema.sql`（含全部 5 表 + 索引 + 视图 + 存储过程 + 触发器）
- **JWT 过期**: 默认 259200000ms (72 小时)，可在 `application.yml` 调整
- **NetEase**: `/api/cloudsearch/pc` (unencrypted, POST with User-Agent + Referer) 可用于搜索和封面获取；`bootstrap_track` 返回空 URL (反爬)，不可用
