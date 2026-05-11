package com.datashare.backend.controller;

import com.datashare.backend.model.FileEntity;
import com.datashare.backend.model.User;
import com.datashare.backend.repository.FileRepository;
import com.datashare.backend.repository.UserRepository;
import com.datashare.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // <-- Ajoute cet import
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j // <-- Ajoute cette annotation pour avoir accès à la variable "log"
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "expirationDays", defaultValue = "7") int expirationDays,
            @RequestParam(value = "tags", required = false) Set<String> tags) {

        // --- NOS LOGS DE DEBUG ---
        log.info("=== REQUÊTE D'UPLOAD REÇUE ! ===");
        if (file != null) {
            log.info("Nom du fichier reçu : {}", file.getOriginalFilename());
            log.info("Taille du fichier : {} octets", file.getSize());
            log.info("Type MIME : {}", file.getContentType());
        } else {
            log.warn("Attention : Aucun fichier n'a été reçu dans la requête !");
        }
        log.info("Paramètres : password={}, expirationDays={}, tags={}",
                (password != null ? "Présent" : "Absent"), expirationDays, tags);
        // -------------------------

        try {
            long maxSizeBytes = 1024L * 1024L * 1024L;
            if (file.getSize() > maxSizeBytes) {
                log.warn("Upload refusé : fichier trop volumineux ({} octets)", file.getSize());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Le fichier dépasse la taille maximale autorisée de 1 Go.");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && (originalFilename.endsWith(".exe") || originalFilename.endsWith(".bat"))) {
                log.warn("Upload refusé : extension interdite pour {}", originalFilename);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("L'extension du fichier est interdite pour des raisons de sécurité.");
            }

            String token = UUID.randomUUID().toString();
            log.info("Génération du token UUID : {}", token);

            String physicalPath = fileStorageService.store(file, token);
            log.info("Fichier physique stocké à l'emplacement : {}", physicalPath);

            FileEntity fileEntity = FileEntity.builder()
                    .name(originalFilename)
                    .size(file.getSize())
                    .mimeType(file.getContentType())
                    .path(physicalPath)
                    .token(token)
                    .password(password != null && !password.isEmpty() ? password : null)
                    .uploadDate(LocalDateTime.now())
                    .expirationDate(LocalDateTime.now().plusDays(expirationDays))
                    .isActive(true)
                    .tags(tags != null ? tags : new HashSet<>())
                    .user(null)
                    .build();

            FileEntity savedFile = fileRepository.save(fileEntity);
            log.info("Métadonnées sauvegardées en BDD avec l'ID : {}", savedFile.getFileId());

            Map<String, Object> response = new HashMap<>();
            response.put("fileId", savedFile.getFileId());
            response.put("name", savedFile.getName());
            response.put("size", savedFile.getSize());
            response.put("mimeType", savedFile.getMimeType());
            response.put("token", savedFile.getToken());
            response.put("expirationDate", savedFile.getExpirationDate());
            response.put("tags", savedFile.getTags());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Erreur critique lors de l'upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur est survenue lors de l'upload : " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "password", required = true) String password
            ) {

        try {

            User user = User.builder()
                    .email(email)
                    .password(password != null && !password.isEmpty() ? password : null)
                    .build();
            if (userRepository.existsByEmail(user.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cet email est déjà utilisé.");
            }

            if (user.getPassword() == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Mot de passe obligatoire.");
            }
            int len = user.getPassword().length();
            if (len < 7 ){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Mot de passe trop court.");
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("password", user.getPassword());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Erreur critique lors de l'enregistrement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur est survenue lors de l'enregistrement : " + e.getMessage());
        }
    }

}