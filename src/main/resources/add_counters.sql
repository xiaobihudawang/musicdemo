-- ================================================================
-- 第 1 步：给 music 表加 3 个计数列
-- ================================================================
ALTER TABLE `music`
    ADD COLUMN `like_count`     INT DEFAULT 0 COMMENT '冗余字段：点赞数，由触发器自动维护',
    ADD COLUMN `comment_count`  INT DEFAULT 0 COMMENT '冗余字段：评论数，由触发器自动维护',
    ADD COLUMN `download_count` INT DEFAULT 0 COMMENT '冗余字段：下载数，由触发器自动维护';

-- ================================================================
-- 第 2 步：把已有数据刷进计数列（首次执行时跑一次）
-- ================================================================
UPDATE `music` m
SET m.like_count     = (SELECT COUNT(*) FROM like_record WHERE music_id = m.id),
    m.comment_count  = (SELECT COUNT(*) FROM comment WHERE music_id = m.id),
    m.download_count = (SELECT COUNT(*) FROM download_record WHERE music_id = m.id);

-- ================================================================
-- 第 3 步：建索引（加速 JOIN 和排序）
-- ================================================================
CREATE INDEX idx_comment_music_id   ON `comment`(`music_id`, `create_time`);
CREATE INDEX idx_like_music_id      ON `like_record`(`music_id`);
CREATE INDEX idx_download_music_id  ON `download_record`(`music_id`);

-- ================================================================
-- 第 4 步：建 6 个计数器触发器
-- ================================================================

-- 4a. 点赞：插入时 +1，删除时 -1
DROP TRIGGER IF EXISTS trg_like_insert;
DELIMITER //
CREATE TRIGGER trg_like_insert AFTER INSERT ON like_record FOR EACH ROW
BEGIN
    UPDATE `music` SET like_count = like_count + 1 WHERE id = NEW.music_id;
END//

DROP TRIGGER IF EXISTS trg_like_delete;
CREATE TRIGGER trg_like_delete AFTER DELETE ON like_record FOR EACH ROW
BEGIN
    UPDATE `music` SET like_count = like_count - 1 WHERE id = OLD.music_id;
END//

-- 4b. 评论：插入时 +1，删除时 -1
DROP TRIGGER IF EXISTS trg_comment_insert;
CREATE TRIGGER trg_comment_insert AFTER INSERT ON comment FOR EACH ROW
BEGIN
    UPDATE `music` SET comment_count = comment_count + 1 WHERE id = NEW.music_id;
END//

DROP TRIGGER IF EXISTS trg_comment_delete;
CREATE TRIGGER trg_comment_delete AFTER DELETE ON comment FOR EACH ROW
BEGIN
    UPDATE `music` SET comment_count = comment_count - 1 WHERE id = OLD.music_id;
END//

-- 4c. 下载：插入时 +1，删除时 -1
DROP TRIGGER IF EXISTS trg_download_insert;
CREATE TRIGGER trg_download_insert AFTER INSERT ON download_record FOR EACH ROW
BEGIN
    UPDATE `music` SET download_count = download_count + 1 WHERE id = NEW.music_id;
END//

DROP TRIGGER IF EXISTS trg_download_delete;
CREATE TRIGGER trg_download_delete AFTER DELETE ON download_record FOR EACH ROW
BEGIN
    UPDATE `music` SET download_count = download_count - 1 WHERE id = OLD.music_id;
END//

DELIMITER ;
