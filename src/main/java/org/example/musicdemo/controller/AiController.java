package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.AiService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/description")
    public Result<?> generateDescription(@RequestBody Map<String, String> req) {
        String title = req.get("title");
        String artist = req.get("artist");
        if (title == null || title.isBlank()) {
            return Result.fail("歌曲标题不能为空");
        }
        String description = aiService.generateDescription(title, artist);
        return Result.success(Map.of("description", description));
    }

    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, Object> req) {
        Integer userId = getCurrentUserId();
        String message = (String) req.get("message");
        Integer contextMusicId = (Integer) req.get("contextMusicId");

        if (message == null || message.isBlank()) {
            return Result.fail("消息不能为空");
        }

        Map<String, Object> result = aiService.chat(userId, message, contextMusicId);
        return Result.success(result);
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }
}
