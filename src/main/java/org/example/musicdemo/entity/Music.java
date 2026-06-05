package org.example.musicdemo.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 音乐实体，对应 music 表。核心实体，包含音乐元数据、统计计数和归属信息。
 */
@Data
public class Music {
    private Integer id;
    private String title;
    /** 艺术家/歌手，未填写时默认 "未知艺术家" */
    private String artist;
    /** 音乐描述/简介 */
    private String description;
    /** 音频文件存储路径 */
    private String filePath;
    /** 封面图片路径（相对路径，位于 covers/ 子目录下） */
    private String coverPath;
    /** LRC格式歌词 */
    private String lyrics;
    /** 音频文件字节大小 */
    private Long fileSize;
    /** 上传用户 ID */
    private Integer userId;
    /** 上传时间，数据库自动填充 */
    private LocalDateTime createTime;
    /** 上传用户昵称（非数据库字段，通过 JOIN user 表获取） */
    private String username;
    /** 点赞数（非数据库字段，通过 like_record 表统计） */
    private Integer likeCount;
    /** 评论数（非数据库字段，通过 comment 表统计） */
    private Integer commentCount;
    /** 下载数（非数据库字段，通过 download_record 表统计） */
    private Integer downloadCount;
}