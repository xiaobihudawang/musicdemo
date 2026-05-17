package org.example.musicdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用启动入口
 * 通过 @MapperScan 自动扫描 MyBatis Mapper 接口
 */
@SpringBootApplication
@MapperScan("org.example.musicdemo.mapper")
public class MusicdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicdemoApplication.class, args);
    }

}
