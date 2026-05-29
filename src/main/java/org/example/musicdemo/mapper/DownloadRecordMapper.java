package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.DownloadRecord;
import org.apache.ibatis.annotations.Param;

/**
 * 下载记录数据访问层接口（MyBatis Mapper）—— 操作 download_record 表
 *
 * 对应 XML 映射文件：src/main/resources/mapper/DownloadRecordMapper.xml
 *
 * download_record 表结构：
 * - id：          主键，自增
 * - user_id：     下载者的用户 ID（外键关联 user 表）
 * - music_id：    被下载的音乐 ID（外键关联 music 表）
 * - download_time：下载时间（TIMESTAMP）
 *
 * ─── 作用 ───
 * 记录每个用户的下载历史，用于：
 * 1. 统计音乐下载量（在排行榜中使用）
 * 2. 用户个人中心显示下载历史
 * 3. 防止重复下载统计（可选）
 *
 * 当前系统仅实现"新增记录"功能，后续可以扩展 findByUserId 等查询方法。
 */
public interface DownloadRecordMapper {

    /**
     * 新增一条下载记录
     * 当用户点击下载按钮时调用此方法，记录用户的下载行为
     *
     * 字段说明：
     * - downloadRecord.userId：   当前登录用户的 ID
     * - downloadRecord.musicId：  被下载的音乐的 ID
     * - downloadRecord.downloadTime：当前时间（在 Mapper XML 中使用 NOW() 或 Java Date）
     *
     * @param downloadRecord 下载记录对象
     * @return 受影响的行数（正常为 1）
     */
    int insert(DownloadRecord downloadRecord);

    /**
     * 根据音乐 ID 删除所有下载记录
     * 用于删除音乐时级联清理
     *
     * @param musicId 音乐 ID
     * @return 删除的记录数
     */
    int deleteByMusicId(@Param("musicId") Integer musicId);
}
