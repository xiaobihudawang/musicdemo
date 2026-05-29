package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.common.ResultCode;
import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.service.CommentService;
import org.example.musicdemo.service.MusicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 音乐控制器 —— 核心资源控制器，处理音乐的所有 CRUD 操作及文件传输。
 * <p>
 * 所有端点以 /api/music 开头。
 * <p>
 * 安全说明：
 * - GET 请求（列表、详情、流式播放）：根据 SecurityConfig 配置可能为公开或需认证。
 *   默认配置中 "GET /api/**" 是公开的，即无需登录即可浏览音乐。
 * - POST /api/music/upload：需要登录（上传者关联 userId）。
 * - DELETE /api/music/{id}：需要登录，且仅创建者本人或 ADMIN 可删除。
 * - GET /api/music/{id}/download：可能为公开或需认证（下载计数依赖 userId）。
 * <p>
 * 文件存储：
 * - 音乐文件保存在配置的 music.file-path 目录下（默认为 D:/workspace/music/）。
 * - WebConfig 中通过资源映射将 /api/music/file/** 映射到此目录。
 * - 数据库中只存储相对路径（filePath 字段），绝对路径由 Controller 拼接。
 * <p>
 * 多媒体类型支持：mp3、flac、wav、aac、ogg、m4a、mp4（参见 CONTENT_TYPE_MAP）。
 */
@RestController
@RequestMapping("/api/music")
public class MusicController {

    /** 音乐服务层 —— 提供音乐的 CRUD、上传、下载等业务逻辑 */
    private final MusicService musicService;
    /** 评论服务层 —— 用于获取音乐关联的评论列表 */
    private final CommentService commentService;

    /**
     * 音乐文件存储根目录。
     * 从 application.yml 的 music.file-path 配置项注入。
     * 默认值：D:/workspace/music/
     */
    @Value("${music.file-path}")
    private String filePath;

    /** SLF4J 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(MusicController.class);

    /**
     * 文件扩展名到 MIME 类型的映射表。
     * <p>
     * 用于在下载/流式播放时正确设置 Content-Type 响应头。
     * 不支持的扩展名默认使用 "application/octet-stream"（通用二进制流）。
     * <p>
     * 注意：.mp4 和 .m4a 虽然扩展名不同，但 MIME 类型相同（audio/mp4），
     * 因为 m4a 本质上是 MP4 容器格式的纯音频版本。
     */
    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>(Map.of(
        ".mp3", "audio/mpeg",        // MPEG Audio Layer III，最通用的音频格式
        ".flac", "audio/flac",       // 自由无损音频编解码器
        ".wav", "audio/wav",         // 波形音频文件格式（未压缩）
        ".aac", "audio/aac",         // 高级音频编码
        ".ogg", "audio/ogg",         // Ogg Vorbis 开源音频格式
        ".m4a", "audio/mp4",         // MPEG-4 音频（纯音频）
        ".mp4", "audio/mp4"          // MPEG-4 视频（对 audio 标签<source>仍可识别）
    ));

    /**
     * 构造器注入 MusicService 和 CommentService。
     *
     * @param musicService   音乐服务
     * @param commentService 评论服务
     */
    public MusicController(MusicService musicService, CommentService commentService) {
        this.musicService = musicService;
        this.commentService = commentService;
    }

    /**
     * 分页获取音乐列表，支持关键词搜索
     * <p>
     * URL: GET /api/music/list?page=1&size=10&keyword=周杰伦
     * <p>
     * 查询参数：
     * - page（可选，默认 1）：当前页码（从 1 开始）
     * - size（可选，默认 10）：每页数量
     * - keyword（可选）：搜索关键词（按歌名或歌手模糊匹配）
     * <p>
     * 权限：公开（无需登录）。
     * <p>
     * 业务说明：
     * - 调用 musicService.list(page, size, keyword) 获取分页数据。
     *   - Service 层使用 MyBatis 分页查询（可能使用 PageHelper 或手动 LIMIT）。
     *   - keyword 为 null 或空时返回全部音乐。
     * - 调用 musicService.count(keyword) 获取匹配条件的总记录数（用于前端分页组件）。
     * - 返回的分页信息包括：list（当前页数据）、total（总数）、page（当前页码）、size（每页大小）。
     * <p>
     * 返回值示例：
     * {"code":200, "data":{"list":[...], "total":100, "page":1, "size":10}}
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword) {
        List<Music> list = musicService.list(page, size, keyword);
        int total = musicService.count(keyword);
        return Result.success(Map.of("list", list, "total", total, "page", page, "size", size));
    }

    /**
     * 获取音乐详情及对应评论列表
     * <p>
     * URL: GET /api/music/{id}
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 权限：公开（无需登录）。
     * <p>
     * 业务说明：
     * - 根据 id 查询音乐基本信息（歌名、歌手、封面、文件路径、上传者等）。
     * - 若音乐不存在，返回 404 NOT_FOUND 错误。
     * - 同时查询该音乐下的全部评论（commentService.listByMusicId(id)）。
     * - 评论和音乐信息合并到一个 Map 中返回，减少前端请求次数。
     * <p>
     * 返回值：
     * - 成功：{"code":200, "data":{"music":{...}, "comments":[{...}]}}
     * - 不存在：{"code":404, "message":"资源不存在"}
     */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        List<Comment> comments = commentService.listByMusicId(id);
        return Result.success(Map.of("music", music, "comments", comments));
    }

    /**
     * 上传音乐文件
     * <p>
     * URL: POST /api/music/upload
     * <p>
     * 请求格式：multipart/form-data（文件 + 表单字段）
     * - file（必填）：上传的音乐文件（MultipartFile）
     * - title（必填）：歌曲名称
     * - artist（必填）：歌手名称
     * - description（可选）：歌曲描述 / 简介
     * <p>
     * 权限：需要用户登录（上传者身份通过 JWT 获取）。
     * <p>
     * 业务说明：
     * 1. 从安全上下文中获取当前用户 ID（上传者）。
     * 2. 调用 musicService.upload(file, title, artist, description, userId)。
     *    - Service 层执行以下操作：
     *      a. 校验文件格式是否支持（检查文件扩展名）。
     *      b. 生成唯一文件名（UUID + 原始扩展名，防止文件名冲突）。
     *      c. 将文件保存到 music.file-path 目录。
     *      d. 在 music 表中插入一条新记录（包含文件路径、歌名、歌手等）。
     *      e. 返回完整的 Music 实体（含数据库生成的 id 等）。
     * 3. 上传成功返回 Music 实体信息。
     * 4. 异常处理：
     *    - RuntimeException：业务异常（如格式不支持），消息直接返回。
     *    - IOException：文件系统异常（如磁盘空间不足），返回通用错误消息。
     * <p>
     * 返回值：
     * - 成功：{"code":200, "data":{"id":1, "title":"...", "filePath":"...", ...}}
     * - 失败：{"code":400, "message":"不支持的文件格式"}
     */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam String title,
                            @RequestParam String artist,
                            @RequestParam(required = false) String description) {
        try {
            Integer userId = getCurrentUserId();
            Music music = musicService.upload(file, title, artist, description, userId);
            return Result.success(music);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (IOException e) {
            return Result.fail("文件上传失败");
        }
    }

    /**
     * 下载音乐文件（记录下载行为）
     * <p>
     * URL: GET /api/music/{id}/download
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 权限：公开或需认证均可（若未登录，下载统计会跳过）。
     * <p>
     * 业务说明：
     * 1. 查询音乐是否存在，不存在则返回 404。
     * 2. 尝试获取当前用户 ID（getCurrentUserIdOrNull）。
     *    - 若已登录且有 userId，调用 musicService.download(id, userId) 记录下载行为。
     *    - 若未登录（Authentication 为 null），跳过计数，不影响文件下载。
     *    - 下载记录失败（如数据库异常）仅记录警告日志，不阻断文件下载。
     * 3. 构建文件对象，检查文件是否存在于磁盘，不存在则返回 404。
     * 4. 根据文件扩展名设置 Content-Type 响应头。
     * 5. 设置 Content-Disposition: attachment，触发浏览器下载。
     *    - 文件名使用 URLEncoder 编码，支持中文（UTF-8）。
     * 6. 使用 8KB 缓冲区流式传输文件内容。
     * <p>
     * 文件定位：
     * - 文件路径 = filePath（配置目录） + music.getFilePath()（数据库中存储的相对路径）。
     * - 例如：D:/workspace/music/uploads/uuid-song.mp3
     * <p>
     * 返回值：直接返回文件二进制流（不是 JSON），错误时设置 HTTP 状态码后返回。
     *
     * @param id       音乐 ID
     * @param response HTTP 响应对象，用于输出文件流
     */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Integer id, HttpServletResponse response) {
        Music music = musicService.findById(id);
        if (music == null) {
            response.setStatus(404);
            return;
        }

        // 尝试记录下载行为（不影响文件下载流程）
        Integer userId = getCurrentUserIdOrNull();
        if (userId != null) {
            try {
                musicService.download(id, userId);
            } catch (RuntimeException e) {
                log.warn("记录下载记录失败，不影响文件下载: musicId={}, userId={}", id, userId, e);
            }
        }

        File file = new File(filePath + music.getFilePath());
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }

        try {
            // 根据文件扩展名确定 Content-Type
            String fileName = music.getFilePath();
            String ext = fileName.substring(fileName.lastIndexOf('.'));
            String contentType = CONTENT_TYPE_MAP.getOrDefault(ext, "application/octet-stream");
            response.setContentType(contentType);
            // 设置为附件下载（Content-Disposition: attachment）
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(music.getTitle() + ext, StandardCharsets.UTF_8));
            response.setContentLengthLong(file.length());

            // 流式输出文件内容
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    /**
     * 流式播放音乐文件（供 HTML5 &lt;audio&gt; 标签使用）
     * <p>
     * URL: GET /api/music/{id}/stream
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 权限：公开（无需登录）。
     * <p>
     * 业务说明：
     * - 与 download 方法类似，但不设置 Content-Disposition: attachment。
     * - 浏览器识别到 Content-Type 为音频类型（如 audio/mpeg）后，会直接播放而不是下载。
     * - 这层抽象可以避免直接暴露文件系统路径给前端。
     *   - 前端只需要知道 musicId，通过 /api/music/{id}/stream 即可播放。
     *   - 无需在 HTML 中写 file:// 或直接的文件路径。
     * - 也便于后续添加权限控制（例如 VIP 用户才能播放）。
     * <p>
     * 文件定位和流式传输逻辑与 download 方法基本一致。
     *
     * @param id       音乐 ID
     * @param response HTTP 响应对象，用于输出音频流
     */
    @GetMapping("/{id}/stream")
    public void stream(@PathVariable Integer id, HttpServletRequest request, HttpServletResponse response) {
        Music music = musicService.findById(id);
        if (music == null) { response.setStatus(404); return; }

        File file = new File(filePath + music.getFilePath());
        if (!file.exists()) { response.setStatus(404); return; }

        String ext = music.getFilePath().substring(music.getFilePath().lastIndexOf('.'));
        String contentType = CONTENT_TYPE_MAP.getOrDefault(ext, "application/octet-stream");
        long fileLength = file.length();

        String rangeHeader = request.getHeader("Range");

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                // 无 Range 请求，返回完整文件
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(contentType);
                response.setContentLengthLong(fileLength);
                response.setHeader("Accept-Ranges", "bytes");
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) os.write(buf, 0, len);
                os.flush();
                return;
            }

            // 解析 Range: bytes=start-end
            String range = rangeHeader.substring("bytes=".length()).trim();
            long start, end;
            int dashIdx = range.indexOf('-');
            if (dashIdx == 0) {
                start = fileLength - Long.parseLong(range.substring(1));
                end = fileLength - 1;
            } else {
                start = Long.parseLong(range.substring(0, dashIdx));
                end = dashIdx < range.length() - 1 ? Long.parseLong(range.substring(dashIdx + 1)) : fileLength - 1;
            }
            if (start > end || start >= fileLength) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }
            end = Math.min(end, fileLength - 1);
            long contentLength = end - start + 1;

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setContentType(contentType);
            response.setContentLengthLong(contentLength);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);

            fis.getChannel().position(start);
            byte[] buf = new byte[8192];
            long remaining = contentLength;
            int len;
            while (remaining > 0 && (len = fis.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                os.write(buf, 0, len);
                remaining -= len;
            }
            os.flush();

        } catch (IOException e) {
            if (!response.isCommitted()) response.setStatus(500);
        }
    }

    /**
     * 删除音乐（仅创建者本人或管理员可操作）
     * <p>
     * URL: DELETE /api/music/{id}
     * <p>
     * 路径参数：id —— 音乐 ID
     * <p>
     * 权限：需要登录。仅当当前用户是音乐的创建者（userId 匹配）或具有 ADMIN 角色时允许。
     * <p>
     * 业务说明：
     * 1. 根据 id 查询音乐是否存在，不存在则返回 404 NOT_FOUND。
     * 2. 获取当前用户的 ID 和角色：
     *    - getCurrentUserId()：从 SecurityContext 获取 userId。
     *    - getCurrentUserRole()：从 SecurityContext 获取角色（去掉 ROLE_ 前缀）。
     * 3. 权限校验：
     *    - 条件：userId != music.userId && role != "admin"
     *    - 即既不是上传者也不是管理员 → 返回 403 FORBIDDEN。
     * 4. 调用 musicService.delete(id) 执行删除。
     *    - Service 层会删除数据库中该音乐的记录。
     *    - 可能会同时删除磁盘上的物理文件（取决于 Service 层实现）。
     *    - 可能会级联删除该音乐相关的评论/点赞记录。
     * <p>
     * 返回值：
     * - 成功：Result.success()
     * - 音乐不存在：Result.fail(ResultCode.NOT_FOUND)   → HTTP 200 + code=404
     * - 无权限：Result.fail(ResultCode.FORBIDDEN)        → HTTP 200 + code=403
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Integer userId = getCurrentUserId();
        String role = getCurrentUserRole();

        // 鉴权：上传者本人或管理员可删除
        if (!userId.equals(music.getUserId()) && !"admin".equals(role)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }

        musicService.delete(id);
        return Result.success();
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户的 ID。
     * <p>
     * 实现依赖于 JwtAuthenticationFilter：
     * - 该过滤器从请求头中提取 JWT Token。
     * - 解析后以 userId（Integer）作为 principal 创建认证对象。
     * - 设置到 SecurityContextHolder 中。
     *
     * @return 当前登录用户的 ID
     * @throws NullPointerException 若当前请求未认证（Authentication 为 null）
     */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }

    /**
     * 安全地获取当前用户 ID，若未认证则返回 null（不抛出异常）。
     * <p>
     * 用于 download 等端点：下载功能对未登录用户也开放，
     * 但若已登录则额外记录下载行为。此方法避免下载流程被认证异常阻断。
     *
     * @return 当前用户 ID（已登录），或 null（未登录）
     */
    private Integer getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户的角色。
     * <p>
     * 角色在 GrantedAuthority 中以 "ROLE_ADMIN" 或 "ROLE_USER" 格式存储。
     * 此方法提取第一个权限（通常只有一个），去掉 "ROLE_" 前缀，转为小写。
     * <p>
     * 转换示例：ROLE_ADMIN → "admin", ROLE_USER → "user"
     *
     * @return 角色字符串（小写），如 "admin"、"user"
     */
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "").toLowerCase();
    }
}
