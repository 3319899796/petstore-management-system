package com.ningcong.petstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 通用 Bean。
 *
 * <p>密码用 BCrypt：加盐哈希、计算慢（故意），库里泄露也拿不到明文；
 * 只引 spring-security-crypto 这一个 jar，不引入整套 Spring Security 过滤器链。</p>
 */
@Configuration
public class BeanConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
