package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.Listen1Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/external")
public class Listen1Controller {

    private static final Logger log = LoggerFactory.getLogger(Listen1Controller.class);
    private final Listen1Service listen1Service;

    public Listen1Controller(Listen1Service listen1Service) {
        this.listen1Service = listen1Service;
    }

    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam String source,
            @RequestParam String keywords,
            @RequestParam(defaultValue = "1") int page) {
        log.info("search: source={}, keywords={}, page={}", source, keywords, page);
        try {
            List<Map<String, Object>> data = listen1Service.search(source, keywords, page);
            return Result.success(data);
        } catch (Exception e) {
            log.error("search failed: {}", e.getMessage());
            return Result.fail("搜索失败: " + e.getMessage());
        }
    }

    @GetMapping("/playlist")
    public Result<List<Map<String, Object>>> showPlaylist(
            @RequestParam String source,
            @RequestParam(defaultValue = "0") int offset) {
        log.info("showPlaylist: source={}, offset={}", source, offset);
        try {
            List<Map<String, Object>> data = listen1Service.showPlaylist(source, offset);
            return Result.success(data);
        } catch (Exception e) {
            log.error("showPlaylist failed: {}", e.getMessage());
            return Result.fail("获取歌单失败: " + e.getMessage());
        }
    }

    @GetMapping("/playlist/{listId}")
    public Result<Map<String, Object>> playlistDetail(@PathVariable String listId) {
        log.info("playlistDetail: listId={}", listId);
        try {
            Map<String, Object> data = listen1Service.playlistDetail(listId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("playlistDetail failed: {}", e.getMessage());
            return Result.fail("获取歌单详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/lyric")
    public Result<String> lyric(@RequestParam String trackId) {
        log.info("lyric: trackId={}", trackId);
        try {
            String lyric = listen1Service.getLyric(trackId);
            return Result.success(lyric);
        } catch (Exception e) {
            log.error("lyric failed: {}", e.getMessage());
            return Result.fail("获取歌词失败: " + e.getMessage());
        }
    }

    @GetMapping("/bootstrap")
    public Result<String> bootstrap(@RequestParam String trackId) {
        log.info("bootstrap: trackId={}", trackId);
        try {
            String url = listen1Service.bootstrapTrack(trackId);
            if (url == null) {
                return Result.fail("无法获取播放地址");
            }
            return Result.success(url);
        } catch (Exception e) {
            log.error("bootstrap failed: {}", e.getMessage());
            return Result.fail("获取播放地址失败: " + e.getMessage());
        }
    }
}
