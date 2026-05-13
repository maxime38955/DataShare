package com.datashare.backend.controller;

import com.datashare.backend.model.User;
import com.datashare.backend.repository.UserRepository;
import com.datashare.backend.service.JwtService;
import com.datashare.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Désactive la sécurité pour tester le contrôleur de façon isolée
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // On mocke toutes les dépendances injectées dans le UserController
    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private JwtService jwtService;

    // ==========================================
    // TESTS POUR /user/register
    // ==========================================

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        // Arrange
        User mockUser = User.builder().email("test@mail.com").build();
        Mockito.when(userService.registerUser("test@mail.com", "password123")).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/user/register")
                        .param("email", "test@mail.com")
                        .param("password", "password123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.message").value("Compte créé avec succès."));
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        // Act & Assert (Pas besoin de mocker le service, le code s'arrête avant)
        mockMvc.perform(post("/user/register")
                        .param("email", "test@mail.com")
                        .param("password", "123456")) // Seulement 6 caractères
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Mot de passe trop court. Il doit contenir au moins 7 caractères."));
    }

    @Test
    void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
        // Arrange : On simule l'erreur métier lancée par le UserService
        Mockito.when(userService.registerUser("existant@mail.com", "password123"))
                .thenThrow(new IllegalArgumentException("Cet email est déjà utilisé."));

        // Act & Assert
        mockMvc.perform(post("/user/register")
                        .param("email", "existant@mail.com")
                        .param("password", "password123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cet email est déjà utilisé."));
    }

    @Test
    void shouldReturnInternalServerErrorOnUnexpectedException() throws Exception {
        // Arrange : On simule un crash grave (ex: base de données inaccessible)
        Mockito.when(userService.registerUser(anyString(), anyString()))
                .thenThrow(new RuntimeException("Crash base de données"));

        // Act & Assert
        mockMvc.perform(post("/user/register")
                        .param("email", "test@mail.com")
                        .param("password", "password123"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Une erreur inattendue est survenue lors de l'enregistrement."));
    }

    // ==========================================
    // TESTS POUR /user/login
    // ==========================================

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Arrange
        User mockUser = User.builder().email("test@mail.com").password("hashedPassword").build();

        Mockito.when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(mockUser));
        Mockito.when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        Mockito.when(jwtService.generateToken("test@mail.com")).thenReturn("fake-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/user/login")
                        .param("email", "test@mail.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsIncorrect() throws Exception {
        // Arrange
        User mockUser = User.builder().email("test@mail.com").password("hashedPassword").build();

        Mockito.when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(mockUser));
        // Le mot de passe ne correspond pas :
        Mockito.when(passwordEncoder.matches("mauvais_password", "hashedPassword")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/user/login")
                        .param("email", "test@mail.com")
                        .param("password", "mauvais_password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Identifiants incorrects"));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserNotFound() throws Exception {
        // Arrange : Utilisateur inexistant
        Mockito.when(userRepository.findByEmail("inconnu@mail.com")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/user/login")
                        .param("email", "inconnu@mail.com")
                        .param("password", "password123"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Identifiants incorrects"));
    }
}