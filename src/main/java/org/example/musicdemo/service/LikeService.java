package org.example.musicdemo.service;

import org.example.musicdemo.entity.LikeRecord;
import org.example.musicdemo.mapper.LikeRecordMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞服务，处理用户点赞与取消点赞逻辑
 */
@Service
public class LikeService {

    private final LikeRecordMapper likeRecordMapper;
    private final MusicMapper musicMapper;

    public LikeService(LikeRecordMapper likeRecordMapper, MusicMapper musicMapper) {
        this.likeRecordMapper = likeRecordMapper;
        this.musicMapper = musicMapper;
    }

    /**
     * 切换点赞状态：已赞则取消，未赞则点赞
     */
    @Transactional
    public Map<String, Object> toggle(Integer userId, Integer musicId) {
        LikeRecord existing = likeRecordMapper.findByUserAndMusic(userId, musicId);

        Map<String, Object> result = new HashMap<>();

        if (existing != null) {
            likeRecordMapper.delete(userId, musicId);
            musicMapper.updateLikeCount(musicId, -1);
            result.put("liked", false);
        } else {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setMusicId(musicId);
            likeRecordMapper.insert(record);
            musicMapper.updateLikeCount(musicId, 1);
            result.put("liked", true);
        }

        return result;
    }

    /**
     * 查询用户是否已点赞指定音乐
     */
    public boolean isLiked(Integer userId, Integer musicId) {
        return likeRecordMapper.findByUserAndMusic(userId, musicId) != null;
    }
}
