package org.example.musicdemo.service;

import org.example.musicdemo.mapper.MusicMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 歌词服务，调用 Python 脚本获取 LRC 歌词。
 */
@Service
public class LyricsService {

    private static final Logger log = LoggerFactory.getLogger(LyricsService.class);
    private static final String SCRIPT = "netease_search.py";

    private final MusicMapper musicMapper;

    public LyricsService(MusicMapper musicMapper) {
        this.musicMapper = musicMapper;
    }

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

    public String fetchLyrics(String title, String artist) {
        try {
            // Step 1: get trackId
            String trackId = searchTrackId(title, artist);
            if (trackId == null) return null;
            log.debug("found trackId: {} for {} - {}", trackId, title, artist);

            // Step 2: get lyric
            String scriptPath = new File(System.getProperty("user.dir"), SCRIPT).getAbsolutePath();
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add(scriptPath);
            command.add("lyric");
            command.add(trackId);

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

            String lrc = output.toString().trim();
            if (!lrc.isEmpty() && lrc.contains("[")) {
                log.info("lyrics found for: {} - {}, {} chars", title, artist, lrc.length());
                return lrc;
            }
        } catch (Exception e) {
            log.debug("fetchLyrics failed for {} - {}: {}", title, artist, e.getMessage());
        }
        return null;
    }

    private String searchTrackId(String title, String artist) {
        try {
            String scriptPath = new File(System.getProperty("user.dir"), SCRIPT).getAbsolutePath();
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add(scriptPath);
            command.add("trackid");
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
            if (result.startsWith("TRACKID:")) {
                return result.substring(8);
            }
        } catch (Exception e) {
            log.debug("searchTrackId failed: {}", e.getMessage());
        }
        return null;
    }

    public String regenerateLyrics(Integer musicId, String title, String artist) {
        String lyrics = fetchLyrics(title, artist);
        if (lyrics != null && !lyrics.isBlank()) {
            musicMapper.updateLyrics(musicId, lyrics);
            return lyrics;
        }
        return null;
    }
}
