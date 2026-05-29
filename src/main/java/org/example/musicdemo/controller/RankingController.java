package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.service.RankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行榜控制器 —— 提供周维度的热门歌曲排行数据。
 * <p>
 * 所有端点以 /api/ranking 开头。
 * 安全说明：根据 SecurityConfig 配置，/api/ranking/** 是公开的（permitAll），
 * 所有用户（包括未登录）都可以查看排行榜。
 * <p>
 * 排行依据：
 * - 点赞排行榜：基于本周的 like_record 表统计各歌曲的点赞数。
 * - 下载排行榜：基于本周的 download_record 表统计各歌曲的下载数。
 * - 评论排行榜：基于本周的 comment 表统计各歌曲的评论数。
 * <p>
 * 时间范围：排行榜统计的是"本周"数据（从本周一到本周日）。
 * RankingService 中通过 MySQL 的 WEEK() 函数或 Java 时间计算实现。
 * <p>
 * 排行榜特点：
 * - 榜单每周重置，鼓励新内容获得曝光机会。
 * - 前 10 名的高人气歌曲会获得更好的推荐位。
 * - 数据来自用户的实际交互行为（点赞/下载/评论），难以刷榜。
 */
@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    /** 排行榜服务层 —— 提供各类排行数据的聚合查询 */
    private final RankingService rankingService;

    /**
     * 构造器注入 RankingService。
     *
     * @param rankingService 排行榜服务
     */
    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * 获取本周点赞排行榜（前 10 名）
     * <p>
     * URL: GET /api/ranking/likes
     * <p>
     * 权限：公开（无需登录）。
     * <p>
     * 业务说明：
     * - 调用 rankingService.getWeeklyLikeTop10() 获取排行数据。
     * - 统计纬度：本周内（周一到周日）各音乐获得的点赞总数。
     * - 按点赞数降序排列，取前 10 条。
     * - 返回的是 Music 实体列表（可能包含点赞数的冗余字段）。
     * - 若某音乐本周无点赞，则不会出现在榜单中。
     * <p>
     * 实现思路（Service 层）：
     * - SQL：SELECT m.*, COUNT(lr.id) AS like_count
     *        FROM music m
     *        LEFT JOIN like_record lr ON m.id = lr.music_id
     *        WHERE YEARWEEK(lr.create_time) = YEARWEEK(CURDATE())
     *        GROUP BY m.id
     *        ORDER BY like_count DESC
     *        LIMIT 10
     * <p>
     * 返回值：Result.success(list) —— 排行榜音乐列表
     */
    @GetMapping("/likes")
    public Result<?> weeklyLikes() {
        List<Music> list = rankingService.getWeeklyLikeTop10();
        return Result.success(list);
    }

    /**
     * 获取本周下载排行榜（前 10 名）
     * <p>
     * URL: GET /api/ranking/downloads
     * <p>
     * 权限：公开（无需登录）。
     * <p>
     * 业务说明：
     * - 调用 rankingService.getWeeklyDownloadTop10() 获取排行数据。
     * - 统计纬度：本周内各音乐的下载次数（来自 download_record 表）。
     * - 按下载数降序排列，取前 10 条。
     * - 下载排行榜反映了音乐的实际受欢迎程度。
     * - 注意：下载记录只在用户登录时记录（未登录用户的下载不计数）
     *   （参见 MusicController.download 中的 getCurrentUserIdOrNull 逻辑）。
     * <p>
     * 返回值：Result.success(list) —— 排行榜音乐列表
     */
    @GetMapping("/downloads")
    public Result<?> weeklyDownloads() {
        List<Music> list = rankingService.getWeeklyDownloadTop10();
        return Result.success(list);
    }

    /**
     * 获取本周评论排行榜（前 10 名）
     * <p>
     * URL: GET /api/ranking/comments
     * <p>
     * 权限：公开（无需登录）。
     * <p>
     * 业务说明：
     * - 调用 rankingService.getWeeklyCommentTop10() 获取排行数据。
     * - 统计纬度：本周内各音乐收到的新评论数量（来自 comment 表）。
     * - 按评论数降序排列，取前 10 条。
     * - 评论排行榜反映了音乐的讨论热度。
     * - 高评论数通常意味着歌曲引发了听众的共鸣或讨论。
     * <p>
     * 返回值：Result.success(list) —— 排行榜音乐列表
     */
    @GetMapping("/comments")
    public Result<?> weeklyComments() {
        List<Music> list = rankingService.getWeeklyCommentTop10();
        return Result.success(list);
    }
}
