package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.common.ResultCode;
import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.service.CommentService;
import org.example.musicdemo.service.LyricsService;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 音乐控制器，处理音乐的 CRUD、上传、下载、流式播放。
 * 所有端点以 /api/music 开头。
 */
@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final MusicService musicService;
    private final CommentService commentService;
    private final LyricsService lyricsService;

    @Value("${music.file-path}")
    private String filePath;

    private static final Logger log = LoggerFactory.getLogger(MusicController.class);

    /** 文件扩展名到 MIME 类型的映射 */
    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>(Map.of(
        ".mp3", "audio/mpeg",
        ".flac", "audio/flac",
        ".wav", "audio/wav",
        ".aac", "audio/aac",
        ".ogg", "audio/ogg",
        ".m4a", "audio/mp4",
        ".mp4", "audio/mp4"
    ));

    public MusicController(MusicService musicService, CommentService commentService, LyricsService lyricsService) {
        this.musicService = musicService;
        this.commentService = commentService;
        this.lyricsService = lyricsService;
    }

    /** 分页获取音乐列表，支持关键词搜索 */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword) {
        if (size > 50) size = 50;
        if (size < 1) size = 1;
        List<Music> list = musicService.list(page, size, keyword);
        int total = musicService.count(keyword);
        return Result.success(Map.of("list", list, "total", total, "page", page, "size", size));
    }

    /** 获取音乐详情及评论列表 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        List<Comment> comments = commentService.listByMusicId(id);
        return Result.success(Map.of("music", music, "comments", comments));
    }

    /** 上传音乐文件 */
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
            return Result.fail(ResultCode.INTERNAL_ERROR);
        }
    }

    /** 下载音乐文件（记录下载行为） */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Integer id, HttpServletResponse response) {
        Music music = musicService.findById(id);
        if (music == null) {
            response.setStatus(404);
            return;
        }

        Integer userId = getCurrentUserIdOrNull();
        if (userId != null) {
            try {
                musicService.download(id, userId);
            } catch (RuntimeException e) {
                log.warn("记录下载记录失败: musicId={}, userId={}", id, userId, e);
            }
        }

        File file = new File(filePath + music.getFilePath());
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }

        try {
            String fileName = music.getFilePath();
            String ext = fileName.substring(fileName.lastIndexOf('.'));
            String contentType = CONTENT_TYPE_MAP.getOrDefault(ext, "application/octet-stream");
            response.setContentType(contentType);
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(music.getTitle() + ext, StandardCharsets.UTF_8));
            response.setContentLengthLong(file.length());

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
            response.setStatus(ResultCode.INTERNAL_ERROR.getCode());
        }
    }

    /** 流式播放音乐（供 HTML5 audio 标签使用），支持 Range 请求 */
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
            if (!response.isCommitted()) response.setStatus(ResultCode.INTERNAL_ERROR.getCode());
        }
    }

    /** 获取音乐歌词 */
    @GetMapping("/{id}/lyrics")
    public Result<?> getLyrics(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        return Result.success(Map.of("lyrics", music.getLyrics()));
    }

    /** 手动重新获取歌词 */
    @PostMapping("/{id}/lyrics/regenerate")
    public Result<?> regenerateLyrics(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        String lyrics = lyricsService.regenerateLyrics(id, music.getTitle(), music.getArtist());
        return Result.success(Map.of("lyrics", lyrics));
    }

    /** 删除音乐（仅创建者或管理员可操作） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        Music music = musicService.findById(id);
        if (music == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Integer userId = getCurrentUserId();
        String role = getCurrentUserRole();

        if (!userId.equals(music.getUserId()) && !"admin".equals(role)) {
            return Result.fail(ResultCode.FORBIDDEN);
        }

        musicService.delete(id);
        return Result.success();
    }

    /** 从 SecurityContext 获取当前用户 ID */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }

    /** 安全获取当前用户 ID，未登录返回 null */
    private Integer getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取当前用户角色（去掉 ROLE_ 前缀，转小写） */
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "").toLowerCase();
    }
}
