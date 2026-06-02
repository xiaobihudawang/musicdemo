package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 下载记录实体，对应 download_record 表。
 */
@Data
public class DownloadRecord {
    private Integer id;
    /** 下载用户 ID */
    private Integer userId;
    /** 被下载的音乐 ID */
    private Integer musicId;
    /** 下载时间，数据库自动填充 */
    private LocalDateTime createTime;
}