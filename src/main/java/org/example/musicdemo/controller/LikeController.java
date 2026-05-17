package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.LikeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 点赞控制器，处理点赞状态查询及切换
 */
@RestController
@RequestMapping("/api/music")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /**
     * 切换当前用户对指定音乐的点赞状态
     */
    @PostMapping("/{id}/like")
    public Result<?> toggleLike(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        Map<String, Object> result = likeService.toggle(userId, id);
        return Result.success(result);
    }

    /**
     * 查询当前用户对指定音乐的点赞状态
     */
    @GetMapping("/{id}/like/status")
    public Result<?> likeStatus(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        boolean liked = likeService.isLiked(userId, id);
        return Result.success(Map.of("liked", liked));
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }
}
