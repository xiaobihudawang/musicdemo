package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 排行榜服务，按周统计点赞/下载/评论 Top 10 排行。
 * 时间范围：周一 00:00:00 到周日 23:59:59。
 */
@Service
public class RankingService {

    private final MusicMapper musicMapper;

    public RankingService(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    /** 计算当前自然周的起止时间 */
    private String[] getWeekRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        String start = monday.atStartOfDay().toString().replace("T", " ");
        String end = sunday.atTime(LocalTime.MAX).toString().replace("T", " ");
        return new String[]{start, end};
    }

    /** 获取本周点赞 Top 10 */
    public List<Music> getWeeklyLikeTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyLikeTop10(range[0], range[1]);
    }

    /** 获取本周下载 Top 10 */
    public List<Music> getWeeklyDownloadTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyDownloadTop10(range[0], range[1]);
    }

    /** 获取本周评论 Top 10 */
    public List<Music> getWeeklyCommentTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyCommentTop10(range[0], range[1]);
    }
}
