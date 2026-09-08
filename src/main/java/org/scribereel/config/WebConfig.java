package org.scribereel.config;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${scribereel.frontend-origin:http://localhost:5173}")
    private String frontendOrigin;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(frontendOrigin)
                        .allowedMethods("GET", "POST")
                        .allowedHeaders("*");
            }
        };
    }
}