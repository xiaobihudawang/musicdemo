package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.LikeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 点赞控制器 —— 处理用户对音乐的点赞操作和状态查询。
 * <p>
 * 所有端点以 /api/music 开头（与 MusicController 共享同一基础路径）。
 * <p>
 * 安全说明：
 * - POST /api/music/{id}/like 和 GET /api/music/{id}/like/status 均需要用户登录。
 * - 接口路径设计为 RESTful 风格，嵌套在音乐资源下。
 * <p>
 * 数据库关联：
 * - LikeRecord 表记录每次点赞行为，包含 user_id、music_id、create_time。
 * - 点赞是"切换"操作：已赞则取消，未赞则新增。
 * - 每周点赞排行由 RankingService 基于 LikeRecord 统计。
 */
@RestController
@RequestMapping("/api/music")
public class LikeController {

    /** 点赞服务层 —— 提供点赞状态切换、查询等业务逻辑 */
    private final LikeService likeService;

    /**
     * 构造器注入 LikeService。
     *
     * @param likeService 点赞服务
     */
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /**
     * 切换当前用户对指定音乐的点赞状态（点赞 / 取消点赞）
     * <p>
     * URL: POST /api/music/{id}/like
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 权限：需要用户登录。
     * <p>
     * 业务说明：
     * - 每次调用都会切换状态：如果当前用户已点赞则取消，未点赞则新增。
     * - 调用 likeService.toggle(userId, musicId) 实现。
     *   - Service 层先查询 LikeRecord 表中是否有该 userId + musicId 的记录。
     *   - 存在则删除记录（取消赞），不存在则插入新记录（点赞）。
     *   - 返回一个 Map，通常包含：
     *     - "liked": true/false（操作后的点赞状态）
     *     - "likeCount": 该音乐的最新点赞总数（可选）
     * - 这是一个幂等性操作吗？不完全是：连续调用两次会回到原始状态。
     *   但对服务器来说，每次调用都会产生明确的数据库变更。
     * <p>
     * 返回值示例：
     * - 点赞成功：{"code":200, "data":{"liked":true, "likeCount":42}}
     * - 取消点赞：{"code":200, "data":{"liked":false, "likeCount":41}}
     */
    @PostMapping("/{id}/like")
    public Result<?> toggleLike(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        Map<String, Object> result = likeService.toggle(userId, id);
        return Result.success(result);
    }

    /**
     * 查询当前用户对指定音乐的点赞状态
     * <p>
     * URL: GET /api/music/{id}/like/status
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 权限：需要用户登录。
     * <p>
     * 业务说明：
     * - 查询当前用户是否已给该音乐点过赞。
     * - 调用 likeService.isLiked(userId, musicId) 实现。
     *   - Service 层在 LikeRecord 表中查找 userId + musicId 的记录。
     *   - 存在返回 true，不存在返回 false。
     * - 前端常用此接口初始化点赞按钮的显示状态（实心/空心）。
     * <p>
     * 返回值示例：
     * - 已点赞：{"code":200, "data":{"liked":true}}
     * - 未点赞：{"code":200, "data":{"liked":false}}
     */
    @GetMapping("/{id}/like/status")
    public Result<?> likeStatus(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        boolean liked = likeService.isLiked(userId, id);
        return Result.success(Map.of("liked", liked));
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户的 ID。
     * <p>
     * 通过 SecurityContextHolder 获取 Authentication 对象，
     * 其 principal 为 Integer 类型的 userId（由 JwtAuthenticationFilter 设置）。
     *
     * @return 当前登录用户的 ID
     */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }
}
