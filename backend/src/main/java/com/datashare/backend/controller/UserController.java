package com.datashare.backend.controller;

import com.datashare.backend.model.User;
import com.datashare.backend.repository.UserRepository;
import com.datashare.backend.service.JwtService;
import com.datashare.backend.service.UserService; // <-- N'oublie pas l'import !
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

    private final UserService userService; // <-- Injection de ton nouveau service
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam("email") String email,
            @RequestParam("password") String password) {

        try {
            // 1. Validation de base (le contrôleur garde ce rôle)
            if (password.length() < 7) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Mot de passe trop court. Il doit contenir au moins 7 caractères.");
            }

            // 2. On délègue tout le travail complexe au Service !
            User user = userService.registerUser(email, password);

            // 3. On prépare la réponse de succès
            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("message", "Compte créé avec succès.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // On attrape spécifiquement l'erreur "Cet email est déjà utilisé" lancée par ton Service
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (Exception e) {
            log.error("Erreur critique lors de l'enregistrement de l'utilisateur {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur inattendue est survenue lors de l'enregistrement.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (passwordEncoder.matches(password, user.getPassword())) {
                String token = jwtService.generateToken(user.getEmail());

                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects");
            }
        } catch (Exception e) {
            // Sécurité : Ne jamais préciser si c'est l'email ou le mot de passe qui est faux
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects");
        }
    }
}