package org.example.musicdemo.service;

import org.example.musicdemo.mapper.MusicMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CoverService {

    private static final Logger log = LoggerFactory.getLogger(CoverService.class);
    private static final String COVERS_DIR = "covers/";
    private static final String SCRIPT = "scripts/netease_search.py";

    private final MusicMapper musicMapper;

    @Value("${music.file-path}")
    private String filePath;

    public CoverService(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

    public String fetchCover(String title, String artist) {
        try {
            String scriptPath = new File(System.getProperty("user.dir"), SCRIPT).getAbsolutePath();
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add(scriptPath);
            command.add("cover");
            command.add(title);
            command.add(artist);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            process.waitFor();

            String result = output.toString().trim();
            if (result.startsWith("COVER:")) {
                String imgUrl = result.substring(6);
                log.info("cover found via python: {} - {}", title, artist);
                return downloadCover(imgUrl);
            }
        } catch (Exception e) {
            log.debug("python cover search failed: {}", e.getMessage());
        }

        log.warn("no cover found for: {} - {}", title, artist);
        return null;
    }

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
        return COVERS_DIR + filename;
    }
}
