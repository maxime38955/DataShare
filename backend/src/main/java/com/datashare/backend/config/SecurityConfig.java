package com.datashare.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    // 1. On ignore complètement la sécurité sur nos patterns de routes publics,
    // avec et sans le préfixe de servlet context-path pour être 100% robuste.
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        log.info("////////test/////////");
        return (web) -> web.ignoring().requestMatchers(

                "/files/upload",
                "/api/v1/files/upload",
                "/files/metadata/**",
                "/api/v1/files/metadata/**",
                "/files/download/**",
                "/api/v1/files/download/**",
                "/auth/**",
                "/api/v1/auth/**"

        );

    }

    // 2. Chaîne de filtrage principale
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.debug("////////stest/////////");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable) // Désactive le CORS par défaut de Security (WebConfig s'en charge déjà)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // On s'assure encore une fois que ces endpoints sont autorisés
                        .requestMatchers("/api/v1/files/upload", "/files/upload").permitAll()
                        .requestMatchers("/api/v1/files/metadata/**", "/files/metadata/**").permitAll()
                        .requestMatchers("/api/v1/files/download/**", "/files/download/**").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/auth/**").permitAll()
                        // Toutes les autres requêtes nécessiteront un compte
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}