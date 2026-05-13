package com.datashare.backend.service;

import com.datashare.backend.model.User;
import com.datashare.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(String email, String password) {
        // 1. On vérifie si l'email existe déjà
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // 2. On crée l'utilisateur avec un mot de passe protégé (haché)
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        // 3. On sauvegarde en base de données
        return userRepository.save(user);
    }
}