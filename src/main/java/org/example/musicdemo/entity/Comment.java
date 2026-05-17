package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评论实体：对应 comment 表
 */
@Data
public class Comment {
    /** 主键 ID */
    private Integer id;
    /** 评论内容 */
    private String content;
    /** 评论用户 ID */
    private Integer userId;
    /** 被评论的音乐 ID */
    private Integer musicId;
    /** 评论时间 */
    private LocalDateTime createTime;

    /** 评论用户昵称（非数据库字段，用于展示） */
    private String username;
}