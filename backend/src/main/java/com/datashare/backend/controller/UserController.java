

package com.datashare.backend.controller;


import com.datashare.backend.model.User;

import com.datashare.backend.repository.UserRepository;
import com.datashare.backend.service.JwtService;
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


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;



    @PostMapping("/register")
    public ResponseEntity<?> register(

            @RequestParam("email") String email,
            @RequestParam("password") String password) {

        try {

            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cet email est déjà utilisé.");
            }

            if (password.length() < 7) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Mot de passe trop court. Il doit contenir au moins 7 caractères.");
            }

            String hashedPassword = passwordEncoder.encode(password);

            User user = User.builder()
                    .email(email)
                    .password(hashedPassword)
                    .build();

            userRepository.save(user);


            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("message", "Compte créé avec succès.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Erreur critique lors de l'enregistrement de l'utilisateur {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur inattendue est survenue lors de l'enregistrement.");
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {

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
    }
}