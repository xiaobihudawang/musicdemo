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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SCRIPT = "netease_search.py";

    public Listen1Service() {
    }

    private String exec(String... args) {
        try {
            String scriptPath = new File(System.getProperty("user.dir"), SCRIPT).getAbsolutePath();
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add(scriptPath);
            for (String arg : args) {
                command.add(arg);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException("python script failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(String source, String keywords, int page) {
        try {
            String json = exec("search", keywords);
            if (json == null || json.isEmpty()) return List.of();
            List<Map<String, Object>> songs = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            return songs != null ? songs : List.of();
        } catch (Exception e) {
            log.warn("search failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> showPlaylist(String source, int offset) {
        return List.of();
    }

    public Map<String, Object> playlistDetail(String listId) {
        return Map.of();
    }

    public String getLyric(String trackId) {
        try {
            return exec("lyric", trackId);
        } catch (Exception e) {
            log.warn("getLyric failed for {}: {}", trackId, e.getMessage());
            return null;
        }
    }

    public String bootstrapTrack(String trackId) {
        return null;
    }
}
