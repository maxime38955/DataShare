package com.datashare.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageService {

    // Dossier où seront stockés physiquement les fichiers sur le serveur
    private final Path rootLocation = Paths.get("uploads-dir");

    public FileStorageService() {
        try {
            // Crée le dossier "uploads-dir" à la racine du projet s'il n'existe pas
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'initialiser le dossier de stockage", e);
        }
    }

    public String store(MultipartFile file, String uniqueToken) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Impossible de stocker un fichier vide.");
            }

            // On extrait l'extension d'origine (ex: .pdf)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // On nomme le fichier physique avec le token UUID pour garantir l'unicité
            String storageFilename = uniqueToken + extension;
            Path destinationFile = this.rootLocation.resolve(Paths.get(storageFilename))
                    .normalize().toAbsolutePath();

            // Écriture du fichier sur le disque
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            return destinationFile.toString(); // On retourne le chemin absolu pour l'enregistrer en BDD
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'enregistrement du fichier physique", e);
        }
    }
}