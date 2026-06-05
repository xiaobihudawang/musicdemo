package org.example.musicdemo.service;

import org.example.musicdemo.entity.LikeRecord;
import org.example.musicdemo.mapper.LikeRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞服务，处理用户对音乐的点赞与取消点赞（toggle 模式）。
 * 不再维护冗余计数字段，点赞数通过 like_record 表实时统计。
 */
@Service
public class LikeService {

    private final LikeRecordMapper likeRecordMapper;

    public LikeService(LikeRecordMapper likeRecordMapper) {
        this.likeRecordMapper = likeRecordMapper;
    }

    /** 切换点赞状态：已赞则取消，未赞则点赞 */
    @Transactional
    public Map<String, Object> toggle(Integer userId, Integer musicId) {
        LikeRecord existing = likeRecordMapper.findByUserAndMusic(userId, musicId);

        Map<String, Object> result = new HashMap<>();

        if (existing != null) {
            likeRecordMapper.delete(userId, musicId);
            result.put("liked", false);
        } else {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setMusicId(musicId);
            likeRecordMapper.insert(record);
            result.put("liked", true);
        }

        return result;
    }

    /** 查询用户是否已点赞指定音乐 */
    public boolean isLiked(Integer userId, Integer musicId) {
        return likeRecordMapper.findByUserAndMusic(userId, musicId) != null;
    }
}
