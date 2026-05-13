package com.datashare.backend.controller;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;
    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "expirationDays", defaultValue = "7") int expirationDays,
            @RequestParam(value = "tags", required = false) Set<String> tags,
            Principal principal) {

        try {
            String email = (principal != null) ? principal.getName() : null;

            // On délègue tout au FileService !
            FileEntity savedFile = fileService.processUpload(file, password, expirationDays, tags, email);

            // Préparation de la réponse propre
            Map<String, Object> response = new HashMap<>();
            response.put("fileId", savedFile.getFileId());
            response.put("name", savedFile.getName());
            response.put("size", savedFile.getSize());
            response.put("mimeType", savedFile.getMimeType());
            response.put("token", savedFile.getToken());
            response.put("expirationDate", savedFile.getExpirationDate());
            response.put("tags", savedFile.getTags());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

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
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'historique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Impossible de récupérer l'historique.");
        }
    }

    @GetMapping("/metadata/{token}")
    public ResponseEntity<?> getMetadata(@PathVariable String token) {
        try {
            FileEntity file = fileService.getFileByToken(token);

            Map<String, Object> meta = new HashMap<>();
            meta.put("name", file.getName());
            meta.put("size", file.getSize());
            meta.put("mimeType", file.getMimeType());
            meta.put("expirationDate", file.getExpirationDate());
            meta.put("path", file.getPath());
            meta.put("token", file.getToken());
            meta.put("isActive", file.getIsActive());
            meta.put("uploadDate", file.getUploadDate());
          


            return ResponseEntity.ok(meta);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String token) {
        try {
            FileEntity fileEntity = fileService.getFileByToken(token);
            Resource resource = fileStorageService.load(fileEntity.getPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getName() + "\"")
                    .body(resource);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
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
}