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
 * 哔哩哔哩服务 —— 调用 Python 脚本解析 B 站视频标题和音频下载地址。
 */
@Service
public class BilibiliService {

    private static final Logger log = LoggerFactory.getLogger(BilibiliService.class);

    /** 音频信息值对象 */
    public static class AudioInfo {
        private final String title;
        private final String audioUrl;
        private final String backupUrl;

        public AudioInfo(String title, String audioUrl, String backupUrl) {
            this.title = title;
            this.audioUrl = audioUrl;
            this.backupUrl = backupUrl;
        }

        public String getTitle() { return title; }
        public String getAudioUrl() { return audioUrl; }
        public String getBackupUrl() { return backupUrl; }
    }

    /** 从 B 站视频 URL 中提取 BV 号 */
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
     * 根据 BV 号调用 Python 脚本获取音频信息。
     * 脚本位于项目根目录 {user.dir}/bilibili_demo.py。
     */
    public AudioInfo getAudioInfo(String bvid) throws Exception {
        log.info("getAudioInfo 开始: bvid=[{}]", bvid);

        String scriptPath = new File(System.getProperty("user.dir"), "bilibili_demo.py").getAbsolutePath();
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(scriptPath);
        command.add(bvid);

        log.debug("getAudioInfo 命令拼装: command={}", command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        log.info("getAudioInfo: Python 进程已启动, pid={}", process.pid());

        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
                log.debug("getAudioInfo 输出: [{}]", line);
            }
        }

        int exitCode = process.waitFor();
        log.info("getAudioInfo: 进程退出, exitCode={}", exitCode);

        if (exitCode != 0) {
            log.error("Python 脚本异常退出, exitCode={}, 输出={}", exitCode, String.join("\n", outputLines));
            throw new RuntimeException("Python 脚本执行失败，退出码: " + exitCode);
        }

        String title = null;
        String audioUrl = null;
        String backupUrl = null;

        for (String line : outputLines) {
            if (line.startsWith("TITLE:")) {
                title = line.substring(6).trim();
            } else if (line.startsWith("URL:")) {
                audioUrl = line.substring(4).trim();
            } else if (line.startsWith("BACKUP_URL:")) {
                backupUrl = line.substring(11).trim();
            }
        }

        if (title == null || audioUrl == null) {
            log.error("Python 输出格式异常: 缺少必要字段, 输出={}", String.join("\n", outputLines));
            throw new RuntimeException("解析 Python 输出失败，缺少标题或音频 URL");
        }

        log.info("getAudioInfo 成功: title=[{}], audioUrl=[{}...]", title, audioUrl.substring(0, Math.min(80, audioUrl.length())));
        return new AudioInfo(title, audioUrl, backupUrl);
    }
}
