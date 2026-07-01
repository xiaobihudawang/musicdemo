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
--   create_time   - 注册时间，默认当前时间
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`            INT AUTO_INCREMENT PRIMARY KEY,
    `username`      VARCHAR(255) NOT NULL UNIQUE,
    `password`      VARCHAR(255) NOT NULL,
    `name`          VARCHAR(255),
    `email`         VARCHAR(255),
    `role`          VARCHAR(50) NOT NULL DEFAULT 'user' CHECK (`role` IN ('user', 'admin')),
    `enabled`       TINYINT(1) DEFAULT 1 CHECK (`enabled` IN (0, 1)),
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
--   这些计数由数据库触发器在插入/删除关联记录时自动维护。
-- ============================================================
CREATE TABLE IF NOT EXISTS `music` (
    `id`             INT AUTO_INCREMENT PRIMARY KEY,
    `title`          VARCHAR(255) NOT NULL,
    `artist`         VARCHAR(255) NOT NULL,
    `description`    VARCHAR(2000),
    `file_path`      VARCHAR(500) NOT NULL,
    `file_size`      BIGINT DEFAULT 0 CHECK (`file_size` >= 0),
    `cover_path`     VARCHAR(500) DEFAULT NULL COMMENT '封面图片路径',
    `lyrics`         TEXT DEFAULT NULL COMMENT 'LRC格式歌词',
    `user_id`        INT NOT NULL,
    `create_time`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    `like_count`     INT DEFAULT 0 COMMENT '冗余字段：点赞数，由触发器自动维护' CHECK (`like_count` >= 0),
    `comment_count`  INT DEFAULT 0 COMMENT '冗余字段：评论数，由触发器自动维护' CHECK (`comment_count` >= 0),
    `download_count` INT DEFAULT 0 COMMENT '冗余字段：下载数，由触发器自动维护' CHECK (`download_count` >= 0),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
-- 删除音乐时，级联删除其下的所有评论、点赞和下载记录由数据库外键 ON DELETE CASCADE 自动处理
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `content`     VARCHAR(2000) NOT NULL,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 表 5：download_record（下载记录表）
--
-- 功能：记录用户的下载行为，用于排行榜统计
-- 关联的冗余字段：music.download_count 由触发器自动维护
-- ============================================================
CREATE TABLE IF NOT EXISTS `download_record` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 存储过程：级联删除用户及其所有关联数据
-- 用途：管理员删除用户时，同时清理该用户的评论、点赞、
--       下载记录和上传的音乐（含关联的评论/点赞/下载记录）
-- ================================================================
DROP PROCEDURE IF EXISTS sp_batch_delete_user;
DELIMITER //
CREATE PROCEDURE sp_batch_delete_user(IN p_user_id INT)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION ROLLBACK;
    START TRANSACTION;

    -- 1. 删除该用户的点赞记录
    DELETE FROM like_record WHERE user_id = p_user_id;
    -- 2. 删除该用户的下载记录
    DELETE FROM download_record WHERE user_id = p_user_id;
    -- 3. 删除该用户的评论
    DELETE FROM comment WHERE user_id = p_user_id;
    -- 4. 删除该用户的音乐（CASCADE 自动清理关联的评论/点赞/下载）
    DELETE FROM music WHERE user_id = p_user_id;
    -- 5. 删除用户本身
    DELETE FROM `user` WHERE id = p_user_id;

    COMMIT;
END //
DELIMITER ;

-- ================================================================
-- 事件：定期清理过期下载记录
-- 作用：每月 1 号凌晨自动清理一年前的下载记录，控制日志表膨胀
-- 启用：SET GLOBAL event_scheduler = ON;
-- ================================================================
DROP EVENT IF EXISTS e_cleanup_old_downloads;
DELIMITER //
CREATE EVENT e_cleanup_old_downloads
ON SCHEDULE EVERY 1 MONTH STARTS '2026-08-01 02:00:00'
DO
BEGIN
    DELETE FROM download_record
    WHERE create_time < DATE_SUB(NOW(), INTERVAL 1 YEAR);
END //
DELIMITER ;

-- ================================================================
-- 索引 + 触发器 — 实际有用的数据库特性
-- ================================================================

-- 索引：加速 JOIN 和搜索（互联网项目标配）
CREATE INDEX idx_music_title ON `music`(`title`);
CREATE INDEX idx_music_artist ON `music`(`artist`);
CREATE INDEX idx_comment_music_id ON `comment`(`music_id`, `create_time`);
CREATE INDEX idx_like_music_id ON `like_record`(`music_id`);
CREATE INDEX idx_download_music_id ON `download_record`(`music_id`);

-- 初始数据同步：把已有数据刷进计数列（首次执行时跑一次）
UPDATE `music` m
SET m.like_count = (SELECT COUNT(*) FROM like_record WHERE music_id = m.id),
    m.comment_count = (SELECT COUNT(*) FROM comment WHERE music_id = m.id),
    m.download_count = (SELECT COUNT(*) FROM download_record WHERE music_id = m.id);

-- 触发器：点赞时 +1，取消点赞时 -1
DROP TRIGGER IF EXISTS trg_like_insert;
DELIMITER //
CREATE TRIGGER trg_like_insert AFTER INSERT ON like_record FOR EACH ROW
BEGIN
    UPDATE `music` SET like_count = like_count + 1 WHERE id = NEW.music_id;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_like_delete;
DELIMITER //
CREATE TRIGGER trg_like_delete AFTER DELETE ON like_record FOR EACH ROW
BEGIN
    UPDATE `music` SET like_count = like_count - 1 WHERE id = OLD.music_id;
END //
DELIMITER ;

-- 触发器：发表评论时 +1，删除评论时 -1
DROP TRIGGER IF EXISTS trg_comment_insert;
DELIMITER //
CREATE TRIGGER trg_comment_insert AFTER INSERT ON comment FOR EACH ROW
BEGIN
    UPDATE `music` SET comment_count = comment_count + 1 WHERE id = NEW.music_id;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_comment_delete;
DELIMITER //
CREATE TRIGGER trg_comment_delete AFTER DELETE ON comment FOR EACH ROW
BEGIN
    UPDATE `music` SET comment_count = comment_count - 1 WHERE id = OLD.music_id;
END //
DELIMITER ;

-- 触发器：下载时 +1，删除下载记录时 -1
DROP TRIGGER IF EXISTS trg_download_insert;
DELIMITER //
CREATE TRIGGER trg_download_insert AFTER INSERT ON download_record FOR EACH ROW
BEGIN
    UPDATE `music` SET download_count = download_count + 1 WHERE id = NEW.music_id;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_download_delete;
DELIMITER //
CREATE TRIGGER trg_download_delete AFTER DELETE ON download_record FOR EACH ROW
BEGIN
    UPDATE `music` SET download_count = download_count - 1 WHERE id = OLD.music_id;
END //
DELIMITER ;

-- ================================================================
-- 触发器：评论写入前校验（非空 + 长度限制）
-- 未在 schema.sql 中，从数据库同步过来
-- ================================================================
DROP TRIGGER IF EXISTS trg_comment_before_insert;
DELIMITER //
CREATE TRIGGER trg_comment_before_insert BEFORE INSERT ON comment FOR EACH ROW
BEGIN
    IF NEW.content IS NULL OR TRIM(NEW.content) = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '评论内容不能为空';
    END IF;
    IF CHAR_LENGTH(NEW.content) > 2000 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '评论内容不能超过2000个字符';
    END IF;
END //
DELIMITER ;

-- ================================================================
-- 函数：计算音乐热度分（用于排行榜排序）
-- 权重：点赞×3 + 评论×2 + 下载×1
-- 未在 schema.sql 中，从数据库同步过来
-- ================================================================
DROP FUNCTION IF EXISTS fn_music_hot_score;
DELIMITER //
CREATE FUNCTION fn_music_hot_score(p_music_id INT) RETURNS INT DETERMINISTIC
BEGIN
    DECLARE v_likes INT DEFAULT 0;
    DECLARE v_comments INT DEFAULT 0;
    DECLARE v_downloads INT DEFAULT 0;

    SELECT COUNT(*) INTO v_likes FROM like_record WHERE music_id = p_music_id;
    SELECT COUNT(*) INTO v_comments FROM comment WHERE music_id = p_music_id;
    SELECT COUNT(*) INTO v_downloads FROM download_record WHERE music_id = p_music_id;

    RETURN v_likes * 3 + v_comments * 2 + v_downloads;
END //
DELIMITER ;

-- ================================================================
-- 存储过程：按类型获取排行榜（支持 likes / comments / downloads / hot）
-- 未在 schema.sql 中，从数据库同步过来
-- ================================================================
DROP PROCEDURE IF EXISTS sp_get_leaderboard;
DELIMITER //
CREATE PROCEDURE sp_get_leaderboard(IN p_type VARCHAR(20), IN p_limit INT)
BEGIN
    IF p_type = 'likes' THEN
        SELECT m.id, m.title, m.artist, COUNT(lr.id) AS total
        FROM music m LEFT JOIN like_record lr ON lr.music_id = m.id
        GROUP BY m.id ORDER BY total DESC LIMIT p_limit;
    ELSEIF p_type = 'comments' THEN
        SELECT m.id, m.title, m.artist, COUNT(c.id) AS total
        FROM music m LEFT JOIN comment c ON c.music_id = m.id
        GROUP BY m.id ORDER BY total DESC LIMIT p_limit;
    ELSEIF p_type = 'downloads' THEN
        SELECT m.id, m.title, m.artist, COUNT(dr.id) AS total
        FROM music m LEFT JOIN download_record dr ON dr.music_id = m.id
        GROUP BY m.id ORDER BY total DESC LIMIT p_limit;
    ELSE
        SELECT m.id, m.title, m.artist,
               (COUNT(DISTINCT lr.id) * 3 + COUNT(DISTINCT c.id) * 2 + COUNT(DISTINCT dr.id)) AS hot_score
        FROM music m
        LEFT JOIN like_record lr ON lr.music_id = m.id
        LEFT JOIN comment c ON c.music_id = m.id
        LEFT JOIN download_record dr ON dr.music_id = m.id
        GROUP BY m.id ORDER BY hot_score DESC LIMIT p_limit;
    END IF;
END //
DELIMITER ;

-- ================================================================
-- 存储过程（游标示例）：批量重算所有音乐的冗余计数字段
-- 用途：当触发器未执行或数据被手动修改导致计数不一致时，一键校正
-- 展示 MySQL CURSOR 的使用
-- ================================================================
DROP PROCEDURE IF EXISTS sp_recalc_all_counts;
DELIMITER //
CREATE PROCEDURE sp_recalc_all_counts()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_music_id INT;
    DECLARE cur CURSOR FOR SELECT id FROM music;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    recalc_loop: LOOP
        FETCH cur INTO v_music_id;
        IF done THEN LEAVE recalc_loop; END IF;

        UPDATE music SET
            like_count    = (SELECT COUNT(*) FROM like_record WHERE music_id = v_music_id),
            comment_count = (SELECT COUNT(*) FROM comment WHERE music_id = v_music_id),
            download_count= (SELECT COUNT(*) FROM download_record WHERE music_id = v_music_id)
        WHERE id = v_music_id;
    END LOOP;
    CLOSE cur;
END //
DELIMITER ;