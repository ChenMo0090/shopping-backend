package com.shop.config;

import com.shop.common.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：拦截器 + CORS
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${shop.upload.path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将本地 uploads 目录映射为 /uploads/** 静态资源
        File uploadDir = new File(uploadPath).getAbsoluteFile();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir.getAbsolutePath() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 以下路径不需要登录（context-path=/api 已在容器层处理，这里不需要 /api 前缀）
                .excludePathPatterns(
                        "/user/login",
                        "/product/list",
                        "/product/detail/**",
                        "/product/categories",
                        "/group/detail/**",
                        "/pay/notify",
                        "/refund/notify",
                        "/admin/login",
                        "/upload",
                        "/uploads/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/doc.html",
                        "/webjars/**",
                        "/error"
                );
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
