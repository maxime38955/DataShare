package com.datashare.backend.controller;

import com.datashare.backend.model.FileEntity;
import com.datashare.backend.model.User;
import com.datashare.backend.repository.FileRepository;
import com.datashare.backend.repository.UserRepository;
import com.datashare.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "expirationDays", defaultValue = "7") int expirationDays,
            @RequestParam(value = "tags", required = false) Set<String> tags,
            Principal principal) {

        try {
            long maxSizeBytes = 1024L * 1024L * 1024L;
            if (file.getSize() > maxSizeBytes) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Le fichier dépasse la taille maximale autorisée de 1 Go.");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && (originalFilename.endsWith(".exe") || originalFilename.endsWith(".bat"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("L'extension du fichier est interdite pour des raisons de sécurité.");
            }

            User uploader = null;
            if (principal != null) {
                uploader = userRepository.findByEmail(principal.getName()).orElse(null);
            }

            String token = UUID.randomUUID().toString();
            String physicalPath = fileStorageService.store(file, token);

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
                    .user(uploader)
                    .build();

            FileEntity savedFile = fileRepository.save(fileEntity);

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

    @GetMapping("/user/files")
    public ResponseEntity<?> getUserFiles(Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Accès refusé. Connectez-vous.");
            }

            String email = principal.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<FileEntity> userFiles = fileRepository.findByUser(user);

            return ResponseEntity.ok(userFiles);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'historique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Impossible de récupérer l'historique des fichiers.");
        }
    }

    @GetMapping("/metadata/{token}")
    public ResponseEntity<?> getMetadata(@PathVariable String token) {
        Optional<FileEntity> fileOpt = fileRepository.findByToken(token);

        if (fileOpt.isPresent()) {
            FileEntity file = fileOpt.get();
            Map<String, Object> meta = new HashMap<>();
            meta.put("name", file.getName());
            meta.put("size", file.getSize());
            meta.put("mimeType", file.getMimeType());
            meta.put("expirationDate", file.getExpirationDate());

            return ResponseEntity.ok(meta);
        }
        else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Lien invalide ou expiré.");
        }
    }


    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String token) {
        Optional<FileEntity> fileOpt = fileRepository.findByToken(token);

        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FileEntity fileEntity = fileOpt.get();
        Resource resource = fileStorageService.load(fileEntity.getPath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))

                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getName() + "\"")
                .body(resource);
    }


    @DeleteMapping("/user/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId, Principal principal) {
        try {

            FileEntity file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Fichier introuvable"));


            if (file.getUser() == null || !file.getUser().getEmail().equals(principal.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas l'autorisation de supprimer ce fichier.");
            }

            fileStorageService.delete(file.getPath());

            fileRepository.delete(file);

            return ResponseEntity.ok("Fichier supprimé avec succès.");

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du fichier {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur technique.");
        }
    }


}