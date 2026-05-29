package org.example.musicdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类 —— 处理静态资源映射（将虚拟 URL 路径映射到磁盘上的实际文件目录）
 *
 * ─── 作用 ───
 * 本系统中，用户上传的音乐文件（MP3 等）存储在服务器磁盘上的某个目录中（如 D:/workspace/music/）。
 * 但前端代码无法直接访问文件系统路径，需要通过 HTTP URL 来访问。
 * WebConfig 的作用就是在"虚拟 URL 路径"和"实际文件路径"之间建立映射关系。
 *
 * ─── 映射示例 ───
 * 当浏览器访问：    http://localhost:8080/api/music/file/example.mp3
 * 实际读取文件：    D:/workspace/music/example.mp3
 *
 * ─── 配置来源 ───
 * 文件存储路径在 application.yml 中通过 music.file-path 属性配置，
 * 开发环境默认指向 D:/workspace/music/（注意代码中路径写法 file: + 路径）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 音乐文件的存储根路径
     * 从 application.yml 的 ${music.file-path} 注入
     * 默认值为 D:/workspace/music/（含末尾斜杠）
     * 注意：生产环境中应改为 Linux 路径如 /data/music/
     */
    @Value("${music.file-path}")
    private String filePath;

    /**
     * 添加资源处理器 —— 将虚拟路径 /api/music/file/** 映射到本地文件系统
     *
     * 实现原理：
     * Spring MVC 的 ResourceHandlerRegistry 可以注册一个或多个资源处理器，
     * 每个处理器将匹配的 URL 模式映射到一组资源位置（classpath:、file:、http: 等）。
     *
     * 这里使用 "file:" 前缀来指向本地文件系统路径，Spring 会自动将 URL 中的
     * 路径部分拼接到 filePath 后面，找到对应的文件并返回给客户端。
     *
     * @param registry Spring MVC 的资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // addResourceHandler：定义 URL 匹配模式（虚拟路径）
        // addResourceLocations：定义文件在磁盘上的实际位置
        registry.addResourceHandler("/api/music/file/**")
                .addResourceLocations("file:" + filePath);

        // 封面图文件映射：/api/music/cover/** → {filePath}（cover_path 已含 covers/ 前缀）
        registry.addResourceHandler("/api/music/cover/**")
                .addResourceLocations("file:" + filePath);
    }
}