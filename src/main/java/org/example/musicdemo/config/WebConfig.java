package org.example.musicdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 把虚拟 URL 路径映射到实际的文件目录
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${music.file-path}")
    private String filePath;

    /**
     * 添加资源处理器
     * 当浏览器访问 /api/music/file/xxx.mp3 时
     * 实际读取 D:/workspace/music/xxx.mp3 这个文件
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/music/file/**")
                .addResourceLocations("file:" + filePath);
    }
}