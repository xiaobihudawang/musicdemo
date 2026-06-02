package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.LikeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 点赞控制器，处理用户对音乐的点赞操作和状态查询。
 * 所有端点以 /api/music 开头，需要登录。
 */
@RestController
@RequestMapping("/api/music")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /** 切换点赞状态（点赞/取消点赞） */
    @PostMapping("/{id}/like")
    public Result<?> toggleLike(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        Map<String, Object> result = likeService.toggle(userId, id);
        return Result.success(result);
    }

    /** 查询当前用户是否已点赞 */
    @GetMapping("/{id}/like/status")
    public Result<?> likeStatus(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        boolean liked = likeService.isLiked(userId, id);
        return Result.success(Map.of("liked", liked));
    }

    /** 从 SecurityContext 获取当前用户 ID */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }
}
