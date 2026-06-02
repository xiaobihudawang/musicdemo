package org.example.musicdemo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CoverService {

    private static final Logger log = LoggerFactory.getLogger(CoverService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String WORKER_PATH = "sidecar" + File.separator + "listen1-worker.js";
    private static final String NETEASE_SEARCH_PATH = "sidecar" + File.separator + "netease-search.js";
    private static final String COVERS_DIR = "covers/";

    private final MusicMapper musicMapper;

    @Value("${music.file-path}")
    private String filePath;

    public CoverService(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    public String fetchCover(String title, String artist) {
        // 1. Try NetEase cloudsearch (works without auth)
        try {
            String imgUrl = searchNetease(title, artist);
            if (imgUrl != null) {
                log.info("cover found via netease: {} - {}", title, artist);
                return downloadCover(imgUrl);
            }
        } catch (Exception e) {
            log.debug("netease cover search failed: {}", e.getMessage());
        }

        // 2. Fallback: try other sources via listen1-worker
        String[] sources = {"kugou", "kuwo", "qq", "migu", "baidu", "xiami"};
        for (String source : sources) {
            try {
                String imgUrl = searchCover(source, title, artist);
                if (imgUrl != null) {
                    log.info("cover found via {}: {} - {}", source, title, artist);
                    return downloadCover(imgUrl);
                }
            } catch (Exception e) {
                log.debug("cover search failed for {}: {}", source, e.getMessage());
            }
        }

        log.warn("no cover found for: {} - {}", title, artist);
        return null;
    }

    /** 异步获取封面图并更新数据库，不阻塞上传请求 */
    @Async
    public void fetchCoverAndUpdate(Integer musicId, String title, String artist) {
        try {
            String coverPath = fetchCover(title, artist);
            if (coverPath != null) {
                musicMapper.updateCoverPath(musicId, coverPath);
                log.info("async cover fetched for music {}: {}", musicId, coverPath);
            }
        } catch (Exception e) {
            log.warn("async fetch cover failed for {} - {}: {}", title, artist, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String searchNetease(String title, String artist) throws Exception {
        String scriptPath = new File(System.getProperty("user.dir"), NETEASE_SEARCH_PATH).getAbsolutePath();
        String keywords = title + " " + artist;
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
        process.waitFor();

        return extractImgUrl(output.toString().trim(), title);
    }

    @SuppressWarnings("unchecked")
    private String searchCover(String source, String title, String artist) throws Exception {
        String keywords = URLEncoder.encode(title + " " + artist, StandardCharsets.UTF_8);
        String params = "source=" + source + "&keywords=" + keywords + "&curpage=1";

        String scriptPath = new File(System.getProperty("user.dir"), WORKER_PATH).getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder("node", scriptPath, "search", params);
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
        process.waitFor();

        return extractImgUrl(output.toString().trim(), title);
    }

    @SuppressWarnings("unchecked")
    private String extractImgUrl(String json, String expectedTitle) {
        if (json == null || json.isEmpty()) return null;
        try {
            Map<String, Object> resp = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            int code = ((Number) resp.getOrDefault("code", 500)).intValue();
            if (code != 200) return null;

            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
            if (result == null || result.isEmpty()) return null;

            // 优先匹配歌名，避免同歌手不同歌取到同一张封面
            String imgUrl = null;
            for (Map<String, Object> item : result) {
                String songTitle = (String) item.get("title");
                if (songTitle != null && songTitle.equals(expectedTitle)) {
                    imgUrl = (String) item.get("img_url");
                    break;
                }
            }
            if (imgUrl == null) {
                imgUrl = (String) result.get(0).get("img_url");
            }
            if (imgUrl == null || imgUrl.isBlank()) return null;

            if (imgUrl.startsWith("http://")) {
                imgUrl = "https://" + imgUrl.substring(7);
            }
            return imgUrl;
        } catch (Exception e) {
            log.debug("parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String downloadCover(String imgUrl) throws Exception {
        File dir = new File(filePath + COVERS_DIR);
        if (!dir.exists()) dir.mkdirs();

        String ext = ".jpg";
        int idx = imgUrl.lastIndexOf('.');
        if (idx > 0) {
            String candidate = imgUrl.substring(idx).replaceAll("[?].*", "").toLowerCase();
            if (candidate.matches("\\.(jpg|jpeg|png|webp)")) ext = candidate;
        }

        String filename = UUID.randomUUID() + ext;
        File dest = new File(dir, filename);

        URL url = new URL(imgUrl);
        try (InputStream is = url.openStream();
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }

        log.info("cover downloaded: {}", filename);
        // 始终用 / 分隔（URL 路径），不用 File.separator
        return COVERS_DIR + filename;
    }
}
