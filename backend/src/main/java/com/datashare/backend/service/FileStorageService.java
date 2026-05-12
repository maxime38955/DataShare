package com.datashare.backend.service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageService {


    private final Path rootLocation = Paths.get("uploads-dir");

    public FileStorageService() {
        try {

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


            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }


            String storageFilename = uniqueToken + extension;
            Path destinationFile = this.rootLocation.resolve(Paths.get(storageFilename))
                    .normalize().toAbsolutePath();


            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            return destinationFile.toString();
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'enregistrement du fichier physique", e);
        }
    }

    public Resource load(String pathString) {
        try {
            Path file = Paths.get(pathString);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Impossible de lire le fichier");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur de chemin : " + e.getMessage());
        }
    }

    public void delete(String pathString) {
        try {
            Files.deleteIfExists(Paths.get(pathString));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la suppression physique : " + e.getMessage());
        }
    }

}