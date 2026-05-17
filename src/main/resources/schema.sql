-- =====================================================
-- 用户表：存储注册用户信息
-- =====================================================
CREATE TABLE IF NOT EXISTS `user` (
                                      `id`          INT AUTO_INCREMENT PRIMARY KEY,       -- 主键，自增
                                      `username`    VARCHAR(255) NOT NULL UNIQUE,          -- 用户名，唯一
    `password`    VARCHAR(255) NOT NULL,                 -- BCrypt 加密后的密码
    `name`        VARCHAR(255),                          -- 昵称（可选）
    `email`       VARCHAR(255),                          -- 邮箱（可选）
    `role`        VARCHAR(50) NOT NULL DEFAULT 'user',   -- 角色：user 普通用户 / admin 管理员
    `enabled`     TINYINT(1) DEFAULT 1,                  -- 是否启用：1启用 0禁用
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP     -- 创建时间
    );

-- =====================================================
-- 音乐表：存储上传的音乐信息
-- =====================================================
CREATE TABLE IF NOT EXISTS `music` (
                                       `id`             INT AUTO_INCREMENT PRIMARY KEY,
                                       `title`          VARCHAR(255) NOT NULL,               -- 歌曲标题
    `artist`         VARCHAR(255) NOT NULL,               -- 歌手名
    `description`    VARCHAR(2000),                       -- 简介（最长 2000 字）
    `file_path`      VARCHAR(500) NOT NULL,               -- 文件在硬盘上的路径/文件名
    `file_size`      BIGINT DEFAULT 0,                    -- 文件大小（字节）
    `like_count`     INT DEFAULT 0,                       -- 点赞数（冗余字段，方便查询）
    `comment_count`  INT DEFAULT 0,                       -- 评论数（冗余字段）
    `download_count` INT DEFAULT 0,                       -- 下载数（冗余字段）
    `user_id`        INT NOT NULL,                        -- 上传者 ID
    `create_time`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)       -- 外键：关联用户表
    );

-- =====================================================
-- 评论表
-- =====================================================
CREATE TABLE IF NOT EXISTS `comment` (
                                         `id`          INT AUTO_INCREMENT PRIMARY KEY,
                                         `content`     VARCHAR(2000) NOT NULL,                 -- 评论内容
    `user_id`     INT NOT NULL,                           -- 评论者 ID
    `music_id`    INT NOT NULL,                           -- 被评论的音乐 ID
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`)
    );

-- =====================================================
-- 点赞记录表：记录谁给哪首歌点了赞
-- =====================================================
CREATE TABLE IF NOT EXISTS `like_record` (
                                             `id`          INT AUTO_INCREMENT PRIMARY KEY,
                                             `user_id`     INT NOT NULL,
                                             `music_id`    INT NOT NULL,
                                             `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- 唯一约束：同一个用户对同一首歌只能有一条记录
    -- 这就是 Toggle 点赞的核心：有记录=已赞，没记录=未赞
                                             UNIQUE KEY `uk_user_music` (`user_id`, `music_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`)
    );

-- =====================================================
-- 下载记录表（不设唯一约束，每次下载都记录一条）
-- =====================================================
CREATE TABLE IF NOT EXISTS `download_record` (
                                                 `id`          INT AUTO_INCREMENT PRIMARY KEY,
                                                 `user_id`     INT NOT NULL,
                                                 `music_id`    INT NOT NULL,
                                                 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                 FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`)
    );