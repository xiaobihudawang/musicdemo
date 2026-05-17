package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 点赞记录实体：对应 like_record 表
 */
@Data
public class LikeRecord {
    /** 主键 ID */
    private Integer id;
    /** 点赞用户 ID */
    private Integer userId;
    /** 被点赞的音乐 ID */
    private Integer musicId;
    /** 点赞时间 */
    private LocalDateTime createTime;
}