package com.datashare.backend.controller;

import com.datashare.backend.dto.AuthResponse;
import com.datashare.backend.dto.LoginRequest;
import com.datashare.backend.dto.RegisterRequest;
import com.datashare.backend.model.User;
import com.datashare.backend.repository.UserRepository;
import com.datashare.backend.service.JwtService;
import com.datashare.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) { // Utilisation du DTO
        try {
            if (request.getPassword().length() < 7) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Mot de passe trop court.");
            }

            User user = userService.registerUser(request.getEmail(), request.getPassword());

            // On renvoie un AuthResponse avec un token tout de suite, c'est plus sympa pour l'UX
            String token = jwtService.generateToken(user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(token, user.getEmail(), "Compte créé avec succès."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur register", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur inattendue.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                String token = jwtService.generateToken(user.getEmail());
                return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), "Connexion réussie"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects");
        }
    }
}