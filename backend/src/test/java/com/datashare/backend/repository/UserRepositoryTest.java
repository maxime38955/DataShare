package com.datashare.backend.repository;

import com.datashare.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest // Charge uniquement la couche Base de données (très rapide)
@ActiveProfiles("test") // Utilise le fichier application-test.yml (base H2)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        // 1. Préparation (Arrange)
        User user = User.builder()
                .email("test@mail.com")
                .password("hashedpass")
                .build();
        userRepository.save(user);

        // 2. Exécution (Act)
        Optional<User> foundUser = userRepository.findByEmail("test@mail.com");

        // 3. Vérification (Assert)
        assertTrue(foundUser.isPresent(), "L'utilisateur devrait être trouvé");
        assertEquals("test@mail.com", foundUser.get().getEmail());
    }

    @Test
    void shouldNotFindUserWithWrongEmail() {
        Optional<User> foundUser = userRepository.findByEmail("inexistant@mail.com");
        assertTrue(foundUser.isEmpty(), "L'utilisateur ne devrait pas exister");
    }
}