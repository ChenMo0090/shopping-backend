package com.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 微信小程序拼团商城 - 启动类
 */
@SpringBootApplication
@MapperScan("com.shop.**.mapper")
@EnableCaching
@EnableScheduling
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
        System.out.println("""
                ╔═══════════════════════════════════════╗
                ║   拼团商城后端服务启动成功！              ║
                ║   API 文档：http://localhost:8080/doc.html ║
                ╚═══════════════════════════════════════╝
                """);
    }
}