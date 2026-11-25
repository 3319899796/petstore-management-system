package com.ningcong.petstore.config;

import com.ningcong.petstore.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册登录拦截器 + 跨域。
 *
 * <p>注意：context-path 是 /api，所以路径模式都相对于 /api 之后的部分。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /** 照片落盘根目录（application.yml 里的 app.upload.dir） */
    @Value("${app.upload.dir}")
    private String uploadDir;

    /** 照片访问前缀，例如 /files */
    @Value("${app.upload.url-prefix}")
    private String uploadUrlPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 登录接口本身不能要求令牌；/error 是 Spring 的错误转发路径；
                // /files/** 是照片静态资源，<img> 标签带不了 Authorization 头，必须放行
                .excludePathPatterns("/auth/login", "/error", uploadUrlPrefix + "/**");
    }

    /**
     * 把磁盘上的照片目录映射成静态资源 URL。
     *
     * <p>因为有 context-path=/api，最终访问地址是 http://127.0.0.1:8080/api/files/2026/11/xxx.jpg。
     * 接口返回的 photoPath 只带 /files/... 这一段，前端加上 /api 前缀即可显示。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = uploadUrlPrefix.endsWith("/")
                ? uploadUrlPrefix.substring(0, uploadUrlPrefix.length() - 1)
                : uploadUrlPrefix;
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations("file:" + normalizeDir(uploadDir));
    }

    /** 统一成 file:D:/xxx/ 这种写法，反斜杠会让 addResourceLocations 解析失败。 */
    private String normalizeDir(String dir) {
        String value = dir.replace("\\", "/");
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
