package org.example.musicdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot 启动入口类。
 * @SpringBootApplication 自动扫描组件，@MapperScan 扫描 MyBatis Mapper，@EnableAsync 启用异步支持。
 */
@SpringBootApplication
@MapperScan("org.example.musicdemo.mapper")
@EnableAsync
public class MusicdemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicdemoApplication.class, args);
    }
}
