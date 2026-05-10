package com.datashare.backend.config; // Ajuste le package si nécessaire

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Autorise tous les endpoints de l'API
                        .allowedOrigins("http://localhost:4200") // L'adresse de ton application Angular
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Les requêtes HTTP autorisées
                        .allowedHeaders("*") // Autorise tous les headers (important pour le futur header Authorization avec le JWT)
                        .allowCredentials(true);
            }
        };
    }
}