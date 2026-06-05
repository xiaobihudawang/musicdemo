-- ================================================================
-- 数据库 Schema — 音乐分享平台（music_platform）
-- 本文件定义了全部 5 张数据表的 DDL 结构
--
-- 表清单：
--   1. user            — 用户表（注册用户信息）
--   2. music           — 音乐表（上传的音乐元信息）
--   3. comment         — 评论表（用户对音乐的评论）
--   4. like_record     — 点赞记录表（谁赞了哪首歌）
--   5. download_record — 下载记录表（谁下载了哪首歌）
--
-- 注：本 schema 由 MySQL 8.0 使用，sql.init.mode=never 不会自动执行
-- ================================================================

-- ============================================================
-- 表 1：user（用户表）
--
-- 字段说明：
--   id            - 主键，自增整数
--   username      - 用户名，唯一约束（UNIQUE），不允许重复
--   password      - 密码，使用 BCrypt 算法加密后存储
--   name          - 昵称（显示名称），可选，不填时与 username 相同
--   email         - 邮箱，可选
--   role          - 角色：'user' 普通用户 / 'admin' 管理员，默认 user
--   enabled       - 是否启用：1=启用（正常使用），0=禁用（无法登录）
--   totp_secret   - TOTP 密钥（Base32 编码），用于生成/验证动态验证码
--   totp_enabled  - 是否已启用 TOTP 两步验证：1=已启用，0=未启用
--   create_time   - 注册时间，默认当前时间
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`            INT AUTO_INCREMENT PRIMARY KEY,
    `username`      VARCHAR(255) NOT NULL UNIQUE,
    `password`      VARCHAR(255) NOT NULL,
    `name`          VARCHAR(255),
    `email`         VARCHAR(255),
    `role`          VARCHAR(50) NOT NULL DEFAULT 'user',
    `enabled`       TINYINT(1) DEFAULT 1,
    `totp_secret`   VARCHAR(255),
    `totp_enabled`  TINYINT(1) DEFAULT 0,
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 表 2：music（音乐表）
--
-- 字段说明：
--   id             - 主键，自增整数
--   title          - 歌曲标题（必填）
--   artist         - 歌手名（必填）
--   description    - 简介描述，最长 2000 字符
--   file_path      - 文件存储路径（相对于 music.file-path 或绝对路径）
--   file_size      - 文件大小（字节数）
--   like_count     - 点赞数（冗余字段，通过 like_record 统计后更新）
--   comment_count  - 评论数（冗余字段，通过 comment 统计后更新）
--   download_count - 下载数（冗余字段，通过 download_record 统计后更新）
--   user_id        - 上传者 ID，外键关联 user(id)
--   create_time    - 上传时间
--
-- 冗余字段说明：
--   like_count / comment_count / download_count 是反范式设计，
--   目的是避免每次查询都需要 JOIN 或 COUNT 子查询，提升列表页性能。
--   通过点赞/评论/下载操作的 Service 层同步更新这些计数。
-- ============================================================
CREATE TABLE IF NOT EXISTS `music` (
    `id`             INT AUTO_INCREMENT PRIMARY KEY,
    `title`          VARCHAR(255) NOT NULL,
    `artist`         VARCHAR(255) NOT NULL,
    `description`    VARCHAR(2000),
    `file_path`      VARCHAR(500) NOT NULL,
    `file_size`      BIGINT DEFAULT 0,
    `cover_path`     VARCHAR(500) DEFAULT NULL COMMENT '封面图片路径',
    `lyrics`         TEXT DEFAULT NULL COMMENT 'LRC格式歌词',
    `user_id`        INT NOT NULL,
    `create_time`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- ============================================================
-- 表 3：comment（评论表）
--
-- 字段说明：
--   id          - 主键，自增整数
--   content     - 评论内容，最长 2000 字符
--   user_id     - 评论者 ID，外键关联 user(id)
--   music_id    - 被评论的音乐 ID，外键关联 music(id)
--   create_time - 评论时间
--
-- 删除音乐时，级联删除关联的评论由应用层处理
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `content`     VARCHAR(2000) NOT NULL,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
);

-- ============================================================
-- 表 4：like_record（点赞记录表）
--
-- 功能：实现 Toggle 点赞的核心数据表
--   - 用户点赞：插入一条记录
--   - 取消点赞：删除对应记录
--   - 判断是否已赞：查询是否存在 user_id + music_id 的记录
--
-- 唯一约束 uk_user_music：
--   确保同一个用户对同一首歌最多只有一条点赞记录，
--   防止重复点赞。这是 Toggle 点赞机制的基础。
--
-- 关联的冗余字段：music.like_count 通过本表统计后更新
-- ============================================================
CREATE TABLE IF NOT EXISTS `like_record` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_music` (`user_id`, `music_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
);

-- ============================================================
-- 表 5：download_record（下载记录表）
--
-- 与点赞记录不同，下载记录不设唯一约束，
-- 每次用户下载都记录一条新的日志行，
-- 可用于统计下载频次、用户行为分析等。
--
-- 字段说明：
--   id          - 主键，自增整数
--   user_id     - 下载者 ID，外键关联 user(id)
--   music_id    - 被下载的音乐 ID，外键关联 music(id)
--   create_time - 下载时间
-- ============================================================
CREATE TABLE IF NOT EXISTS `download_record` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
);