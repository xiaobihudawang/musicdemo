-- ================================================================
-- 示例数据（需在执行 schema.sql 之后手动运行）
-- 用法：USE music_platform; SOURCE sample-data.sql;
-- ================================================================

-- 清空已有数据（按外键顺序）
DELETE FROM download_record;
DELETE FROM like_record;
DELETE FROM comment;
DELETE FROM music;
DELETE FROM `user`;

-- 1. 用户（密码均为 BCrypt 加密后的 "admin123" / "123456"）
INSERT INTO `user` (`username`, `password`, `name`, `email`, `role`, `enabled`) VALUES
('admin',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'admin@music.com', 'admin', 1),
('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户', 'test@music.com',  'user',  1),
('alice',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '爱丽丝', 'alice@music.com', 'user',  1),
('bob',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '鲍勃',   'bob@music.com',   'user',  1),
('disabled', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '已禁用', 'disabled@music.com', 'user', 0);

-- 2. 音乐
INSERT INTO `music` (`title`, `artist`, `description`, `file_path`, `file_size`, `user_id`) VALUES
('晴天',        '周杰伦', '一首充满青春回忆的校园歌曲',           '/music/sunny.mp3',  5242880, 2),
('告白气球',    '周杰伦', '浪漫轻快的告白神曲',                   '/music/balloon.mp3', 4194304, 2),
('光年之外',    '邓紫棋', '太空爱情主题的流行金曲',               '/music/light.mp3',   6291456, 3),
('泡沫',        '邓紫棋', '情感层层递进的伤感之作',               '/music/foam.mp3',    5242880, 3),
('起风了',      '买辣椒也用券', '翻唱自日本歌曲的经典中文版',      '/music/wind.mp3',    4718592, 4),
('平凡之路',    '朴树',   '关于人生与选择的哲理歌曲',              '/music/road.mp3',    5767168, 4),
('夜曲',        '周杰伦', '以古典钢琴为基调的叙事曲',              '/music/nocturne.mp3', 4849664, 2),
('追光者',      '岑宁儿', '电视剧插曲，温暖治愈',                  '/music/chaser.mp3',  3984384, 3),
('成都',        '赵雷',   '写给成都的情歌，民谣风格',               '/music/chengdu.mp3', 5046272, 4),
('十年',        '陈奕迅', '关于时间与感情的经典粤语歌曲',          '/music/decade.mp3',  5505024, 4);

-- 更新计数列（模拟触发器已生效后的状态）
UPDATE `music` SET
    like_count    = 15, comment_count = 8,  download_count = 42 WHERE title = '晴天';
UPDATE `music` SET
    like_count    = 12, comment_count = 5,  download_count = 31 WHERE title = '告白气球';
UPDATE `music` SET
    like_count    = 10, comment_count = 6,  download_count = 28 WHERE title = '光年之外';
UPDATE `music` SET
    like_count    = 8,  comment_count = 4,  download_count = 22 WHERE title = '泡沫';
UPDATE `music` SET
    like_count    = 20, comment_count = 10, download_count = 55 WHERE title = '起风了';
UPDATE `music` SET
    like_count    = 6,  comment_count = 3,  download_count = 18 WHERE title = '平凡之路';
UPDATE `music` SET
    like_count    = 9,  comment_count = 4,  download_count = 25 WHERE title = '夜曲';
UPDATE `music` SET
    like_count    = 7,  comment_count = 5,  download_count = 20 WHERE title = '追光者';
UPDATE `music` SET
    like_count    = 5,  comment_count = 2,  download_count = 15 WHERE title = '成都';
UPDATE `music` SET
    like_count    = 11, comment_count = 7,  download_count = 35 WHERE title = '十年';

-- 3. 评论
INSERT INTO `comment` (`content`, `user_id`, `music_id`) VALUES
('这首晴天真的百听不厌！',         2, 1),
('每次听到前奏就想起学生时代',     3, 1),
('告白气球太甜了，婚礼必备！',     2, 2),
('邓紫棋的唱功太强了',            4, 3),
('泡沫这首歌听了无数遍',          2, 4),
('起风了永远的神！',              3, 5),
('朴树的歌总是那么有深度',        2, 6),
('夜曲的旋律太美了',              4, 7),
('追光者让人感觉很温暖',          2, 8),
('成都让我想去走走',              3, 9),
('十年听了十年还是那么感动',      2, 10);

-- 4. 点赞记录
INSERT INTO `like_record` (`user_id`, `music_id`) VALUES
(2, 1), (3, 1), (4, 1),
(2, 2), (3, 2),
(2, 3), (4, 3),
(2, 4),
(2, 5), (3, 5), (4, 5),
(3, 7),
(2, 8),
(2, 10), (3, 10);

-- 5. 下载记录
INSERT INTO `download_record` (`user_id`, `music_id`) VALUES
(2, 1), (3, 1), (4, 1),
(2, 2), (3, 2),
(2, 3), (3, 3),
(2, 5), (3, 5), (4, 5),
(2, 10), (3, 10), (4, 10);


