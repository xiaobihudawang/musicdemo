package org.example.musicdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 音乐平台应用的 Spring Boot 启动入口类。
 *
 * <p>这是整个应用的入口点，通过 {@code main} 方法启动嵌入式的 Tomcat 服务器
 * 并初始化 Spring IoC 容器。所有 Bean 的自动配置、组件扫描、属性绑定等
 * 均由 {@code @SpringBootApplication} 注解驱动。</p>
 *
 * <p>关键注解说明：</p>
 * <ul>
 *   <li><b>@SpringBootApplication</b>：组合注解，等效于
 *       {@code @Configuration + @EnableAutoConfiguration + @ComponentScan}。
 *       自动扫描 {@code org.example.musicdemo} 及其子包下的所有 Spring 组件
 *       （@Component、@Service、@Repository、@Controller 等）。</li>
 *   <li><b>@MapperScan("org.example.musicdemo.mapper")</b>：指定 MyBatis 的 Mapper 接口扫描路径。
 *       被扫描的接口会被动态代理生成实现类并注册为 Spring Bean，
 *       之后可以直接通过 {@code @Autowired} 或构造器注入使用，无需手动编写实现类。
 *       Mapper 对应的 SQL 定义在 {@code src/main/resources/mapper/} 目录下的 XML 文件中。</li>
 *   <li><b>@EnableAsync</b>：启用异步方法支持，配合 {@code @Async} 注解使用。</li>
 * </ul>
 *
 * <p>应用配置：</p>
 * <ul>
 *   <li>配置文件：{@code application.yml}</li>
 *   <li>服务器端口：8443（HTTPS）</li>
 *   <li>数据库：MySQL 8.0，数据库名 music_platform</li>
 *   <li>MyBatis 映射：驼峰命名自动转换（map-underscore-to-camel-case: true）</li>
 * </ul>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.mybatis.spring.annotation.MapperScan
 */
@SpringBootApplication
@MapperScan("org.example.musicdemo.mapper")
@EnableAsync
public class MusicdemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicdemoApplication.class, args);
    }

}
