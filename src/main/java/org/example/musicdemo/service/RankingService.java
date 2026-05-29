package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 排行榜服务 —— 按周统计各类 Top 10 排行榜。
 * <p>
 * 排行榜是音乐平台的重要功能，提供以下三个维度的周排行：
 * <ul>
 *   <li>点赞榜（本周点赞数最多的 10 首歌）</li>
 *   <li>下载榜（本周下载数最多的 10 首歌）</li>
 *   <li>评论榜（本周评论数最多的 10 首歌）</li>
 * </ul>
 * </p>
 *
 * <h3>时间范围</h3>
 * 按自然周统计：周一 00:00:00 到周日 23:59:59。
 * 使用 {@link LocalDate} 和 {@link DayOfWeek} 计算，
 * 不受服务器时区影响（前提是 JVM 时区设置正确）。
 * 如果本周还未结束，排行榜反映的是截至当前时刻的数据。
 *
 * <h3>数据来源</h3>
 * 排行榜数据来源于三张行为记录表：
 * <ul>
 *   <li>{@code like_record} —— 点赞记录</li>
 *   <li>{@code download_record} —— 下载记录</li>
 *   <li>{@code comment} —— 评论记录</li>
 * </ul>
 * 由 MyBatis XML 中的 SQL 按 {@code create_time} 字段进行周内过滤和聚合。
 */
@Service
public class RankingService {

    /** 音乐表的数据访问层接口，排行榜 SQL 定义在对应的 XML 文件中 */
    private final MusicMapper musicMapper;

    /**
     * 构造器注入。
     *
     * @param musicMapper 音乐 Mapper
     */
    public RankingService(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    /**
     * 计算当前自然周的起止时间字符串。
     * <p>
     * 格式：{@code yyyy-MM-dd HH:mm:ss}（MySQL DATETIME 兼容格式）。
     * 例如周一为 {@code 2026-05-18 00:00:00}，周日为 {@code 2026-05-24 23:59:59}。
     * </p>
     *
     * @return String[2]，[0]=起始时间，[1]=结束时间
     */
    private String[] getWeekRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);   // 本周一
        LocalDate sunday = monday.plusDays(6);             // 本周日
        // 将 LocalDate 转为时间字符串，去掉中间的 "T" 以兼容 MySQL 格式
        String start = monday.atStartOfDay().toString().replace("T", " ");
        String end = sunday.atTime(LocalTime.MAX).toString().replace("T", " ");
        return new String[]{start, end};
    }

    /**
     * 获取本周点赞数 Top 10 歌曲排行。
     * 统计范围为本周一 00:00:00 到本周日 23:59:59，
     * 按点赞记录数降序排列，取前 10 名。
     *
     * @return 点赞榜 Top 10 歌曲列表
     */
    public List<Music> getWeeklyLikeTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyLikeTop10(range[0], range[1]);
    }

    /**
     * 获取本周下载数 Top 10 歌曲排行。
     * 统计范围为本周一 00:00:00 到本周日 23:59:59，
     * 按下记录数降序排列，取前 10 名。
     *
     * @return 下载榜 Top 10 歌曲列表
     */
    public List<Music> getWeeklyDownloadTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyDownloadTop10(range[0], range[1]);
    }

    /**
     * 获取本周评论数 Top 10 歌曲排行。
     * 统计范围为本周一 00:00:00 到本周日 23:59:59，
     * 按评论记录数降序排列，取前 10 名。
     *
     * @return 评论榜 Top 10 歌曲列表
     */
    public List<Music> getWeeklyCommentTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyCommentTop10(range[0], range[1]);
    }
}
