package com.datashare.backend.controller;

import com.datashare.backend.dto.FileResponseDTO; // Import de ton nouveau DTO
import com.datashare.backend.model.FileEntity;
import com.datashare.backend.service.FileService;
import com.datashare.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.datashare.backend.config.ApplicationConfig;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "expirationDays", defaultValue = "7") int expirationDays,
            @RequestParam(value = "tags", required = false) Set<String> tags,
            Principal principal) {

        try {
            password = passwordEncoder.encode(password);
            String email = (principal != null) ? principal.getName() : null;


            FileEntity savedFile = fileService.processUpload(file, password, expirationDays, tags, email);


            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponseDTO(savedFile));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur critique lors de l'upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur est survenue lors de l'upload.");
        }
    }

    @GetMapping("/user/files")
    public ResponseEntity<?> getUserFiles(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accès refusé. Connectez-vous.");
            }

            List<FileEntity> history = fileService.getUserHistory(principal.getName());


            List<FileResponseDTO> responseList = history.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'historique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Impossible de récupérer l'historique.");
        }
    }

    @GetMapping("/metadata/{token}")
    public ResponseEntity<?> getMetadata(@PathVariable String token) {
        try {
            FileEntity file = fileService.getFileByToken(token);

            // On renvoie le DTO, ce qui masque automatiquement le "path" physique du fichier
            return ResponseEntity.ok(mapToResponseDTO(file));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }



    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String token,
            @RequestParam(required = false) String password // 1. On récupère le mot de passe s'il est envoyé
    ) {
        try {
            FileEntity fileEntity = fileService.getFileByToken(token);

            // 2. VÉRIFICATION DU MOT DE PASSE
            // On vérifie si le fichier possède un mot de passe en base de données
            if (fileEntity.getPassword() != null && !fileEntity.getPassword().isEmpty()) {

                // Si le fichier est protégé mais que l'utilisateur n'a rien envoyé -> 401 Unauthorized
                if (password == null || password.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }


                // Exemple basique (si le mot de passe n'est pas crypté en BDD) :
                if (!passwordEncoder.matches(password, fileEntity.getPassword())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
                }
            }

            // 3. Si tout est OK, on charge le fichier
            Resource resource = fileStorageService.load(fileEntity.getPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getName() + "\"")
                    .body(resource);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build(); // 500 en cas d'erreur de lecture
        }
    }

    @DeleteMapping("/user/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accès refusé.");
            }

            fileService.deleteSecuredFile(fileId, principal.getName());
            return ResponseEntity.ok("Fichier supprimé avec succès.");

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du fichier {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur technique.");
        }
    }


    private FileResponseDTO mapToResponseDTO(FileEntity entity) {
        return FileResponseDTO.builder()
                .fileId(entity.getFileId())
                .name(entity.getName())
                .size(entity.getSize())
                .mimeType(entity.getMimeType())
                .token(entity.getToken())
                .uploadDate(entity.getUploadDate())
                .expirationDate(entity.getExpirationDate())
                .isActive(entity.getIsActive())
                .password(entity.getPassword())
                .tags(entity.getTags())
                .build();
    }
}