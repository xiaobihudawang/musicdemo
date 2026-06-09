package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.DownloadRecord;
import org.apache.ibatis.annotations.Param;

/**
 * 下载记录数据访问层接口，操作 download_record 表。
 */
public interface DownloadRecordMapper {

    /** 新增下载记录 */
    int insert(DownloadRecord downloadRecord);
}
