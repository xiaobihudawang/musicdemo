package org.example.musicdemo.controller;

import org.example.musicdemo.common.Result;
import org.example.musicdemo.service.AiService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 控制器 —— 提供 AI 驱动的描述生成和智能对话功能。
 * <p>
 * 所有端点以 /api/ai 开头。
 * 安全说明：
 * - /api/ai/description 在 SecurityConfig 中可能配置为公开或需认证，需结合实际情况确认。
 * - /api/ai/chat 需要用户登录（需要获取当前用户 ID）。
 * <p>
 * 功能概览：
 * - POST /api/ai/description → 根据歌曲标题和歌手生成 AI 描述文本。
 * - POST /api/ai/chat       → AI 对话，携带上下文音乐 ID 进行智能推荐问答。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    /** AI 服务层 —— 封装与 AI 模型交互的业务逻辑 */
    private final AiService aiService;

    /**
     * 构造器注入 AiService。
     * 无 @Autowired，利用 Spring 4.3+ 的隐式自动注入特性，
     * 当类只有一个构造器时 Spring 自动注入参数。
     */
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * AI 生成歌曲描述（音乐详情页的智能摘要）
     * <p>
     * URL: POST /api/ai/description
     * <p>
     * 请求体 JSON：{"title": "歌曲名", "artist": "歌手名"}
     * <p>
     * 权限：公开（GET）/ 需认证（POST）—— 取决于 SecurityConfig 配置。
     * <p>
     * 业务说明：
     * - 从请求体中提取 title 和 artist 参数。
     * - title 为必填，若为空或空白字符串则直接返回 400 级别错误消息。
     * - artist 为可选（可以为 null）。
     * - 调用 aiService.generateDescription(title, artist) 触发 AI 生成。
     *   - 底层可能调用 OpenAI / DeepSeek 等大模型 API。
     *   - 也可能使用本地规则模板或关键词匹配策略。
     * - 将生成的描述文本包装在 Map 中返回。
     * <p>
     * 返回值示例：{"code":200, "message":"success", "data":{"description":"一首优美的..."}}
     */
    @PostMapping("/description")
    public Result<?> generateDescription(@RequestBody Map<String, String> req) {
        String title = req.get("title");
        String artist = req.get("artist");
        // title 为必填校验 —— 空值或纯空白字符串均拒绝
        if (title == null || title.isBlank()) {
            return Result.fail("歌曲标题不能为空");
        }
        String description = aiService.generateDescription(title, artist);
        return Result.success(Map.of("description", description));
    }

    /**
     * AI 智能对话（音乐助手问答）
     * <p>
     * URL: POST /api/ai/chat
     * <p>
     * 请求体 JSON：
     * {"message": "用户消息文本", "contextMusicId": 1}
     * <p>
     * 权限：需要用户登录（从 JWT 中提取 userId）。
     * <p>
     * 业务说明：
     * - 从 SecurityContextHolder 中获取当前认证用户 ID（getCurrentUserId()）。
     * - 提取 message（用户输入的消息）和 contextMusicId（上下文音乐 ID，可 null）。
     * - message 不能为空，否则直接返回错误。
     * - contextMusicId 用于给 AI 提供当前正在查看的音乐作为上下文，
     *   使 AI 的推荐或回答更有针对性（例如："这首歌的风格是..."）。
     * - 调用 aiService.chat(userId, message, contextMusicId) 执行对话逻辑。
     *   - 可能维护每个用户的对话历史（基于 userId 做会话管理）。
     *   - 返回的内容可能包括 AI 回复文本、推荐歌曲列表等。
     * <p>
     * 返回值：Result.success(result) —— result 是一个 Map，包含 AI 回复数据。
     */
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, Object> req) {
        // 获取当前登录用户 ID —— 用于会话跟踪和历史管理
        Integer userId = getCurrentUserId();
        String message = (String) req.get("message");
        Integer contextMusicId = (Integer) req.get("contextMusicId");

        // 输入校验 —— 消息不能为空
        if (message == null || message.isBlank()) {
            return Result.fail("消息不能为空");
        }

        Map<String, Object> result = aiService.chat(userId, message, contextMusicId);
        return Result.success(result);
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户的 ID。
     * <p>
     * 实现原理：
     * - SecurityContextHolder.getContext().getAuthentication() 获取当前线程绑定的认证信息。
     * - 在 JWT 认证过滤器（JwtAuthenticationFilter）中，解析 Token 后会将 userId
     *   封装为 UsernamePasswordAuthenticationToken 的 principal。
     * - 因此 auth.getPrincipal() 返回的就是 Integer 类型的 userId。
     * <p>
     * 注意：
     * - 此方法必须在已认证的请求中调用，否则 getAuthentication() 返回 null，
     *   导致 NullPointerException（但 /chat 端点已确保有认证，不会出现此问题）。
     * - 若需要在未认证时可选的逻辑中获取 userId，应使用 try-catch 或判断是否认证。
     *
     * @return 当前登录用户的 ID（Integer）
     */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Integer) auth.getPrincipal();
    }
}
