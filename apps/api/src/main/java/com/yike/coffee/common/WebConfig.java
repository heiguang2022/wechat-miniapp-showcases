package com.yike.coffee.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String uploadDir;
    public WebConfig(@Value("${app.upload-dir}") String uploadDir){this.uploadDir=uploadDir;}
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry){
        registry.addResourceHandler("/uploads/**").addResourceLocations(Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString());
    }
    @Override public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/api/**").allowedOriginPatterns("http://localhost:*","https://localhost:*").allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS").allowedHeaders("*");
    }
}
