-- ================================================================
-- 数据库 Schema — 音乐分享平台（music_platform）
-- 由 mysqldump 导出后手工清理，保持可读性
-- ================================================================

CREATE TABLE IF NOT EXISTS `user` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `username`    VARCHAR(255) NOT NULL UNIQUE,
    `password`    VARCHAR(255) NOT NULL,
    `name`        VARCHAR(255),
    `email`       VARCHAR(255),
    `role`        VARCHAR(50) NOT NULL DEFAULT 'user',
    `enabled`     TINYINT(1) DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    `like_count`     INT DEFAULT 0 COMMENT '冗余字段：点赞数，由触发器自动维护',
    `comment_count`  INT DEFAULT 0 COMMENT '冗余字段：评论数，由触发器自动维护',
    `download_count` INT DEFAULT 0 COMMENT '冗余字段：下载数，由触发器自动维护',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `content`     VARCHAR(2000) NOT NULL,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `like_record` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_music` (`user_id`, `music_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `download_record` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     INT NOT NULL,
    `music_id`    INT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================================
-- 索引
-- ================================================================
CREATE INDEX idx_music_title ON `music`(`title`);
CREATE INDEX idx_music_artist ON `music`(`artist`);
CREATE INDEX idx_comment_music_id ON `comment`(`music_id`, `create_time`);
CREATE INDEX idx_like_music_id ON `like_record`(`music_id`);
CREATE INDEX idx_download_music_id ON `download_record`(`music_id`);

-- ================================================================
-- 视图
-- ================================================================
CREATE OR REPLACE VIEW `v_music_leaderboard` AS
SELECT m.id, m.title, m.artist, u.username AS uploader,
       (SELECT COUNT(*) FROM like_record WHERE music_id = m.id) AS like_count,
       (SELECT COUNT(*) FROM comment WHERE music_id = m.id) AS comment_count,
       (SELECT COUNT(*) FROM download_record WHERE music_id = m.id) AS download_count,
       m.create_time
FROM music m LEFT JOIN `user` u ON m.user_id = u.id;

CREATE OR REPLACE VIEW `v_admin_stats` AS
SELECT (SELECT COUNT(*) FROM `user`) AS total_users,
       (SELECT COUNT(*) FROM music) AS total_music,
       (SELECT COUNT(*) FROM comment) AS total_comments,
       (SELECT COUNT(*) FROM like_record) AS total_likes,
       (SELECT COUNT(*) FROM download_record) AS total_downloads;

-- ================================================================
-- 函数
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
-- 存储过程
-- ================================================================
DROP PROCEDURE IF EXISTS sp_batch_delete_user;
DELIMITER //
CREATE PROCEDURE sp_batch_delete_user(IN p_user_id INT)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION ROLLBACK;
    START TRANSACTION;
    DELETE FROM like_record WHERE user_id = p_user_id;
    DELETE FROM download_record WHERE user_id = p_user_id;
    DELETE FROM comment WHERE user_id = p_user_id;
    DELETE FROM music WHERE user_id = p_user_id;
    DELETE FROM `user` WHERE id = p_user_id;
    COMMIT;
END //
DELIMITER ;

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

-- ================================================================
-- 触发器
-- ================================================================
DROP TRIGGER IF EXISTS trg_like_insert;
DELIMITER //
CREATE TRIGGER trg_like_insert AFTER INSERT ON like_record FOR EACH ROW
BEGIN
    UPDATE music SET like_count = like_count + 1 WHERE id = NEW.music_id;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_like_delete;
DELIMITER //
CREATE TRIGGER trg_like_delete AFTER DELETE ON like_record FOR EACH ROW
BEGIN
    UPDATE music SET like_count = like_count - 1 WHERE id = OLD.music_id;
END //
DELIMITER ;

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

DROP TRIGGER IF EXISTS trg_comment_insert;
DELIMITER //
CREATE TRIGGER trg_comment_insert AFTER INSERT ON comment FOR EACH ROW
BEGIN
    UPDATE music SET comment_count = comment_count + 1 WHERE id = NEW.music_id;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_comment_delete;
DELIMITER //
CREATE TRIGGER trg_comment_delete AFTER DELETE ON comment FOR EACH ROW
BEGIN
    UPDATE music SET comment_count = comment_count - 1 WHERE id = OLD.music_id;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_download_insert;
DELIMITER //
CREATE TRIGGER trg_download_insert AFTER INSERT ON download_record FOR EACH ROW
BEGIN
    UPDATE music SET download_count = download_count + 1 WHERE id = NEW.music_id;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_download_delete;
DELIMITER //
CREATE TRIGGER trg_download_delete AFTER DELETE ON download_record FOR EACH ROW
BEGIN
    UPDATE music SET download_count = download_count - 1 WHERE id = OLD.music_id;
END //
DELIMITER ;

-- ================================================================
-- 事件
-- ================================================================
DROP EVENT IF EXISTS e_cleanup_old_downloads;
DELIMITER //
CREATE EVENT e_cleanup_old_downloads
ON SCHEDULE EVERY 1 MONTH STARTS '2026-08-01 02:00:00'
DO
BEGIN
    DELETE FROM download_record WHERE create_time < DATE_SUB(NOW(), INTERVAL 1 YEAR);
END //
DELIMITER ;

-- ================================================================
-- 初始数据同步：把已有数据刷进计数列（首次执行时跑一次）
-- ================================================================
UPDATE music m
SET m.like_count    = (SELECT COUNT(*) FROM like_record WHERE music_id = m.id),
    m.comment_count = (SELECT COUNT(*) FROM comment WHERE music_id = m.id),
    m.download_count= (SELECT COUNT(*) FROM download_record WHERE music_id = m.id);
