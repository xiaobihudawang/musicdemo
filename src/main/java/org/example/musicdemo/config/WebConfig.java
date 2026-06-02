package org.example.musicdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置，处理静态资源映射（虚拟 URL 映射到磁盘文件）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${music.file-path}")
    private String filePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/music/file/**")
                .addResourceLocations("file:" + filePath);

        registry.addResourceHandler("/api/music/cover/**")
                .addResourceLocations("file:" + filePath);
    }
}