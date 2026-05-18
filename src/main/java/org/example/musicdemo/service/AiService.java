package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiService {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final MusicMapper musicMapper;

    private final Map<Integer, List<Map<String, String>>> chatHistories = new HashMap<>();

    public AiService(MusicMapper musicMapper) {
        this.restTemplate = new RestTemplate();
        this.musicMapper = musicMapper;
    }

    public String generateDescription(String title, String artist) {
        String prompt = "你是一位音乐编辑。请根据歌曲标题《" + title + "》";
        if (artist != null && !artist.isEmpty()) {
            prompt += "和歌手 " + artist;
        }
        prompt += "，写一段30-80字的中文简介，要求生动有感染力，用于展示在歌曲详情页。直接返回简介文本，不要加任何前缀。如果歌曲信息明显是虚构的，请发挥创意写一段合理的简介。";

        Map<String, Object> requestBody = buildRequestBody(prompt);
        return callDeepSeek(requestBody);
    }

    public Map<String, Object> chat(Integer userId, String message, Integer contextMusicId) {
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

    public String generateRecommendationReason(Music music, String userName) {
        String prompt = "用户「" + userName + "」正在听歌曲《" + music.getTitle() + "》- " + music.getArtist()
                + "，请用一句话（20字以内）描述这首歌的魅力所在，作为推荐语。";
        Map<String, Object> requestBody = buildRequestBody(prompt);
        return callDeepSeek(requestBody);
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.7);
        body.put("max_tokens", 512);
        return body;
    }

    private String callDeepSeek(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            return "AI 服务暂时无法响应，请稍后再试。";
        } catch (Exception e) {
            return "抱歉，AI 助手出小差了（" + e.getMessage() + "）";
        }
    }
}
