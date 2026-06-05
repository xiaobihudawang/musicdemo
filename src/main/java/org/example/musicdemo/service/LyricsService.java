package org.example.musicdemo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 歌词服务，通过网易云 cloudsearch API 搜索歌曲，再用 Listen1 获取 LRC 格式歌词。
 */
@Service
public class LyricsService {

    private static final Logger log = LoggerFactory.getLogger(LyricsService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String NETEASE_SEARCH_PATH = "sidecar" + File.separator + "netease-search.js";

    private final Listen1Service listen1Service;
    private final MusicMapper musicMapper;

    public LyricsService(Listen1Service listen1Service, MusicMapper musicMapper) {
        this.listen1Service = listen1Service;
        this.musicMapper = musicMapper;
    }

    /**
     * 异步获取歌词并更新数据库，不阻塞上传请求。
     */
    @Async
    public void fetchLyricsAndUpdate(Integer musicId, String title, String artist) {
        try {
            String lyrics = fetchLyrics(title, artist);
            if (lyrics != null && !lyrics.isBlank()) {
                musicMapper.updateLyrics(musicId, lyrics);
                log.info("async lyrics fetched for music {}: {} chars", musicId, lyrics.length());
            } else {
                log.info("no lyrics found for music {}: {} - {}", musicId, title, artist);
            }
        } catch (Exception e) {
            log.warn("async fetch lyrics failed for {} - {}: {}", title, artist, e.getMessage());
        }
    }

    /**
     * 搜索并获取歌词。
     * 流程：netease-search.js 搜索 → 获取 trackId → listen1.getLyric() 获取歌词
     */
    public String fetchLyrics(String title, String artist) {
        String keywords = title + " " + artist;

        // 使用网易云搜索获取 trackId
        String trackId = searchNeteaseTrackId(keywords, title);
        if (trackId == null) {
            log.warn("no trackId found for: {} - {}", title, artist);
            return null;
        }

        log.debug("found trackId: {} for {} - {}", trackId, title, artist);

        // 通过 Listen1 获取歌词
        try {
            String lyric = listen1Service.getLyric(trackId);
            if (lyric != null && !lyric.isBlank() && lyric.contains("[")) {
                log.info("lyrics found for: {} - {}, {} chars", title, artist, lyric.length());
                return lyric;
            }
        } catch (Exception e) {
            log.debug("getLyric failed for trackId {}: {}", trackId, e.getMessage());
        }

        return null;
    }

    /**
     * 使用 netease-search.js 搜索网易云音乐获取 trackId。
     * 返回的 ID 格式: netrack_xxxxx
     */
    @SuppressWarnings("unchecked")
    private String searchNeteaseTrackId(String keywords, String expectedTitle) {
        try {
            String scriptPath = new File(System.getProperty("user.dir"), NETEASE_SEARCH_PATH).getAbsolutePath();
            ProcessBuilder pb = new ProcessBuilder("node", scriptPath, keywords, "1");
            pb.redirectErrorStream(true);
            String nodePath = new File(System.getProperty("user.dir"), "sidecar/node_modules").getAbsolutePath();
            pb.environment().put("NODE_PATH", nodePath);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (InputStream is = process.getInputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) {
                    output.append(new String(buf, 0, len, StandardCharsets.UTF_8));
                }
            }
            int exitCode = process.waitFor();
            String json = output.toString().trim();

            if (exitCode != 0 || json.isEmpty()) {
                log.debug("netease-search failed: exit={}, out={}", exitCode, json.substring(0, Math.min(200, json.length())));
                return null;
            }

            Map<String, Object> resp = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            int code = ((Number) resp.getOrDefault("code", 500)).intValue();
            if (code != 200) {
                log.debug("netease-search returned code={}: {}", code, json.substring(0, Math.min(200, json.length())));
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            if (data == null) return null;
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
            if (result == null || result.isEmpty()) return null;

            // 优先匹配歌名
            for (Map<String, Object> item : result) {
                String songTitle = (String) item.get("title");
                if (songTitle != null && songTitle.equalsIgnoreCase(expectedTitle)) {
                    return (String) item.get("id");
                }
            }
            // 找不到精确匹配就用第一个
            return (String) result.get(0).get("id");
        } catch (Exception e) {
            log.debug("searchNeteaseTrackId failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 手动重新获取歌词。
     */
    public String regenerateLyrics(Integer musicId, String title, String artist) {
        String lyrics = fetchLyrics(title, artist);
        if (lyrics != null && !lyrics.isBlank()) {
            musicMapper.updateLyrics(musicId, lyrics);
            return lyrics;
        }
        return null;
    }
}
