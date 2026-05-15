package com.datashare.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // On instancie le service "à la main" avant chaque test
        jwtService = new JwtService();
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String email = "maxime@test.fr";

        // 1. Génération
        String token = jwtService.generateToken(email);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // 2. Extraction & Validation
        String extractedEmail = jwtService.extractUsername(token);
        assertEquals(email, extractedEmail, "L'email extrait doit correspondre à celui encodé");

        UserDetails dummyUser = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("password") // Le password n'importe pas ici
                .authorities("USER")
                .build();

        // On passe l'objet dummyUser au lieu de la simple String email
        boolean isValid = jwtService.isTokenValid(token, dummyUser);

        assertTrue(isValid, "Le token généré à l'instant devrait être valide pour cet utilisateur");
    }
}