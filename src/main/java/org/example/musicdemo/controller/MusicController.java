package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.common.ResultCode;
import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.service.CommentService;
import org.example.musicdemo.service.MusicService;
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
 * 音乐控制器，处理音乐列表、详情、上传、下载、删除等HTTP请求
 */
@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final MusicService musicService;
    private final CommentService commentService;

    @Value("${music.file-path}")
    private String filePath;

    private static final Logger log = LoggerFactory.getLogger(MusicController.class);

    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>(Map.of(
        ".mp3", "audio/mpeg",
        ".flac", "audio/flac",
        ".wav", "audio/wav",
        ".aac", "audio/aac",
        ".ogg", "audio/ogg",
        ".m4a", "audio/mp4",
        ".mp4", "audio/mp4"
    ));

    public MusicController(MusicService musicService, CommentService commentService) {
        this.musicService = musicService;
        this.commentService = commentService;
    }

    /**
     * 分页获取音乐列表，可选关键词搜索
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
     * 处理音乐文件上传请求
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
     * 下载音乐文件，记录下载行为
     */
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
                log.warn("记录下载记录失败，不影响文件下载: musicId={}, userId={}", id, userId, e);
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
            response.setStatus(500);
        }
    }

    /**
     * 流式播放音乐文件（给 audio 标签用，不走直接文件路径暴露）
     */
    @GetMapping("/{id}/stream")
    public void stream(@PathVariable Integer id, HttpServletResponse response) {
        Music music = musicService.findById(id);
        if (music == null) {
            response.setStatus(404);
            return;
        }

        File file = new File(filePath + music.getFilePath());
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }

        try {
            String ext = music.getFilePath().substring(music.getFilePath().lastIndexOf('.'));
            String contentType = CONTENT_TYPE_MAP.getOrDefault(ext, "application/octet-stream");
            response.setContentType(contentType);
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
            response.setStatus(500);
        }
    }

    /**
     * 删除音乐，仅创建者或管理员可操作
     */
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

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }

    private Integer getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "").toLowerCase();
    }
}
