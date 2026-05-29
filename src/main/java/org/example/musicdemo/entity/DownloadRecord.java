package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 下载记录实体，对应数据库中的 download_record 表。
 *
 * <p>每当用户点击下载按钮下载某首音乐时，后端会向此表中插入一条记录，
 * 用于记录用户的下载行为。这些数据有两个核心用途：</p>
 * <ol>
 *   <li><b>下载计数</b>：统计每首音乐的下载次数，用于热度排行（与 LikeRecord 配合计算权重）。</li>
 *   <li><b>用户行为记录</b>：可用于展示用户的下载历史，或防止重复下载（业务需要）。</li>
 * </ol>
 *
 * <p>数据库表结构（download_record）：</p>
 * <pre>
 * id          INT PRIMARY KEY AUTO_INCREMENT
 * user_id     INT NOT NULL      -- 下载用户 ID，关联 user.id
 * music_id    INT NOT NULL      -- 被下载音乐 ID，关联 music.id
 * create_time DATETIME DEFAULT CURRENT_TIMESTAMP
 * </pre>
 *
 * <p>本实体与 LikeRecord 结构非常相似，两者都是「用户-音乐-时间」的三字段行为记录，
 * 区别在于语义不同：下载记录通常需要保证幂等性检查（同用户同音乐只记录一次或允许多次？），
 * 具体策略由 Service 层决定。</p>
 *
 * @see org.example.musicdemo.entity.LikeRecord
 * @see org.example.musicdemo.entity.Music
 */
@Data
public class DownloadRecord {
    /** 主键 ID，自增长，唯一标识每一条下载记录 */
    private Integer id;

    /**
     * 执行下载操作的用户 ID。
     * 关联 {@link org.example.musicdemo.entity.User#id}，
     * 由后端从 JWT 令牌中获取当前登录用户的 ID 后注入。
     */
    private Integer userId;

    /**
     * 被下载的音乐 ID。
     * 关联 {@link org.example.musicdemo.entity.Music#id}，
     * 前端在下载请求中传递 musicId，后端据此写入记录并递增 music 表的 download_count 字段。
     */
    private Integer musicId;

    /**
     * 下载记录的创建时间（即下载行为发生的时间），
     * 由数据库自动填充 CURRENT_TIMESTAMP。
     * 可用于统计时间范围内的下载热度，例如「近 7 日下载排行榜」。
     */
    private LocalDateTime createTime;
}