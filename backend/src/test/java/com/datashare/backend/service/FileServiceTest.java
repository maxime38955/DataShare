package com.datashare.backend.service;

import com.datashare.backend.model.FileEntity;
import com.datashare.backend.model.User;
import com.datashare.backend.repository.FileRepository;
import com.datashare.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private FileRepository fileRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private FileService fileService;

    // ==========================================
    // TESTS POUR processUpload
    // ==========================================

    @Test
    void shouldUploadFileSuccessfullyWithConnectedUser() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "contenu".getBytes());
        User uploader = User.builder().email("test@mail.com").build();
        Set<String> tags = new HashSet<>(Arrays.asList("travail", "urgent"));

        Mockito.when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(uploader));
        Mockito.when(fileStorageService.store(any(), anyString())).thenReturn("/chemin/physique.pdf");

        FileEntity mockSavedFile = FileEntity.builder().name("document.pdf").build();
        Mockito.when(fileRepository.save(any(FileEntity.class))).thenReturn(mockSavedFile);

        // Act
        FileEntity result = fileService.processUpload(file, "secret123", 7, tags, "test@mail.com");

        // Assert
        assertNotNull(result);
        assertEquals("document.pdf", result.getName());

        // Vérification avancée : On capture l'objet sauvegardé pour vérifier ses champs
        ArgumentCaptor<FileEntity> fileCaptor = ArgumentCaptor.forClass(FileEntity.class);
        Mockito.verify(fileRepository).save(fileCaptor.capture());
        FileEntity capturedFile = fileCaptor.getValue();

        assertEquals("document.pdf", capturedFile.getName());
        assertEquals("secret123", capturedFile.getPassword());
        assertEquals(uploader, capturedFile.getUser());
        assertEquals(2, capturedFile.getTags().size());
    }

    @Test
    void shouldUploadFileSuccessfullyWithoutConnectedUser() {
        // Arrange (Upload Anonyme : userEmail est null)
        MockMultipartFile file = new MockMultipartFile("file", "anonyme.png", "image/png", "img".getBytes());

        Mockito.when(fileStorageService.store(any(), anyString())).thenReturn("/chemin/anonyme.png");
        Mockito.when(fileRepository.save(any(FileEntity.class))).thenReturn(new FileEntity());

        // Act
        fileService.processUpload(file, null, 3, null, null);

        // Assert
        ArgumentCaptor<FileEntity> fileCaptor = ArgumentCaptor.forClass(FileEntity.class);
        Mockito.verify(fileRepository).save(fileCaptor.capture());
        assertNull(fileCaptor.getValue().getUser()); // On vérifie que l'utilisateur est bien nul
        assertNull(fileCaptor.getValue().getPassword());
    }

    @Test
    void shouldThrowExceptionWhenFileIsTooLarge() {
        // Arrange
        MockMultipartFile bigFile = Mockito.mock(MockMultipartFile.class);
        Mockito.when(bigFile.getSize()).thenReturn(1073741825L); // 1 Go + 1 octet

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.processUpload(bigFile, null, 7, null, "test@test.fr");
        });
        assertEquals("Le fichier dépasse la taille maximale autorisée de 1 Go.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenFileIsExecutable() {
        // Arrange
        MockMultipartFile exeFile = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", new byte[10]);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.processUpload(exeFile, null, 7, null, "test@test.fr");
        });
        assertEquals("L'extension du fichier est interdite pour des raisons de sécurité.", exception.getMessage());
    }

    // ==========================================
    // TESTS POUR getUserHistory
    // ==========================================

    @Test
    void shouldReturnUserHistorySuccessfully() {
        // Arrange
        User user = User.builder().email("test@mail.com").build();
        List<FileEntity> files = Arrays.asList(new FileEntity(), new FileEntity());

        Mockito.when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        Mockito.when(fileRepository.findByUser(user)).thenReturn(files);

        // Act
        List<FileEntity> result = fileService.getUserHistory("test@mail.com");

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void shouldThrowExceptionWhenUserHistoryNotFound() {
        // Arrange
        Mockito.when(userRepository.findByEmail("inconnu@mail.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            fileService.getUserHistory("inconnu@mail.com");
        });
    }

    // ==========================================
    // TESTS POUR getFileByToken
    // ==========================================

    @Test
    void shouldReturnFileByTokenSuccessfully() {
        // Arrange
        FileEntity file = FileEntity.builder().token("token123").build();
        Mockito.when(fileRepository.findByToken("token123")).thenReturn(Optional.of(file));

        // Act
        FileEntity result = fileService.getFileByToken("token123");

        // Assert
        assertNotNull(result);
        assertEquals("token123", result.getToken());
    }

    @Test
    void shouldThrowExceptionWhenTokenIsInvalid() {
        // Arrange
        Mockito.when(fileRepository.findByToken("faux-token")).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            fileService.getFileByToken("faux-token");
        });
        assertEquals("Lien invalide ou expiré.", exception.getMessage());
    }

    // ==========================================
    // TESTS POUR deleteSecuredFile
    // ==========================================



    @Test
    void shouldThrowExceptionWhenDeletingNonExistentFile() {
        // Arrange
        Mockito.when(fileRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            fileService.deleteSecuredFile(99L, "proprio@mail.com");
        });
        assertEquals("Fichier introuvable", exception.getMessage());
    }

    @Test
    void shouldThrowSecurityExceptionWhenDeletingOthersFile() {
        // Arrange
        User trueOwner = User.builder().email("proprio@mail.com").build();
        FileEntity file = FileEntity.builder().fileId(1L).user(trueOwner).build();

        Mockito.when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            fileService.deleteSecuredFile(1L, "hacker@mail.com");
        });
        assertEquals("Vous n'avez pas l'autorisation de supprimer ce fichier.", exception.getMessage());
        Mockito.verify(fileStorageService, Mockito.never()).delete(anyString());
    }

    @Test
    void shouldThrowSecurityExceptionWhenDeletingAnonymousFile() {
        // Arrange (Le fichier n'a pas de propriétaire)
        FileEntity anonymousFile = FileEntity.builder().fileId(1L).user(null).build();
        Mockito.when(fileRepository.findById(1L)).thenReturn(Optional.of(anonymousFile));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            fileService.deleteSecuredFile(1L, "quelquun@mail.com");
        });
        assertEquals("Vous n'avez pas l'autorisation de supprimer ce fichier.", exception.getMessage());
    }

    @Test
    void shouldDeleteFileWhenUserIsOwner() {
        // Arrange
        User owner = User.builder().email("proprio@mail.com").build();
        FileEntity file = FileEntity.builder().fileId(1L).path("/tmp/test.txt").user(owner).isActive(true).build();

        Mockito.when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

        // Act
        fileService.deleteSecuredFile(1L, "proprio@mail.com");

        // Assert : On vérifie que la sauvegarde est appelée (Soft Delete)
        ArgumentCaptor<FileEntity> fileCaptor = ArgumentCaptor.forClass(FileEntity.class);
        Mockito.verify(fileRepository).save(fileCaptor.capture());

        // On vérifie que le fichier est bien passé en inactif
        assertFalse(fileCaptor.getValue().getIsActive());

        // On s'assure que le "vrai" delete n'est jamais appelé
        Mockito.verify(fileRepository, Mockito.never()).delete(Mockito.any());
    }


}