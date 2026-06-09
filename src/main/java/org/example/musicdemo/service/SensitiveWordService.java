package org.example.musicdemo.service;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 敏感词检测服务。
 *
 * 基于 sensitive-word 库（DFA 算法）实现：
 *   - 内置 6W+ 词库，覆盖黄色 / 暴力 / 侮辱 / 政治等常见敏感词
 *   - 启动时通过 @PostConstruct 初始化并预热（首次构建 DFA Trie 较耗时）
 *   - 自动开启繁简 / 大小写 / 全角半角 / 数字形式等归一化，抗常见规避手段
 *
 * 使用方法：在 CommentService.add() 等需要校验用户输入的地方调用
 *           containsForbidden(text)，命中即抛 RuntimeException。
 */
@Service
public class SensitiveWordService {

    private SensitiveWordBs sensitiveWordBs;

    /**
     * 项目自定义补充词 —— 覆盖音乐评论中常见的脏话变体与
     * 内置库可能未收录的口语化骂人话。
     * 注意：内置库已覆盖 6W+ 词，补充列表只需补足场景化术语。
     */
    private static final List<String> EXTRA_WORDS = Arrays.asList(
            // 侮辱补充
            "垃圾", "屎", "狗屎", "狗东西", "脑残", "脑瘫", "弱智", "蠢货", "废物",
            "杠精", "喷子", "键盘侠", "阴阳怪气", "智障", "低能", "二货", "二百五",
            // 暴力补充
            "打死你", "打死他", "打死她", "打死它", "弄死你", "弄死他", "整死你",
            "砍死", "炸死", "揍死", "锤死", "踢死", "掐死", "勒死",
            // 音乐场景
            "歌屎", "歌烂", "难听到爆", "污染耳朵", "噪音", "狗叫"
    );

    @PostConstruct
    public void init() {
        sensitiveWordBs = SensitiveWordBs.newInstance()
                .ignoreCase(true)           // 忽略英文大小写
                .ignoreWidth(true)          // 全角 / 半角互换
                .ignoreNumStyle(true)       // 数字常见形式互换（"1" = "一" = "壹"）
                .ignoreChineseStyle(true)   // 中文繁简互换
                .ignoreEnglishStyle(true)   // 英文常见写法互换（"fuck" = "f*u*c*k"）
                .ignoreRepeat(true)         // 忽略重复字符（"傻傻傻逼" 也能命中）
                .wordFailFast(true)         // 命中后立即返回，提升性能
                .init();

        // 补充自定义词
        sensitiveWordBs.addWord(EXTRA_WORDS);
    }

    /**
     * 判断文本是否包含敏感词。
     *
     * @param text 待检测文本
     * @return true 包含敏感词；false 干净
     */
    public boolean containsForbidden(String text) {
        if (text == null || text.isEmpty()) return false;
        return sensitiveWordBs.contains(text);
    }
}
