package com.praio.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] ORIGENS_DESENVOLVIMENTO = {
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000",
            "http://192.168.*.*:5173",
            "http://10.*.*.*:5173",
            "http://172.16.*.*:5173",
            "http://172.17.*.*:5173",
            "http://172.18.*.*:5173",
            "http://172.19.*.*:5173",
            "http://172.20.*.*:5173",
            "http://172.21.*.*:5173",
            "http://172.22.*.*:5173",
            "http://172.23.*.*:5173",
            "http://172.24.*.*:5173",
            "http://172.25.*.*:5173",
            "http://172.26.*.*:5173",
            "http://172.27.*.*:5173",
            "http://172.28.*.*:5173",
            "http://172.29.*.*:5173",
            "http://172.30.*.*:5173",
            "http://172.31.*.*:5173"
    };

    @Value("${praio.uploads.dir:uploads}")
    private String uploadsDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(ORIGENS_DESENVOLVIMENTO)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true);

        registry.addMapping("/uploads/**")
                .allowedOriginPatterns(ORIGENS_DESENVOLVIMENTO)
                .allowedMethods("GET")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadsDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
