package com.JobGraph.jobgraph_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.LinkedHashSet;
import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-url}") String frontendUrl) {
        CorsConfiguration cors = new CorsConfiguration();
        // Session cookies require exact allowed origins and a credentials header.
        cors.setAllowedOrigins(List.copyOf(new LinkedHashSet<>(List.of(
                frontendUrl, "http://localhost:5173", "http://127.0.0.1:5173"))));
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
