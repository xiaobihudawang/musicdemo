package org.example.musicdemo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class Listen1Service {

    private static final Logger log = LoggerFactory.getLogger(Listen1Service.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String WORKER_PATH = "sidecar" + File.separator + "listen1-worker.js";

    private Map<String, Object> exec(String apiName, String params) {
        String scriptPath = new File(System.getProperty("user.dir"), WORKER_PATH).getAbsolutePath();
        List<String> command = new ArrayList<>();
        command.add("node");
        command.add(scriptPath);
        command.add(apiName);
        if (params != null && !params.isEmpty()) {
            command.add(params);
        }

        log.debug("exec: command={}", command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        // Ensure NODE_PATH points to sidecar node_modules
        String nodePath = new File(System.getProperty("user.dir"), "sidecar/node_modules").getAbsolutePath();
        pb.environment().put("NODE_PATH", nodePath);

        try {
            Process process = pb.start();

            // Read all stdout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            String json = output.toString();

            log.debug("exec: exitCode={}, output={}", exitCode, json.length() > 200 ? json.substring(0, 200) + "..." : json);

            if (exitCode != 0 || json.isBlank()) {
                throw new RuntimeException("listen1-worker exited with code " + exitCode + ": " + json);
            }

            Map<String, Object> result = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            int code = ((Number) result.getOrDefault("code", 500)).intValue();
            if (code != 200) {
                String msg = (String) result.getOrDefault("message", "unknown error");
                throw new RuntimeException("listen1 worker error: " + msg);
            }

            return result;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("listen1 worker failed: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> search(String source, String keywords, int page) {
        String params = "source=" + source + "&keywords=" + java.net.URLEncoder.encode(keywords, StandardCharsets.UTF_8)
                + "&curpage=" + page;
        Map<String, Object> resp = exec("search", params);
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
        return result != null ? result : List.of();
    }

    public List<Map<String, Object>> showPlaylist(String source, int offset) {
        String params = "source=" + source + "&offset=" + offset;
        Map<String, Object> resp = exec("show_playlist", params);
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
        return result != null ? result : List.of();
    }

    public Map<String, Object> playlistDetail(String listId) {
        String params = "list_id=" + listId;
        Map<String, Object> resp = exec("playlist", params);
        return (Map<String, Object>) resp.get("data");
    }

    public String getLyric(String trackId) {
        String params = "track_id=" + trackId;
        Map<String, Object> resp = exec("lyric", params);
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        return (String) data.get("lyric");
    }

    public String bootstrapTrack(String trackId) {
        String params = "track_id=" + trackId;
        try {
            Map<String, Object> resp = exec("bootstrap_track", params);
            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            return (String) data.get("url");
        } catch (Exception e) {
            log.warn("bootstrapTrack failed for {}: {}", trackId, e.getMessage());
            return null;
        }
    }
}
