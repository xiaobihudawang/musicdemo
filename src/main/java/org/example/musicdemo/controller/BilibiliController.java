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

/**
 * Bilibili 控制器 —— 提供 B 站（Bilibili）视频音频下载转发功能。
 * <p>
 * 所有端点以 /api/bilibili 开头。
 * 安全说明：此端点可能配置为公开或需认证，根据 SecurityConfig 的具体配置而定。
 * 若需限制使用，可配置 .requestMatchers("/api/bilibili/**").authenticated()。
 * <p>
 * 核心功能：
 * - 接收前端传入的 B 站视频链接。
 * - 提取 BV 号（B 站视频唯一标识）。
 * - 调用 Python 脚本/服务获取视频的音频流真实 URL。
 * - 将音频流透传（proxy/relay）给客户端，实现下载。
 * <p>
 * 设计意图：
 * - 避免前端直接跨域请求 B 站接口（CORS / 防盗链限制）。
 * - 隐藏 B 站音频真实 URL（防盗链 Referer 检查）。
 * - 统一文件命名和 Content-Disposition 头，方便用户下载。
 */
@RestController
@RequestMapping("/api/bilibili")
public class BilibiliController {

    /** SLF4J 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(BilibiliController.class);

    /** Bilibili 服务层 —— 封装 BV 号提取、音频信息获取等业务逻辑 */
    private final BilibiliService bilibiliService;

    /**
     * 构造器注入 BilibiliService。
     *
     * @param bilibiliService B 站业务服务
     */
    public BilibiliController(BilibiliService bilibiliService) {
        this.bilibiliService = bilibiliService;
    }

    /**
     * 下载 B 站视频的音频（透传代理模式）
     * <p>
     * URL: POST /api/bilibili/download
     * <p>
     * 请求体 JSON：{"url": "https://www.bilibili.com/video/BV1xx411c7mD"}
     * <p>
     * 响应：直接返回音频文件流（不是 JSON 格式），设置为附件下载。
     * <p>
     * 权限：取决于 SecurityConfig 配置（可能是公开或需认证）。
     * <p>
     * 执行流程：
     * 1. 从请求体中提取 url 参数，若为空则返回 400 错误。
     * 2. 调用 bilibiliService.extractBvid(url) 从 URL 中提取 BV 号。
     *    - BV 号是 B 站的视频唯一标识符，形如 BV1xx411c7mD。
     *    - 提取失败（非 B 站链接）将抛出异常。
     * 3. 调用 bilibiliService.getAudioInfo(bvid) 获取音频信息。
     *    - 此方法内部可能调用外部 Python 脚本抓取 B 站接口。
     *    - 返回 AudioInfo 对象，包含 title（视频标题）和 audioUrl（音频流地址）。
     *    - audioUrl 通常是 B 站 CDN 的 m4a 音频流地址，带有防盗链检查。
     * 4. 建立与 B 站 CDN 的 HTTP 连接：
     *    - 设置 Referer: https://www.bilibili.com（B 站防盗链要求）。
     *    - 设置 User-Agent 模拟浏览器。
     * 5. 检查 B 站 CDN 返回状态码，非 200 时返回 502 Bad Gateway。
     * 6. 设置响应头：
     *    - Content-Type：透传 B 站返回的 Content-Type（通常为 audio/mp4）。
     *    - Content-Disposition：attachment; filename*=UTF-8''{文件名}.m4a
     *      （RFC 5987 编码，支持中文文件名）。
     *    - Content-Length：透传文件大小。
     * 7. 使用 8KB 缓冲区循环读写，将 B 站的音频流透传给客户端。
     * 8. 完成后释放连接，记录日志。
     * <p>
     * 异常处理：
     * - 所有异常（网络超时、B 站接口错误、Python 脚本异常等）统一捕获，
     *   返回 500 内部服务器错误并记录完整堆栈。
     * <p>
     * 注意：
     * - 此方法返回 void 而非 Result，因为直接向 HttpServletResponse 写入二进制流，
     *   而不是返回 JSON 响应体。
     * - 错误时调用 writeError() 辅助方法手动写入 JSON 格式的错误信息。
     *
     * @param body     请求体（包含 url 字段）
     * @param response HTTP 响应对象，用于直接输出文件流
     */
    @PostMapping("/download")
    public void download(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String url = body.get("url");
        log.info("收到 B 站下载请求, url=[{}]", url);

        // 参数校验 —— URL 不能为空
        if (url == null || url.isBlank()) {
            writeError(response, 400, "请提供 B 站视频链接");
            return;
        }

        try {
            // 步骤 1：从 B 站 URL 中提取 BV 号
            log.debug("提取 BV 号");
            String bvid = bilibiliService.extractBvid(url);
            log.info("BV 号提取成功, bvid=[{}]", bvid);

            // 步骤 2：通过 Python 脚本或其他方式获取音频流信息
            log.debug("调用 Python 获取音频 URL");
            BilibiliService.AudioInfo audioInfo = bilibiliService.getAudioInfo(bvid);
            String title = audioInfo.getTitle();
            String audioUrl = audioInfo.getAudioUrl();

            log.info("开始透传音频流, title=[{}]", title);

            // 步骤 3：建立到 B 站 CDN 的 HTTP 连接（带防盗链 Header）
            HttpURLConnection conn = (HttpURLConnection) new URL(audioUrl).openConnection();
            // B 站防盗链：必须设置正确的 Referer
            conn.setRequestProperty("Referer", "https://www.bilibili.com");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.connect();

            // 步骤 4：检查 B 站 CDN 是否正常返回
            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                log.error("B 站音频源返回非 200, statusCode={}", statusCode);
                writeError(response, 502, "B 站音频源返回 " + statusCode);
                return;
            }

            // 步骤 5：设置响应 Content-Type（默认 audio/mp4）
            String contentType = conn.getContentType();
            if (contentType == null) contentType = "audio/mp4";

            // 步骤 6：对文件名进行 URL 编码（支持中文、空格等特殊字符）
            // 使用 RFC 5987 标准的 filename* 格式
            String encodedFilename = URLEncoder.encode(title + ".m4a", StandardCharsets.UTF_8)
                    .replace("+", "%20");

            // 步骤 7：设置下载响应头
            response.setContentType(contentType);
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFilename);

            // 透传文件大小（如果有的话）
            long contentLength = conn.getContentLengthLong();
            if (contentLength > 0) {
                response.setContentLengthLong(contentLength);
            }

            // 步骤 8：流式透传 —— 从 B 站 CDN 读取，写入客户端响应
            try (InputStream is = conn.getInputStream();
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];  // 8KB 缓冲区
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.info("音频透传完成, title=[{}]", title);

        } catch (Exception e) {
            // 统一异常处理：网络异常、B 站接口异常、Python 脚本异常等
            log.error("B 站下载失败: {}", e.getMessage(), e);
            writeError(response, 500, "下载失败: " + e.getMessage());
        }
    }

    /**
     * 向客户端写入 JSON 格式的错误响应。
     * <p>
     * 由于 download() 方法返回 void（直接操作 HttpServletResponse），
     * 在发生错误时不能使用 Result.success()/Result.fail() 这种常规方式，
     * 需要手动构造 JSON 字符串写入响应流。
     * <p>
     * JSON 格式：{"code": {status}, "message": "{message}", "data": null}
     * <p>
     * 此格式与 Result 类的 JSON 结构保持一致，方便前端统一处理。
     *
     * @param response HTTP 响应对象
     * @param status   HTTP 状态码（如 400、500、502 等）
     * @param message  错误描述信息
     */
    private void writeError(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            // 手动构建与 Result 结构一致的 JSON 字符串
            // 注意对消息中的双引号进行转义，防止 JSON 语法错误
            String json = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                    status, message.replace("\"", "\\\""));
            response.getWriter().write(json);
        } catch (Exception ignored) {
            // 写入错误响应时若再次发生异常（如流已关闭），直接忽略
            // 这是最后的手段，无法再向前端报告错误
        }
    }
}
