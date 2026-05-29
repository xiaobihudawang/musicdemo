package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 点赞记录实体，对应数据库中的 like_record 表。
 *
 * <p>当用户在某首音乐的详情页点击「点赞/喜欢」按钮时，后端向此表写入一条记录；
 * 若用户再次点击（取消点赞），则删除对应记录。点赞是一种「可反悔」的用户行为，
 * 因此本表的记录在逻辑上应保证「同一用户对同一音乐最多只有一条有效记录」，
 * 即 (user_id, music_id) 构成逻辑唯一约束。</p>
 *
 * <p>数据库表结构（like_record）：</p>
 * <pre>
 * id          INT PRIMARY KEY AUTO_INCREMENT
 * user_id     INT NOT NULL      -- 点赞用户 ID，关联 user.id
 * music_id    INT NOT NULL      -- 被点赞音乐 ID，关联 music.id
 * create_time DATETIME DEFAULT CURRENT_TIMESTAMP
 * -- 建议业务层保证 (user_id, music_id) 唯一，或数据库加 UNIQUE KEY
 * </pre>
 *
 * <p>核心业务逻辑：</p>
 * <ul>
 *   <li><b>点赞</b>：先查是否已存在 userId + musicId 的记录，
 *       若不存在则 INSERT，同时递增 music 表的 like_count；若已存在则直接返回成功（幂等）。</li>
 *   <li><b>取消点赞</b>：按 userId + musicId 删除记录，同时递减 music 表的 like_count。</li>
 *   <li><b>点赞状态查询</b>：在音乐列表页展示当前用户是否已点赞时，通过 userId + musicId 查询是否有记录。</li>
 * </ul>
 *
 * @see org.example.musicdemo.entity.Music
 * @see org.example.musicdemo.entity.User
 */
@Data
public class LikeRecord {
    /** 主键 ID，自增长，唯一标识每一条点赞记录 */
    private Integer id;

    /**
     * 点赞的用户 ID。
     * 关联 {@link org.example.musicdemo.entity.User#id}，
     * 由后端从 JWT 令牌中提取当前登录用户的 ID。
     */
    private Integer userId;

    /**
     * 被点赞的音乐 ID。
     * 关联 {@link org.example.musicdemo.entity.Music#id}，
     * 前端在点赞/取消点赞请求中传递 musicId 参数。
     */
    private Integer musicId;

    /**
     * 点赞操作发生的时间，由数据库自动填充。
     * 点赞记录的 createTime 不像评论那样按时间排序展示，
     * 但可用于分析用户的活跃时间段等辅助功能。
     */
    private LocalDateTime createTime;
}