package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.common.ResultCode;
import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.entity.User;
import org.example.musicdemo.mapper.MusicMapper;
import org.example.musicdemo.service.CommentService;
import org.example.musicdemo.service.MusicService;
import org.example.musicdemo.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理员控制器，提供后台管理 API。
 * 所有端点以 /api/admin 开头，需要 ROLE_ADMIN 权限。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final MusicService musicService;
    private final CommentService commentService;
    private final MusicMapper musicMapper;

    @Value("${music.file-path}")
    private String filePath;

    public AdminController(UserService userService, MusicService musicService,
                           CommentService commentService, MusicMapper musicMapper) {
        this.userService = userService;
        this.musicService = musicService;
        this.commentService = commentService;
        this.musicMapper = musicMapper;
    }

    /** 获取所有用户列表 */
    @GetMapping("/users")
    public Result<?> users() {
        List<User> list = userService.findAll();
        return Result.success(list);
    }

    /** 切换用户启用/禁用状态 */
    @PutMapping("/users/{id}/status")
    public Result<?> toggleUserStatus(@PathVariable Integer id, @RequestBody User user) {
        userService.toggleEnabled(id, user.getEnabled());
        return Result.success();
    }

    /** 管理员删除音乐（不需要是创建者） */
    @DeleteMapping("/music/{id}")
    public Result<?> deleteMusic(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        musicService.delete(id);
        return Result.success();
    }

    /** 管理员删除评论（不需要是评论者） */
    @DeleteMapping("/comments/{id}")
    public Result<?> deleteComment(@PathVariable Integer id) {
        Comment comment = commentService.findById(id);
        if (comment == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        commentService.delete(id, comment.getMusicId());
        return Result.success();
    }

    /** 管理员上传/替换音乐封面 */
    @PostMapping("/music/{id}/cover")
    public Result<?> uploadCover(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.fail("文件名不能为空");
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
            return Result.fail("仅支持 JPG、PNG、WebP 格式");
        }

        try {
            String filename = UUID.randomUUID() + ext;
            File dest = new File(filePath + "covers" + File.separator + filename);
            dest.getParentFile().mkdirs();
            file.transferTo(dest);

            String coverPath = "covers/" + filename;
            musicMapper.updateCoverPath(id, coverPath);
            return Result.success(Map.of("coverPath", coverPath));
        } catch (IOException e) {
            return Result.fail(ResultCode.INTERNAL_ERROR);
        }
    }
}
