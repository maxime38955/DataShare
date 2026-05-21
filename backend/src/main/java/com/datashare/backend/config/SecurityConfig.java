package com.datashare.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. On active le CORS avec la config définie plus bas
                .cors(Customizer.withDefaults())

                // 2. On désactive le CSRF (puisqu'on utilise des tokens JWT)
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // TRÈS IMPORTANT : On autorise TOUTES les requêtes OPTIONS (le preflight)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // 1. On autorise l'accès public au login et register
                        .requestMatchers(
                                "/user/login", "/user/register",
                                "/api/v1/user/login", "/api/v1/user/register"
                        ).permitAll()

                        // 2. NOUVEAU : On autorise l'accès public aux fichiers (Upload, Download, Metadata)
                        .requestMatchers(
                                "/files/upload", "/api/v1/files/upload",
                                "/files/download/**", "/api/v1/files/download/**",
                                "/files/metadata/**", "/api/v1/files/metadata/**"
                        ).permitAll()

                        // 3. Le reste doit être authentifié (ex: /files/user/files pour l'historique)
                        .anyRequest().authenticated()
                )
                // 4. On ajoute le filtre JWT
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Assure-toi d'avoir ce Bean EXACTEMENT comme ça dans SecurityConfig
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200")); // Ton front Angular
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}