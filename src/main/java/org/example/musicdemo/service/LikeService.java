package org.example.musicdemo.service;

import org.example.musicdemo.entity.LikeRecord;
import org.example.musicdemo.mapper.LikeRecordMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞服务 —— 处理用户对音乐的点赞与取消点赞。
 * <p>
 * 点赞功能使用「切换（toggle）」设计模式：同一用户对同一首歌重复调用时，
 * 第一次点赞，第二次取消，第三次再点赞……如此循环。
 * </p>
 *
 * <h3>数据一致性</h3>
 * 每次点赞或取消点赞都需要同时更新两张表：
 * <ul>
 *   <li>{@code like_record} 表 —— 记录每对 (userId, musicId) 的点赞关系</li>
 *   <li>{@code music.like_count} 字段 —— 歌曲的点赞总数统计</li>
 * </ul>
 * 写操作在 {@link Transactional} 事务中执行，保证两者一致。
 */
@Service
public class LikeService {

    /** 点赞记录表的数据访问层接口 */
    private final LikeRecordMapper likeRecordMapper;

    /** 音乐表的数据访问层接口，用于更新点赞计数 */
    private final MusicMapper musicMapper;

    /**
     * 构造器注入。
     *
     * @param likeRecordMapper 点赞记录 Mapper
     * @param musicMapper      音乐 Mapper
     */
    public LikeService(LikeRecordMapper likeRecordMapper, MusicMapper musicMapper) {
        this.likeRecordMapper = likeRecordMapper;
        this.musicMapper = musicMapper;
    }

    /**
     * 切换点赞状态：如果用户已点赞则取消，未点赞则点赞。
     * <p>
     * 判断逻辑：
     * <ol>
     *   <li>根据 (userId, musicId) 查询 {@code like_record} 表</li>
     *   <li>若查到记录 → 说明已赞 → 删除记录 + music.like_count 减 1</li>
     *   <li>若未查到 → 说明未赞 → 插入记录 + music.like_count 加 1</li>
     * </ol>
     * 返回的 Map 中包含 {@code liked} 字段（true/false），
     * 前端可根据此字段实时更新点赞按钮的 UI 状态。
     * </p>
     *
     * @param userId  当前操作用户的 ID
     * @param musicId 目标音乐的 ID
     * @return 包含点赞后状态的 Map：{@code {"liked": true/false}}
     */
    @Transactional
    public Map<String, Object> toggle(Integer userId, Integer musicId) {
        // 查询是否已存在点赞记录
        LikeRecord existing = likeRecordMapper.findByUserAndMusic(userId, musicId);

        Map<String, Object> result = new HashMap<>();

        if (existing != null) {
            // 已点赞 → 取消点赞：删除记录，点赞数 -1
            likeRecordMapper.delete(userId, musicId);
            musicMapper.updateLikeCount(musicId, -1);
            result.put("liked", false);
        } else {
            // 未点赞 → 执行点赞：插入记录，点赞数 +1
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
     * 查询指定用户是否已点赞指定音乐。
     * <p>
     * 此方法为只读操作，不需要事务。
     * 用于页面加载时初始化点赞按钮状态。
     * </p>
     *
     * @param userId  用户 ID
     * @param musicId 音乐 ID
     * @return true 表示已赞，false 表示未赞
     */
    public boolean isLiked(Integer userId, Integer musicId) {
        return likeRecordMapper.findByUserAndMusic(userId, musicId) != null;
    }
}
