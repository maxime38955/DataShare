package com.datashare.backend.controller;

import com.datashare.backend.model.User;
import com.datashare.backend.repository.UserRepository;
import com.datashare.backend.service.JwtService;
import com.datashare.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private org.springframework.security.web.SecurityFilterChain securityFilterChain;
    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private JwtService jwtService;

    // ==========================================
    // TESTS POUR /user/register
    // ==========================================

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        User mockUser = User.builder().email("test@mail.com").build();
        Mockito.when(userService.registerUser("test@mail.com", "password123")).thenReturn(mockUser);
        Mockito.when(jwtService.generateToken("test@mail.com")).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@mail.com\", \"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.message").value("Compte créé avec succès."));
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@mail.com\", \"password\":\"123456\"}")) // 6 caractères
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Mot de passe trop court.")); // Modifié pour correspondre à ton Controller
    }

    @Test
    void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
        Mockito.when(userService.registerUser("existant@mail.com", "password123"))
                .thenThrow(new IllegalArgumentException("Cet email est déjà utilisé."));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existant@mail.com\", \"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cet email est déjà utilisé."));
    }

    @Test
    void shouldReturnInternalServerErrorOnUnexpectedException() throws Exception {
        Mockito.when(userService.registerUser(anyString(), anyString()))
                .thenThrow(new RuntimeException("Crash base de données"));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@mail.com\", \"password\":\"password123\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur inattendue.")); // Modifié pour correspondre à ton Controller
    }

    // ==========================================
    // TESTS POUR /user/login
    // ==========================================

    @Test
    void shouldLoginSuccessfully() throws Exception {
        User mockUser = User.builder().email("test@mail.com").password("hashedPassword").build();

        Mockito.when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(mockUser));
        Mockito.when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        Mockito.when(jwtService.generateToken("test@mail.com")).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@mail.com\", \"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsIncorrect() throws Exception {
        User mockUser = User.builder().email("test@mail.com").password("hashedPassword").build();

        Mockito.when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(mockUser));
        Mockito.when(passwordEncoder.matches("mauvais_password", "hashedPassword")).thenReturn(false);

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@mail.com\", \"password\":\"mauvais_password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Identifiants incorrects"));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserNotFound() throws Exception {
        Mockito.when(userRepository.findByEmail("inconnu@mail.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inconnu@mail.com\", \"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Identifiants incorrects"));
    }
}