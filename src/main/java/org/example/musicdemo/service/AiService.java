package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.MusicMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 服务，对接 DeepSeek API，提供歌曲描述生成功能。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final MusicMapper musicMapper;

    public AiService(MusicMapper musicMapper) {
        this.restTemplate = new RestTemplate();
        this.musicMapper = musicMapper;
    }

    /** 根据歌曲标题和歌手生成 30~80 字中文简介 */
    public String generateDescription(String title, String artist) {
        String prompt = "你是一位音乐编辑。请根据歌曲标题《" + title + "》";
        if (artist != null && !artist.isEmpty()) {
            prompt += "和歌手 " + artist;
        }
        prompt += "，写一段30-80字的中文简介，要求生动有感染力，用于展示在歌曲详情页。直接返回简介文本，不要加任何前缀。如果歌曲信息明显是虚构的，请发挥创意写一段合理的简介。";

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

    /** 向 DeepSeek API 发送请求并返回回复 */
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
        } catch (ResourceAccessException e) {
            log.error("AI 服务连接超时或不可达: {}", e.getMessage());
            return "AI 服务响应超时，请稍后再试。";
        } catch (RestClientResponseException e) {
            log.error("AI 服务返回错误: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return "AI 服务暂时不可用（" + e.getStatusCode() + "），请稍后再试。";
        } catch (Exception e) {
            log.error("AI 服务调用异常: {}", e.getMessage(), e);
            return "AI 助手暂时无法响应，请稍后再试。";
        }
    }
}
