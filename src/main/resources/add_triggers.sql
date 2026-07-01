-- 6 个计数器触发器（单独执行，不含 CREATE INDEX 避免冲突）

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
