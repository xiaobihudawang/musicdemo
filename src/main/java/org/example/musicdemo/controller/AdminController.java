package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.common.ResultCode;
import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.entity.User;
import org.example.musicdemo.service.CommentService;
import org.example.musicdemo.service.MusicService;
import org.example.musicdemo.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器，提供用户管理、音乐和评论的删除操作
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final MusicService musicService;
    private final CommentService commentService;

    public AdminController(UserService userService, MusicService musicService,
                           CommentService commentService) {
        this.userService = userService;
        this.musicService = musicService;
        this.commentService = commentService;
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public Result<?> users() {
        List<User> list = userService.findAll();
        return Result.success(list);
    }

    /**
     * 切换用户启用/禁用状态
     */
    @PutMapping("/users/{id}/status")
    public Result<?> toggleUserStatus(@PathVariable Integer id, @RequestBody User user) {
        userService.toggleEnabled(id, user.getEnabled());
        return Result.success();
    }

    /**
     * 管理员删除音乐
     */
    @DeleteMapping("/music/{id}")
    public Result<?> deleteMusic(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        musicService.delete(id);
        return Result.success();
    }

    /**
     * 管理员删除评论
     */
    @DeleteMapping("/comments/{id}")
    public Result<?> deleteComment(@PathVariable Integer id) {
        Comment comment = commentService.findById(id);
        if (comment == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        commentService.delete(id, comment.getMusicId());
        return Result.success();
    }
}
