package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 点赞记录实体，对应 like_record 表。
 * 同一用户对同一音乐最多一条记录（逻辑唯一）。
 */
@Data
public class LikeRecord {
    private Integer id;
    /** 点赞用户 ID */
    private Integer userId;
    /** 被点赞的音乐 ID */
    private Integer musicId;
    /** 点赞时间，数据库自动填充 */
    private LocalDateTime createTime;
}