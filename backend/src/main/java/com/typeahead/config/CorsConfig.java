package com.typeahead.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration — allows the React frontend (running on :5173 in dev)
 * to call the Spring Boot backend (:8080) without browser cross-origin errors.
 *
 * <p>In production both frontend and backend would be served from the same origin,
 * making this unnecessary, but it is required for local development where Vite and
 * Spring Boot run on different ports.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:5173",  // Vite dev server
                                "http://localhost:3000"   // fallback
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
