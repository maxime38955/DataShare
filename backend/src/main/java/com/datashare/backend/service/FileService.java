package com.datashare.backend.service;

import com.datashare.backend.model.FileEntity;
import com.datashare.backend.model.User;
import com.datashare.backend.repository.FileRepository;
import com.datashare.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public FileEntity processUpload(MultipartFile file, String password, int expirationDays, Set<String> tags, String userEmail) {
        // 1. Validations métier
        long maxSizeBytes = 1024L * 1024L * 1024L;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale autorisée de 1 Go.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && (originalFilename.endsWith(".exe") || originalFilename.endsWith(".bat"))) {
            throw new IllegalArgumentException("L'extension du fichier est interdite pour des raisons de sécurité.");
        }

        // 2. Identification de l'utilisateur (si connecté)
        User uploader = null;
        if (userEmail != null) {
            uploader = userRepository.findByEmail(userEmail).orElse(null);
        }

        // 3. Sauvegarde physique
        String token = UUID.randomUUID().toString();
        String physicalPath = fileStorageService.store(file, token);

        // 4. Création et sauvegarde en base de données
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

        return fileRepository.save(fileEntity);
    }

    public List<FileEntity> getUserHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return fileRepository.findByUser(user);
    }

    public FileEntity getFileByToken(String token) {
        return fileRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Lien invalide ou expiré."));
    }

    public void deleteSecuredFile(Long fileId, String userEmail) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("Fichier introuvable"));

        // Vérification de sécurité CRUCIALE
        if (file.getUser() == null || !file.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("Vous n'avez pas l'autorisation de supprimer ce fichier.");
        }


        file.setIsActive(false);
        fileRepository.save(file);


        fileStorageService.delete(file.getPath());

    }
}