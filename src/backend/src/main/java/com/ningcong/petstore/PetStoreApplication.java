package com.ningcong.petstore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 宠物店宠物信息管理系统 启动类。
 *
 * <p>启动顺序：加载 application.yml → 初始化数据源 → MyBatis-Plus 扫描 mapper 包 →
 * 注册拦截器/切面 → 由 DataInitializer 写入演示账号 → 内置 Tomcat 监听 8080。</p>
 *
 * <p>@EnableScheduling 开启 Spring Task，让 VaccineReminderTask 里的 @Scheduled 生效。</p>
 */
@SpringBootApplication
@MapperScan("com.ningcong.petstore.mapper")
@EnableScheduling
public class PetStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetStoreApplication.class, args);
    }
}
