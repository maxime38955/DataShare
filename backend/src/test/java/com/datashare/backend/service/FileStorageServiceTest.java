package com.datashare.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    // On instancie ton vrai service
    private final FileStorageService storageService = new FileStorageService();

    // Variable pour mémoriser le fichier créé et pouvoir le supprimer après
    private String lastCreatedFilePath;

    @AfterEach
    void tearDown() {
        // NETTOYAGE : S'exécute automatiquement après chaque test
        // Pour éviter de remplir ton disque dur avec des faux fichiers
        if (lastCreatedFilePath != null) {
            storageService.delete(lastCreatedFilePath);
        }
    }

    @Test
    void shouldStoreAndLoadFileSuccessfully() throws Exception {
        // 1. Arrange : Création d'un faux fichier
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document-test.pdf",
                "application/pdf",
                "Contenu binaire du faux PDF".getBytes()
        );
        String token = "uuid-test-123";

        // 2. Act : On teste la sauvegarde (store)
        lastCreatedFilePath = storageService.store(file, token);

        // 3. Assert : Vérifications de la sauvegarde
        assertNotNull(lastCreatedFilePath, "Le chemin ne doit pas être nul");
        assertTrue(lastCreatedFilePath.endsWith("uuid-test-123.pdf"), "L'extension doit être conservée");
        assertTrue(Files.exists(Paths.get(lastCreatedFilePath)), "Le fichier physique doit exister sur le disque");

        // 4. Act : On teste le chargement (load)
        Resource resource = storageService.load(lastCreatedFilePath);

        // 5. Assert : Vérifications du chargement
        assertNotNull(resource);
        assertTrue(resource.exists(), "La ressource Spring doit exister");
        assertTrue(resource.isReadable(), "La ressource doit être lisible");
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        // 1. Arrange : Un fichier avec 0 octet
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vide.txt",
                "text/plain",
                new byte[0]
        );

        // 2 & 3. Act & Assert : On vérifie que ton code lève bien l'erreur prévue
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            storageService.store(emptyFile, "token-vide");
        });

        assertEquals("Impossible de stocker un fichier vide.", exception.getMessage());
    }

    @Test
    void shouldDeleteFileSuccessfully() {
        // 1. Arrange : Créer un fichier exprès pour le supprimer
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "a-supprimer.txt",
                "text/plain",
                "A effacer".getBytes()
        );
        String pathToDelete = storageService.store(file, "token-delete");

        // On s'assure qu'il est bien là
        assertTrue(Files.exists(Paths.get(pathToDelete)));

        // 2. Act : On teste ta méthode delete
        storageService.delete(pathToDelete);

        // 3. Assert : On vérifie qu'il a bien disparu du disque dur
        assertFalse(Files.exists(Paths.get(pathToDelete)), "Le fichier physique aurait dû être supprimé");
    }
}