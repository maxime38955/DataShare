package com.datashare.backend.service;

import com.datashare.backend.model.User;
import com.datashare.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class) // Test unitaire pur (très rapide)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        String email = "nouveau@test.fr";
        String password = "password123";
        Mockito.when(userRepository.existsByEmail(email)).thenReturn(false);
        Mockito.when(passwordEncoder.encode(password)).thenReturn("haché_123");

        // Act
        userService.registerUser(email, password);

        // Assert
        // On capture l'utilisateur sauvegardé pour vérifier ses valeurs
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("nouveau@test.fr", savedUser.getEmail(), "L'email doit correspondre");
        assertEquals("haché_123", savedUser.getPassword(), "Le mot de passe doit être encodé");

        Mockito.verify(passwordEncoder).encode(password);
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        String email = "deja@pris.fr";
        Mockito.when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(email, "password123");
        });

        // Optionnel mais recommandé : vérifier le message d'erreur
        assertEquals("Cet email est déjà utilisé.", exception.getMessage());

        // Vérifier qu'on n'a pas essayé d'encoder le mot de passe ou de sauvegarder
        Mockito.verify(passwordEncoder, Mockito.never()).encode(Mockito.anyString());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }

    // 🌟 NOUVEAU TEST : Vérification de la taille du mot de passe
    @Test
    void shouldThrowExceptionWhenPasswordIsTooShort() {
        // Arrange
        String email = "test@mail.com";
        String shortPassword = "123456"; // Seulement 6 caractères

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(email, shortPassword);
        });

        assertEquals("Mot de passe trop court. Il doit contenir au moins 7 caractères.", exception.getMessage());

        // Vérifier qu'on ne sollicite pas la base de données
        Mockito.verify(userRepository, Mockito.never()).existsByEmail(Mockito.anyString());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
    }
}