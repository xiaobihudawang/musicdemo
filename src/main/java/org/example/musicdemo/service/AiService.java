package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 人工智能服务 —— 对接 DeepSeek 大模型 API，提供以下 AI 能力：
 * <ul>
 *   <li>自动生成歌曲简介（给上传的歌曲配一段文案）</li>
 *   <li>多轮对话聊天（每个用户独立维护会话历史）</li>
 *   <li>根据当前播放歌曲生成推荐语</li>
 * </ul>
 *
 * <p>所有请求统一走 {@link #callDeepSeek(Map)} 方法，
 * 异常时会返回用户友好的中文错误提示，不会抛出异常到上层。</p>
 */
@Service
public class AiService {

    /** DeepSeek API 密钥，从配置文件注入（ai.deepseek.api-key） */
    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    /** DeepSeek API 基础地址，默认 https://api.deepseek.com */
    @Value("${ai.deepseek.api-url:https://api.deepseek.com}")
    private String apiUrl;

    /** Spring 提供的 HTTP 客户端，用于调用远程 API（每次都 new 一个简单的实例） */
    private final RestTemplate restTemplate;

    /** 音乐数据访问层，用于在对话上下文中查询用户正在浏览的歌曲信息 */
    private final MusicMapper musicMapper;

    /**
     * 用户对话历史缓存（内存级）。
     * Key = 用户 ID，Value = 该用户最近的消息列表（按时间顺序，最多保留 10 条）。
     * 应用重启后历史会丢失，适合演示场景。
     */
    private final Map<Integer, List<Map<String, String>>> chatHistories = new HashMap<>();

    /**
     * 构造器注入（与 AGENTS.md 约定一致，不使用 @Autowired 字段注入）。
     * RestTemplate 在此处手动 new 出来，而非由 Spring 管理。
     *
     * @param musicMapper 音乐数据访问层
     */
    public AiService(MusicMapper musicMapper) {
        this.restTemplate = new RestTemplate();
        this.musicMapper = musicMapper;
    }

    /**
     * 根据歌曲标题和歌手，调用 DeepSeek 生成一段 30~80 字的中文简介。
     * 提示词要求模型：
     * <ul>
     *   <li>直接返回文字，不加任何前缀/格式</li>
     *   <li>如果是虚构歌曲，自由发挥创意</li>
     * </ul>
     *
     * @param title  歌曲标题
     * @param artist 歌手名（可为 null 或空）
     * @return 生成的简介文本；如果 API 调用失败则返回错误提示
     */
    public String generateDescription(String title, String artist) {
        // 拼接系统提示词：告诉 AI 扮演音乐编辑
        String prompt = "你是一位音乐编辑。请根据歌曲标题《" + title + "》";
        if (artist != null && !artist.isEmpty()) {
            prompt += "和歌手 " + artist;
        }
        prompt += "，写一段30-80字的中文简介，要求生动有感染力，用于展示在歌曲详情页。直接返回简介文本，不要加任何前缀。如果歌曲信息明显是虚构的，请发挥创意写一段合理的简介。";

        Map<String, Object> requestBody = buildRequestBody(prompt);
        return callDeepSeek(requestBody);
    }

    /**
     * 多轮对话入口。用户发一条消息，AI 回复一条消息。
     * 每次调用都会：
     * <ol>
     *   <li>从 {@link #chatHistories} 中取出该用户的聊天历史</li>
     *   <li>如果历史超过 10 条，截取最近 10 条</li>
     *   <li>如果指定了 {@code contextMusicId}，从数据库查询歌曲信息作为上下文注入 system prompt</li>
     *   <li>调用 DeepSeek 获取回复</li>
     *   <li>将用户消息 + AI 回复追加到历史中</li>
     *   <li>解析回复中是否包含 {@code 【搜索:关键词】} 格式的指令，若有则自动搜索歌曲推荐</li>
     * </ol>
     *
     * @param userId         当前用户 ID（作为历史记录的 key）
     * @param message        用户本次发送的消息文本
     * @param contextMusicId 用户当前浏览的歌曲 ID（可为 null），用于提供上下文
     * @return 包含 {@code reply} 和可选 {@code recommendations} 的 Map
     */
    public Map<String, Object> chat(Integer userId, String message, Integer contextMusicId) {
        // computeIfAbsent：如果该用户还没有历史记录，创建一个新列表
        List<Map<String, String>> history = chatHistories.computeIfAbsent(userId, k -> new ArrayList<>());

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个音乐平台的AI助手，名叫「音小助」。你是用户的音乐伙伴，回复要简洁亲切，用中文。");
        systemPrompt.append("你可以推荐歌曲、介绍音乐知识、分析歌词、讨论音乐风格。");
        systemPrompt.append("回答中不要使用markdown格式。");

        if (contextMusicId != null) {
            Music music = musicMapper.findById(contextMusicId);
            if (music != null) {
                systemPrompt.append("\n用户正在浏览的歌曲：").append(music.getTitle())
                        .append(" - ").append(music.getArtist())
                        .append("（").append(Optional.ofNullable(music.getDescription()).orElse("暂无简介")).append("）");
            }
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt.toString()));

        int historySize = history.size();
        if (historySize > 10) {
            history = history.subList(historySize - 10, historySize);
            chatHistories.put(userId, history);
        }
        messages.addAll(history);

        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.8);
        requestBody.put("max_tokens", 1024);

        String reply = callDeepSeek(requestBody);

        history.add(Map.of("role", "user", "content", message));
        history.add(Map.of("role", "assistant", "content", reply));

        List<Music> recommendations = null;
        if (reply.contains("【搜索:")) {
            int start = reply.lastIndexOf("【搜索:");
            int end = reply.indexOf("】", start);
            if (end > start) {
                String keyword = reply.substring(start + 4, end);
                List<Music> matched = musicMapper.findList(0, 5, keyword);
                recommendations = matched;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        if (recommendations != null && !recommendations.isEmpty()) {
            result.put("recommendations", recommendations);
        }
        return result;
    }

    /**
     * 为指定歌曲生成一句推荐语（20 字以内），用于分享或推荐场景。
     *
     * @param music    歌曲实体
     * @param userName 当前用户昵称，用于个性化称呼
     * @return AI 生成的推荐语
     */
    public String generateRecommendationReason(Music music, String userName) {
        String prompt = "用户「" + userName + "」正在听歌曲《" + music.getTitle() + "》- " + music.getArtist()
                + "，请用一句话（20字以内）描述这首歌的魅力所在，作为推荐语。";
        Map<String, Object> requestBody = buildRequestBody(prompt);
        return callDeepSeek(requestBody);
    }

    /**
     * 构建 DeepSeek 聊天补全接口的请求体（单轮对话版本）。
     * 使用默认参数：temperature=0.7（中等创造力），max_tokens=512（输出长度）。
     *
     * @param prompt 用户提示词
     * @return 符合 DeepSeek API 格式的请求体 Map
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.7);    // 0~2，值越大回答越随机
        body.put("max_tokens", 512);     // 最大输出 token 数
        return body;
    }

    /**
     * 核心方法：向 DeepSeek Chat API 发送 POST 请求并返回回复内容。
     * <p>
     * 请求地址：{apiUrl}/v1/chat/completions
     * 鉴权方式：Bearer Token（通过请求头 Authorization: Bearer {apiKey}）
     * </p>
     *
     * <h3>错误处理策略</h3>
     * 整个调用被 try-catch 包裹，任何异常（网络超时、JSON 解析失败、API 返回非 200 等）
     * 都不会向上抛出，而是返回中文错误提示。这是为了让前端无需处理异常状态，
     * 直接将返回值显示给用户即可。
     *
     * @param requestBody 请求体 Map（需包含 model、messages 等字段）
     * @return AI 回复文本；调用失败时返回友好提示
     */
    private String callDeepSeek(Map<String, Object> requestBody) {
        try {
            // 设置请求头：Content-Type + Bearer Token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发起 POST 请求，响应类型声明为 Map（泛型擦除，会有 unchecked 警告，可忽略）
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // 解析响应体：data.choices[0].message.content
            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            // API 响应结构不符合预期时的兜底
            return "AI 服务暂时无法响应，请稍后再试。";
        } catch (Exception e) {
            // 捕获所有异常（ConnectException、SocketTimeoutException、HttpClientErrorException 等）
            return "抱歉，AI 助手出小差了（" + e.getMessage() + "）";
        }
    }
}
