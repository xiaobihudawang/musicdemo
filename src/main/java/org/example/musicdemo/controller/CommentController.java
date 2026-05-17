package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.common.ResultCode;
import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.service.CommentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器，处理评论的增删查HTTP请求
 */
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 获取指定音乐的所有评论
     */
    @GetMapping("/music/{id}/comments")
    public Result<?> list(@PathVariable Integer id) {
        List<Comment> comments = commentService.listByMusicId(id);
        return Result.success(comments);
    }

    /**
     * 为指定音乐添加评论
     */
    @PostMapping("/music/{id}/comments")
    public Result<?> add(@PathVariable Integer id, @RequestBody Comment comment) {
        comment.setMusicId(id);
        comment.setUserId(getCurrentUserId());
        return Result.success(commentService.add(comment));
    }

    /**
     * 删除评论，仅评论者或管理员可操作
     */
    @DeleteMapping("/comments/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        Comment comment = commentService.findById(id);
        if (comment == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Integer userId = getCurrentUserId();
        String role = getCurrentUserRole();

        if (!userId.equals(comment.getUserId()) && !"admin".equals(role)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }

        commentService.delete(id, comment.getMusicId());
        return Result.success();
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "").toLowerCase();
    }
}
