-- 存储过程：级联删除用户
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
