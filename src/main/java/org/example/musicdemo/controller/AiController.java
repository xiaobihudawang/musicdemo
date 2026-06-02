package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.AiService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 控制器，提供 AI 描述生成和智能对话功能。
 * 所有端点以 /api/ai 开头。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * AI 生成歌曲描述
     */
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

}