package org.example.musicdemo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.BilibiliService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/bilibili")
public class BilibiliController {

    private static final Logger log = LoggerFactory.getLogger(BilibiliController.class);

    private final BilibiliService bilibiliService;

    public BilibiliController(BilibiliService bilibiliService) {
        this.bilibiliService = bilibiliService;
    }

    @PostMapping("/download")
    public void download(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String url = body.get("url");
        log.info("收到 B 站下载请求, url=[{}]", url);

        if (url == null || url.isBlank()) {
            writeError(response, 400, "请提供 B 站视频链接");
            return;
        }

        try {
            log.debug("提取 BV 号");
            String bvid = bilibiliService.extractBvid(url);
            log.info("BV 号提取成功, bvid=[{}]", bvid);

            log.debug("调用 Python 获取音频 URL");
            BilibiliService.AudioInfo audioInfo = bilibiliService.getAudioInfo(bvid);
            String title = audioInfo.getTitle();
            String audioUrl = audioInfo.getAudioUrl();

            log.info("开始透传音频流, title=[{}]", title);

            HttpURLConnection conn = (HttpURLConnection) new URL(audioUrl).openConnection();
            conn.setRequestProperty("Referer", "https://www.bilibili.com");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.connect();

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                log.error("B 站音频源返回非 200, statusCode={}", statusCode);
                writeError(response, 502, "B 站音频源返回 " + statusCode);
                return;
            }

            String contentType = conn.getContentType();
            if (contentType == null) contentType = "audio/mp4";

            String encodedFilename = URLEncoder.encode(title + ".m4a", StandardCharsets.UTF_8)
                    .replace("+", "%20");

            response.setContentType(contentType);
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFilename);

            long contentLength = conn.getContentLengthLong();
            if (contentLength > 0) {
                response.setContentLengthLong(contentLength);
            }

            try (InputStream is = conn.getInputStream();
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.info("音频透传完成, title=[{}]", title);

        } catch (Exception e) {
            log.error("B 站下载失败: {}", e.getMessage(), e);
            writeError(response, 500, "下载失败: " + e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            String json = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                    status, message.replace("\"", "\\\""));
            response.getWriter().write(json);
        } catch (Exception ignored) {}
    }
}
