package com.datashare.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService storageService;

    // 🌟 MAGIE JUNIT 5 : Crée un dossier temporaire et le détruit tout seul à la fin
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // On instancie le service avant chaque test
        storageService = new FileStorageService();

        // 🔧 On simule l'injection de Spring en forçant le chemin vers notre dossier temporaire.
        // ATTENTION : Remplace "uploadDir" par le nom exact de la variable dans ton FileStorageService
        // Exemple si ta vraie variable s'appelle "storageLocation" :
        // Remplace la ligne dans setUp() par :
        ReflectionTestUtils.setField(storageService, "rootLocation", Paths.get(tempDir.toString()));
    }

    // PLUS BESOIN de @AfterEach ou de "lastCreatedFilePath", @TempDir s'occupe de tout nettoyer !

    @Test
    void shouldStoreAndLoadFileSuccessfully() throws Exception {
        // 1. Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document-test.pdf",
                "application/pdf",
                "Contenu binaire du faux PDF".getBytes()
        );
        String token = "uuid-test-123";

        // 2. Act (Store)
        String savedPath = storageService.store(file, token);

        // 3. Assert (Store)
        assertNotNull(savedPath, "Le chemin ne doit pas être nul");
        assertTrue(savedPath.endsWith("uuid-test-123.pdf"), "L'extension doit être conservée");
        assertTrue(Files.exists(Path.of(savedPath)), "Le fichier physique doit exister sur le disque");

        // 4. Act (Load)
        Resource resource = storageService.load(savedPath);

        // 5. Assert (Load)
        assertNotNull(resource);
        assertTrue(resource.exists(), "La ressource Spring doit exister");
        assertTrue(resource.isReadable(), "La ressource doit être lisible");
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        // 1. Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vide.txt",
                "text/plain",
                new byte[0]
        );

        // 2 & 3. Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            storageService.store(emptyFile, "token-vide");
        });

        assertEquals("Impossible de stocker un fichier vide.", exception.getMessage());
    }

    @Test
    void shouldDeleteFileSuccessfully() {
        // 1. Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "a-supprimer.txt",
                "text/plain",
                "A effacer".getBytes()
        );
        String pathToDelete = storageService.store(file, "token-delete");
        assertTrue(Files.exists(Path.of(pathToDelete)));

        // 2. Act
        storageService.delete(pathToDelete);

        // 3. Assert
        assertFalse(Files.exists(Path.of(pathToDelete)), "Le fichier physique aurait dû être supprimé");
    }
}