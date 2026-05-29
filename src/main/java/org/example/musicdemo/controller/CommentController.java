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
 * 评论控制器 —— 处理音乐评论的查询、添加和删除操作。
 * <p>
 * 所有端点以 /api 开头（注意是 /api 下的子路径映射）。
 * <p>
 * 安全说明：
 * - GET /api/music/{id}/comments：公开（查看评论不需要登录）。
 * - POST /api/music/{id}/comments：需要登录（需要获取当前用户 ID 作为评论者）。
 * - DELETE /api/comments/{id}：需要登录，且仅评论者本人或 ADMIN 角色可操作。
 * <p>
 * 数据库关联：
 * - Comment 表通过 music_id 与 Music 表关联。
 * - Comment 表通过 user_id 与 User 表关联。
 * - 删除音乐时通常会级联删除关联的评论。
 */
@RestController
@RequestMapping("/api")
public class CommentController {

    /** 评论服务层 —— 提供评论的 CRUD 操作 */
    private final CommentService commentService;

    /**
     * 构造器注入 CommentService。
     *
     * @param commentService 评论服务
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 获取指定音乐的所有评论
     * <p>
     * URL: GET /api/music/{id}/comments
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 权限：公开（无需登录）。
     * <p>
     * 业务说明：
     * - 调用 commentService.listByMusicId(id) 查询。
     * - 返回该音乐下所有评论，按时间倒序排列（由 Service 层决定排序规则）。
     * - 每条评论包含评论内容、评论者信息（用户名）、评论时间等。
     * - 不进行分页，适合评论量不大的场景（若评论量大会导致性能问题）。
     * <p>
     * 返回值：Result.success(comments) —— 评论列表包装在 data 字段中。
     */
    @GetMapping("/music/{id}/comments")
    public Result<?> list(@PathVariable Integer id) {
        List<Comment> comments = commentService.listByMusicId(id);
        return Result.success(comments);
    }

    /**
     * 为指定音乐添加评论
     * <p>
     * URL: POST /api/music/{id}/comments
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 请求体 JSON：{"content": "评论内容"}
     * <p>
     * 权限：需要用户登录（从 JWT 中提取 userId）。
     * <p>
     * 业务说明：
     * - 从路径中获取音乐 ID（comment.setMusicId(id)）。
     * - 从安全上下文中获取当前用户 ID（comment.setUserId(getCurrentUserId())）。
     * - 调用 commentService.add(comment) 执行插入。
     *   - Service 层可能设置创建时间、校验内容非空等。
     *   - 可能更新该音乐的评论计数（如有冗余字段）。
     * - 返回添加成功后的 Comment 对象（包含生成的 id 和时间戳）。
     * <p>
     * 返回值：Result.success(comment) —— 包含新评论的完整信息。
     */
    @PostMapping("/music/{id}/comments")
    public Result<?> add(@PathVariable Integer id, @RequestBody Comment comment) {
        comment.setMusicId(id);                         // 设置所属音乐 ID
        comment.setUserId(getCurrentUserId());          // 设置评论者 ID
        return Result.success(commentService.add(comment));  // 添加并返回完整评论
    }

    /**
     * 删除评论（评论者本人或管理员可操作）
     * <p>
     * URL: DELETE /api/comments/{id}
     * <p>
     * 路径参数：id —— 评论 ID
     * <p>
     * 权限：需要登录。评论者本人或 ADMIN 角色可删除，他人无权限。
     * <p>
     * 业务说明：
     * 1. 根据 id 查询评论是否存在，若不存在返回 404 NOT_FOUND。
     * 2. 获取当前用户的 ID 和角色：
     *    - getCurrentUserId()：从 SecurityContext 提取 userId。
     *    - getCurrentUserRole()：从 SecurityContext 提取角色，去掉 "ROLE_" 前缀并转小写。
     * 3. 权限校验：
     *    - 条件：userId != comment.userId && role != "admin"
     *    - 即：既不是评论者本人，也不是管理员 → 返回 403 FORBIDDEN。
     * 4. 调用 commentService.delete(id, musicId) 执行删除。
     *    - 第二个参数 musicId 用于更新音乐的评论计数。
     * <p>
     * 返回值：
     * - 成功：Result.success()
     * - 评论不存在：Result.fail(ResultCode.NOT_FOUND)   → HTTP 200 + code=404
     * - 无权限：Result.fail(ResultCode.FORBIDDEN)        → HTTP 200 + code=403
     */
    @DeleteMapping("/comments/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        Comment comment = commentService.findById(id);
        if (comment == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Integer userId = getCurrentUserId();
        String role = getCurrentUserRole();

        // 权限校验：只有评论者本人或管理员才能删除
        if (!userId.equals(comment.getUserId()) && !"admin".equals(role)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }

        commentService.delete(id, comment.getMusicId());
        return Result.success();
    }

    /**
     * 从 Spring Security 上下文获取当前登录用户的 ID。
     * <p>
     * 通过 SecurityContextHolder 获取 Authentication 对象，
     * 其 principal 字段在 JwtAuthenticationFilter 中被设置为 userId（Integer 类型）。
     *
     * @return 当前登录用户的 ID
     */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }

    /**
     * 从 Spring Security 上下文获取当前登录用户的角色。
     * <p>
     * Authentication.getAuthorities() 返回 GrantedAuthority 集合，
     * 其中存储的是 "ROLE_USER" 或 "ROLE_ADMIN" 格式的权限标识。
     * 此方法取第一个权限（通常只有一个），去掉 "ROLE_" 前缀并转为小写。
     * <p>
     * 转换结果：ROLE_ADMIN → "admin", ROLE_USER → "user"
     *
     * @return 当前用户的角色字符串（小写，如 "admin"、"user"）
     */
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "").toLowerCase();
    }
}
