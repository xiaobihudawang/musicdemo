package org.example.musicdemo;

import org.example.musicdemo.service.SensitiveWordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensitiveWordService 单元测试。
 *
 * 验证库集成是否生效、常见命中场景、归一化是否生效。
 * 不依赖数据库（@SpringBootTest 会复用现有上下文配置，
 * 但只调用 service bean 的内存方法，不触碰 CommentMapper）。
 */
@SpringBootTest
class SensitiveWordServiceTest {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Test
    void contextLoads() {
        // 验证 bean 注入成功，@PostConstruct 已跑完
        assertNotNull(sensitiveWordService);
    }

    @Test
    void detectsChineseProfanity() {
        assertTrue(sensitiveWordService.containsForbidden("你这个人真傻逼"));
        assertTrue(sensitiveWordService.containsForbidden("这歌太垃圾了"));
    }

    @Test
    void detectsEnglishProfanity() {
        assertTrue(sensitiveWordService.containsForbidden("fuck this song"));
        assertTrue(sensitiveWordService.containsForbidden("what the shit"));
    }

    @Test
    void detectsViolence() {
        assertTrue(sensitiveWordService.containsForbidden("杀你全家"));
        // "打死你" 是通过 EXTRA_WORDS 自定义补充的
        assertTrue(sensitiveWordService.containsForbidden("我要打死你"));
    }

    @Test
    void detectsPornography() {
        assertTrue(sensitiveWordService.containsForbidden("约炮"));
        assertTrue(sensitiveWordService.containsForbidden("裸体"));
    }

    @Test
    void allowsNormalComments() {
        assertFalse(sensitiveWordService.containsForbidden("这首歌很好听，推荐给大家"));
        assertFalse(sensitiveWordService.containsForbidden("歌词写得很有意境"));
        assertFalse(sensitiveWordService.containsForbidden(""));
        assertFalse(sensitiveWordService.containsForbidden(null));
    }

    @Test
    void handlesNormalization() {
        // 忽略大小写
        assertTrue(sensitiveWordService.containsForbidden("FUCK"));
        // 忽略全角半角
        assertTrue(sensitiveWordService.containsForbidden("ＦＵＣＫ"));
        // 忽略繁简
        assertTrue(sensitiveWordService.containsForbidden("殺你全家"));
        // 重复字符
        assertTrue(sensitiveWordService.containsForbidden("傻傻傻逼"));
    }

    @Test
    void findAllReturnsMatchedWords() {
        var matched = sensitiveWordService.findAll("这歌真垃圾fuck");
        assertFalse(matched.isEmpty());
        assertTrue(matched.size() >= 2);
    }
}
