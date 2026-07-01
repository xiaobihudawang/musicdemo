-- ================================================================
-- 样本数据导入（用于测试和演示）
-- 执行前确保已先运行 schema.sql 建表
-- 使用：mysql -uroot -p music_platform < data.sql
-- ================================================================

-- 插入管理员账号（密码：admin123，BCrypt 加密）
INSERT INTO `user` (`username`, `password`, `name`, `role`, `enabled`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'admin', 1);

-- 插入测试普通用户
INSERT INTO `user` (`username`, `password`, `name`, `email`, `role`, `enabled`) VALUES
('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户', 'test@example.com', 'user', 1);

-- 插入示例音乐（使用占位路径，用户上传后应替换）
INSERT INTO `music` (`title`, `artist`, `description`, `file_path`, `file_size`, `user_id`) VALUES
('晴天', '周杰伦', '经典校园民谣，回忆青春的旋律', 'placeholder/sunny.mp3', 0, 1),
('七里香', '周杰伦', '充满诗意的情歌，带你回到那个夏天', 'placeholder/qilixiang.mp3', 0, 1),
('海阔天空', 'Beyond', '励志经典，追逐梦想的勇气', 'placeholder/sea.mp3', 0, 1),
('光辉岁月', 'Beyond', '致敬曼德拉，永不放弃的精神', 'placeholder/glory.mp3', 0, 1),
('匆匆那年', '王菲', '电影主题曲，回忆青春的感动', 'placeholder/hurry.mp3', 0, 1),
('平凡之路', '朴树', '在平凡中寻找不平凡的人生', 'placeholder/road.mp3', 0, 1),
('成都', '赵雷', '一首写给成都的情歌，温暖治愈', 'placeholder/chengdu.mp3', 0, 1),
('遥远的她', '张学友', '经典粤语情歌，感人至深', 'placeholder/far.mp3', 0, 1),
('小幸运', '田馥甄', '青春校园电影主题曲，纯真爱恋', 'placeholder/lucky.mp3', 0, 1),
('后来', '刘若英', '后来我总算学会了如何去爱', 'placeholder/after.mp3', 0, 1);
