package org.example.musicdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 哔哩哔哩服务 —— 调用外部 Python 脚本解析 B 站视频信息。
 * <p>
 * 由于 Java 生态缺少成熟的 B 站 API 库，此服务通过 {@link ProcessBuilder}
 * 启动一个本地 Python 进程来执行 {@code bilibili_demo.py} 脚本，
 * 然后从标准输出中解析视频标题和音频下载地址（包括备用地址）。
 * </p>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>从用户输入的 URL 中正则提取 BV 号</li>
 *   <li>拼装命令 {@code python bilibili_demo.py {bvid}}</li>
 *   <li>启动子进程，逐行读取 stdout</li>
 *   <li>按 {@code TITLE:}、{@code URL:}、{@code BACKUP_URL:} 前缀解析输出</li>
 *   <li>返回 {@link AudioInfo} 对象</li>
 * </ol>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>依赖 Python 环境和 {@code bilibili_demo.py} 脚本存在于项目根目录</li>
 *   <li>进程退出码非 0 时抛出 {@link RuntimeException}</li>
 *   <li>编码强制设置为 UTF-8（通过环境变量 {@code PYTHONIOENCODING=utf-8}）</li>
 * </ul>
 */
@Service
public class BilibiliService {

    /** SLF4J 日志记录器，所有关键步骤均有 debug/info/warn/error 级别日志 */
    private static final Logger log = LoggerFactory.getLogger(BilibiliService.class);

    /**
     * 音频信息的值对象（Value Object / DTO）。
     * 由 Python 脚本解析结果封装而来，包含标题、音频直链和备用链接三个不可变字段。
     */
    public static class AudioInfo {
        /** 视频标题（同时也是音频标题） */
        private final String title;
        /** 音频文件的直接下载 URL（主用） */
        private final String audioUrl;
        /** 备用音频下载 URL（当主 URL 失效时使用） */
        private final String backupUrl;

        /**
         * 全参构造器。
         * @param title     视频标题
         * @param audioUrl  音频 URL
         * @param backupUrl 备用 URL（可为 null）
         */
        public AudioInfo(String title, String audioUrl, String backupUrl) {
            this.title = title;
            this.audioUrl = audioUrl;
            this.backupUrl = backupUrl;
        }

        public String getTitle() { return title; }
        public String getAudioUrl() { return audioUrl; }
        public String getBackupUrl() { return backupUrl; }
    }

    /**
     * 从 B 站视频 URL 中提取 BV 号。
     * <p>
     * B 站 BV 号格式为 {@code BV} 开头 + 10~12 位字母数字组合。
     * 例如 {@code https://www.bilibili.com/video/BV1GJ411x7p7} 中的 {@code BV1GJ411x7p7}。
     * </p>
     *
     * @param url 用户输入的完整 B 站视频 URL
     * @return 提取出的 BV 号字符串
     * @throws RuntimeException 如果 URL 中不包含合法的 BV 号
     */
    public String extractBvid(String url) {
        log.debug("extractBvid 开始: url=[{}]", url);
        Pattern pattern = Pattern.compile("BV[a-zA-Z0-9]{10,12}");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String bvid = matcher.group();
            log.info("extractBvid 成功: bvid=[{}]", bvid);
            return bvid;
        }
        log.warn("extractBvid 失败: URL 格式无法匹配 BV 号, url=[{}]", url);
        throw new RuntimeException("无法从 URL 中提取 BV 号: " + url);
    }

    /**
     * 根据 BV 号调用 Python 脚本获取音频信息（标题 + 下载地址）。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>定位脚本路径为 {@code {user.dir}/bilibili_demo.py}</li>
     *   <li>启动 {@code python bilibili_demo.py {bvid}} 子进程</li>
     *   <li>设置环境变量 {@code PYTHONIOENCODING=utf-8} 确保中文不乱码</li>
     *   <li>合并 stdout 和 stderr（{@link ProcessBuilder#redirectErrorStream(boolean)}）</li>
     *   <li>逐行读取输出，存到 {@link List} 中</li>
     *   <li>等待进程结束，检查退出码</li>
     *   <li>解析输出行，提取 TITLE / URL / BACKUP_URL 字段</li>
     * </ol>
     *
     * <h3>错误处理</h3>
     * <ul>
     *   <li>退出码非 0 → 抛出 RuntimeException，附带完整输出便于排查</li>
     *   <li>缺少 title 或 audioUrl → 抛出 RuntimeException</li>
     *   <li>{@code backupUrl} 是可选字段，可为 null</li>
     * </ul>
     *
     * <h3>线程安全</h3>
     * 每次调用都创建全新的 {@link Process}，实例之间互不干扰，本服务是线程安全的。
     *
     * @param bvid B 站视频 BV 号
     * @return 解析后的 AudioInfo 对象（包含标题、音频 URL、备用 URL）
     * @throws RuntimeException Python 脚本执行失败或输出格式异常时抛出
     */
    public AudioInfo getAudioInfo(String bvid) throws Exception {
        log.info("getAudioInfo 开始: bvid=[{}]", bvid);

        // 脚本位于项目根目录（和 pom.xml 同级）
        String scriptPath = new File(System.getProperty("user.dir"), "bilibili_demo.py").getAbsolutePath();
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(scriptPath);
        command.add(bvid);

        log.debug("getAudioInfo 命令拼装: command={}", command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // 强制 UTF-8 编码，防止 Python 打印中文时在 Windows 下乱码
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        // 将 stderr 合并到 stdout，简化读取逻辑
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        log.info("getAudioInfo: Python 进程已启动, pid={}", process.pid());

        // 逐行读取进程的标准输出（含错误输出）
        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
                log.debug("getAudioInfo 输出: [{}]", line);
            }
        } // try-with-resources 自动关闭 BufferedReader

        // 阻塞等待进程执行完毕
        int exitCode = process.waitFor();
        log.info("getAudioInfo: 进程退出, exitCode={}", exitCode);

        // 非 0 退出码表示脚本执行出错
        if (exitCode != 0) {
            log.error("Python 脚本异常退出, exitCode={}, 输出={}", exitCode, String.join("\n", outputLines));
            throw new RuntimeException("Python 脚本执行失败，退出码: " + exitCode);
        }

        // 解析 Python 脚本约定好的输出格式
        String title = null;
        String audioUrl = null;
        String backupUrl = null;

        for (String line : outputLines) {
            if (line.startsWith("TITLE:")) {
                title = line.substring(6).trim();          // 去掉 "TITLE:" 前缀
            } else if (line.startsWith("URL:")) {
                audioUrl = line.substring(4).trim();       // 去掉 "URL:" 前缀
            } else if (line.startsWith("BACKUP_URL:")) {
                backupUrl = line.substring(11).trim();     // 去掉 "BACKUP_URL:" 前缀
            }
        }

        // title 和 audioUrl 是必填字段，backupUrl 可选
        if (title == null || audioUrl == null) {
            log.error("Python 输出格式异常: 缺少必要字段, 输出={}", String.join("\n", outputLines));
            throw new RuntimeException("解析 Python 输出失败，缺少标题或音频 URL");
        }

        // 日志中只截取 URL 前 80 个字符，避免敏感信息或超长内容
        log.info("getAudioInfo 成功: title=[{}], audioUrl=[{}...]", title, audioUrl.substring(0, Math.min(80, audioUrl.length())));
        return new AudioInfo(title, audioUrl, backupUrl);
    }
}
