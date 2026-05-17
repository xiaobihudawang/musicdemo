package org.example.musicdemo.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 音乐实体：对应 music 表
 */
@Data
public class Music {
    /** 主键 ID */
    private Integer id;
    /** 音乐标题 */
    private String title;
    /** 艺术家 / 歌手 */
    private String artist;
    /** 音乐描述 */
    private String description;
    /** 文件存储路径 */
    private String filePath;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 点赞数 */
    private Integer likeCount;
    /** 评论数 */
    private Integer commentCount;
    /** 下载数 */
    private Integer downloadCount;
    /** 上传用户 ID */
    private Integer userId;
    /** 创建时间 */
    private LocalDateTime createTime;

    /** 上传用户昵称（非数据库字段，用于列表展示） */
    private String username;
}