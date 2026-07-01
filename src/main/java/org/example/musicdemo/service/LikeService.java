package org.example.musicdemo.service;

import org.example.musicdemo.entity.LikeRecord;
import org.example.musicdemo.mapper.LikeRecordMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞服务，处理用户对音乐的点赞与取消点赞（toggle 模式）。
 * 点赞/取消点赞时，music.like_count 由数据库触发器自动维护，无需 Java 手动更新。
 * 切换后返回实际点赞数（从冗余字段读取，无需 COUNT 子查询）。
 *
 * 并发安全：采用 try-insert → catch-DuplicateKeyException → delete 策略。
 *           利用 MySQL 的 UNIQUE KEY (user_id, music_id) 约束做原子性判断，
 *           @Transactional 保证每个请求的 INSERT 和 DELETE 在同一个事务中，
 *           不会产生死锁（MySQL 对同一唯一键的并发 INSERT 会串行化处理）。
 */
@Service
public class LikeService {

    private final LikeRecordMapper likeRecordMapper;
    private final MusicMapper musicMapper;

    public LikeService(LikeRecordMapper likeRecordMapper, MusicMapper musicMapper) {
        this.likeRecordMapper = likeRecordMapper;
        this.musicMapper = musicMapper;
    }

    /** 切换点赞状态：已赞则取消，未赞则点赞 */
    @Transactional
    public Map<String, Object> toggle(Integer userId, Integer musicId) {
        Map<String, Object> result = new HashMap<>();
        try {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setMusicId(musicId);
            likeRecordMapper.insert(record);
            result.put("liked", true);
        } catch (DuplicateKeyException e) {
            likeRecordMapper.delete(userId, musicId);
            result.put("liked", false);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("音乐不存在或已被删除");
        }
        result.put("likeCount", musicMapper.getLikeCountById(musicId));
        return result;
    }

    /** 查询用户是否已点赞指定音乐 */
    public boolean isLiked(Integer userId, Integer musicId) {
        return likeRecordMapper.findByUserAndMusic(userId, musicId) != null;
    }
}
