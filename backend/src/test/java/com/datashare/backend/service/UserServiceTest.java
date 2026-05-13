package com.datashare.backend.service;

import com.datashare.backend.model.User;
import com.datashare.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        Mockito.verify(userRepository).save(Mockito.any(User.class));
        Mockito.verify(passwordEncoder).encode(password);
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        String email = "deja@pris.fr";
        Mockito.when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(email, "password");
        });
    }
}