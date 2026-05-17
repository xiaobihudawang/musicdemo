package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.service.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行榜控制器，提供每周点赞/下载/评论排行数据
 */
@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * 获取本周点赞排行榜（前10）
     */
    @GetMapping("/likes")
    public Result<?> weeklyLikes() {
        List<Music> list = rankingService.getWeeklyLikeTop10();
        return Result.success(list);
    }

    /**
     * 获取本周下载排行榜（前10）
     */
    @GetMapping("/downloads")
    public Result<?> weeklyDownloads() {
        List<Music> list = rankingService.getWeeklyDownloadTop10();
        return Result.success(list);
    }

    /**
     * 获取本周评论排行榜（前10）
     */
    @GetMapping("/comments")
    public Result<?> weeklyComments() {
        List<Music> list = rankingService.getWeeklyCommentTop10();
        return Result.success(list);
    }
}
