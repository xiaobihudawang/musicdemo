package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评论实体，对应 comment 表。
 */
@Data
public class Comment {
    private Integer id;
    /** 评论文本内容 */
    private String content;
    /** 评论用户 ID（由 JWT 自动填充） */
    private Integer userId;
    /** 被评论的音乐 ID */
    private Integer musicId;
    /** 评论时间，数据库自动填充 */
    private LocalDateTime createTime;
    /** 评论用户昵称（非数据库字段，通过 JOIN user 表获取） */
    private String username;
}