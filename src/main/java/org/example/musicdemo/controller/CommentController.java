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
 * 评论控制器，处理音乐评论的查询、添加和删除。
 * 所有端点以 /api 开头。
 */
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 获取指定音乐的所有评论 */
    @GetMapping("/music/{id}/comments")
    public Result<?> list(@PathVariable Integer id) {
        List<Comment> comments = commentService.listByMusicId(id);
        return Result.success(comments);
    }

    /** 为指定音乐添加评论（需登录） */
    @PostMapping("/music/{id}/comments")
    public Result<?> add(@PathVariable Integer id, @RequestBody Comment comment) {
        comment.setMusicId(id);
        comment.setUserId(getCurrentUserId());
        return Result.success(commentService.add(comment));
    }

    /** 删除评论（评论者本人或管理员可操作） */
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

    /** 从 SecurityContext 获取当前用户 ID */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }

    /** 获取当前用户角色（去掉 ROLE_ 前缀，转小写） */
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "").toLowerCase();
    }
}
