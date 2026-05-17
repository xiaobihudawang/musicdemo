package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RankingService {

    private final MusicMapper musicMapper;

    public RankingService(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    private String[] getWeekRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        String start = monday.atStartOfDay().toString().replace("T", " ");
        String end = sunday.atTime(LocalTime.MAX).toString().replace("T", " ");
        return new String[]{start, end};
    }

    public List<Music> getWeeklyLikeTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyLikeTop10(range[0], range[1]);
    }

    public List<Music> getWeeklyDownloadTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyDownloadTop10(range[0], range[1]);
    }

    public List<Music> getWeeklyCommentTop10() {
        String[] range = getWeekRange();
        return musicMapper.findWeeklyCommentTop10(range[0], range[1]);
    }
}
