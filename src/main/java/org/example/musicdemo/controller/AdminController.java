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
 * 管理员控制器 —— 提供系统管理后台的 REST API（所有请求以 /api/admin 开头）。
 * <p>
 * 安全说明：
 * 本控制器的所有端点均通过 Spring Security 的 HTTP 安全配置放行，
 * 但 SecurityConfig 中配置了 .requestMatchers("/api/admin/**").hasRole("ADMIN")，
 * 意味着只有持有 ROLE_ADMIN 角色的用户（JWT 中 role=admin）才能访问。
 * <p>
 * 功能概览：
 * - 查询所有用户列表
 * - 切换用户的启用/禁用状态（封号/解封）
 * - 删除任意音乐（覆盖音乐创建者的删除权限边界）
 * - 删除任意评论（覆盖评论者的删除权限边界）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** 用户服务 —— 提供用户查询、状态切换等操作 */
    private final UserService userService;
    /** 音乐服务 —— 提供音乐的增删改查 */
    private final MusicService musicService;
    /** 评论服务 —— 提供评论的增删查 */
    private final CommentService commentService;
    /** 音乐 Mapper —— 用于更新封面路径 */
    private final MusicMapper musicMapper;

    @Value("${music.file-path}")
    private String filePath;

    /**
     * 构造器注入 —— 无 @Autowired，纯构造方法注入（Spring 官方推荐方式）。
     */
    public AdminController(UserService userService, MusicService musicService,
                           CommentService commentService, MusicMapper musicMapper) {
        this.userService = userService;
        this.musicService = musicService;
        this.commentService = commentService;
        this.musicMapper = musicMapper;
    }

    /**
     * 获取所有用户列表
     * <p>
     * URL: GET /api/admin/users
     * <p>
     * 权限：仅 ADMIN 角色可访问。
     * <p>
     * 业务说明：
     * - 调用 userService.findAll() 获取全部 User 实体列表。
     * - 返回的数据中包含 id、username、name、email、role、enabled 等全部字段。
     * - 不进行分页，适合后台管理的用户总量较少的场景。
     * - 密码字段（password）默认不会返回给前端（需在 User 实体上配置 @JsonIgnore 等）。
     * <p>
     * 返回值：Result.success(list) —— 用户列表包装在 Result 的 data 字段中。
     */
    @GetMapping("/users")
    public Result<?> users() {
        List<User> list = userService.findAll();
        return Result.success(list);
    }

    /**
     * 切换用户的启用/禁用状态（封号或解封）
     * <p>
     * URL: PUT /api/admin/users/{id}/status
     * <p>
     * 请求体 JSON 示例：{"enabled": false}   （false = 禁用，true = 启用）
     * <p>
     * 权限：仅 ADMIN 角色可访问。
     * <p>
     * 业务说明：
     * - 路径变量 @PathVariable Integer id 指定目标用户的 ID。
     * - @RequestBody User user 只读取 enabled 字段，其余字段忽略。
     * - 核心逻辑在 userService.toggleEnabled(id, enabled) 中：
     *   1. 根据 id 查找用户（若不存在则抛出异常）。
     *   2. 设置 user.setEnabled(enabled) 并更新到数据库。
     * - 被禁用的用户无法登录（AuthController.login 中有 !user.getEnabled() 的检查）。
     * - 禁止禁用超级管理员自身（通常在 Service 层校验），以防误操作。
     * <p>
     * 返回值：Result.success() —— 无 data，仅包含成功状态码。
     */
    @PutMapping("/users/{id}/status")
    public Result<?> toggleUserStatus(@PathVariable Integer id, @RequestBody User user) {
        userService.toggleEnabled(id, user.getEnabled());
        return Result.success();
    }

    /**
     * 管理员强制删除音乐（不需要是音乐的创建者）
     * <p>
     * URL: DELETE /api/admin/music/{id}
     * <p>
     * 权限：仅 ADMIN 角色可访问。
     * <p>
     * 业务说明：
     * - 先根据 id 查询音乐是否存在（findById），若不存在则返回 404 NOT_FOUND。
     * - 存在则调用 musicService.delete(id) 执行物理删除。
     * - 与普通用户的删除不同（MusicController.delete 需要 userId 匹配），
     *   管理员可以删除任意用户上传的音乐。
     * - 删除操作会连带删除该音乐相关的评论（由 Service 层或级联约束处理）。
     * <p>
     * 返回值：
     * - 成功：Result.success()
     * - 音乐不存在：Result.fail(ResultCode.NOT_FOUND)  → HTTP 200 + code=404
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
     * 管理员强制删除评论（不需要是评论者本人）
     * <p>
     * URL: DELETE /api/admin/comments/{id}
     * <p>
     * 权限：仅 ADMIN 角色可访问。
     * <p>
     * 业务说明：
     * - 先根据 id 查询评论是否存在，若不存在则返回 404 NOT_FOUND。
     * - 调用 commentService.delete(id, musicId) 执行删除。
     *   - 第二个参数 musicId 用于在删除后更新该音乐的评论计数（如果有缓存或冗余字段）。
     * - 与 CommentController.delete 不同：不校验当前用户 ID 是否等于评论的 userId。
     * <p>
     * 返回值：
     * - 成功：Result.success()
     * - 评论不存在：Result.fail(ResultCode.NOT_FOUND)
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

    /**
     * 管理员手动上传/替换音乐封面
     * <p>
     * URL: POST /api/admin/music/{id}/cover
     * <p>
     * 请求格式：multipart/form-data
     * - file（必填）：封面图片文件（jpg/png/webp）
     * <p>
     * 权限：仅 ADMIN。
     */
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
            return Result.fail("封面上传失败");
        }
    }
}
