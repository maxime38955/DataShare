package com.datashare.backend.controller;

import com.datashare.backend.model.FileEntity;
import com.datashare.backend.service.FileService;
import com.datashare.backend.service.FileStorageService;
import com.datashare.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false) // Désactive les filtres JWT pour le test unitaire
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FileService fileService;
    @MockitoBean private FileStorageService fileStorageService;
    @MockitoBean private JwtService jwtService;

    // ==========================================
    // TESTS POUR /files/upload
    // ==========================================

    @Test
    void shouldUploadFileSuccessfully() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "contenu".getBytes());
        FileEntity mockSavedFile = FileEntity.builder()
                .fileId(1L).name("test.pdf").token("uuid-123").build();

        Mockito.when(fileService.processUpload(any(), any(), anyInt(), any(), anyString()))
                .thenReturn(mockSavedFile);

        mockMvc.perform(multipart("/files/upload")
                        .file(mockFile)
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test.pdf"))
                .andExpect(jsonPath("$.token").value("uuid-123"));
    }

    @Test
    void shouldReturnBadRequestWhenUploadFailsValidation() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.exe", "application/x-msdownload", "exe".getBytes());

        Mockito.when(fileService.processUpload(any(), any(), anyInt(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("L'extension du fichier est interdite."));

        mockMvc.perform(multipart("/files/upload")
                        .file(mockFile)
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("L'extension du fichier est interdite."));
    }

    @Test
    void shouldReturnInternalServerErrorWhenUploadCrashes() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "contenu".getBytes());

        Mockito.when(fileService.processUpload(any(), any(), anyInt(), any(), anyString()))
                .thenThrow(new RuntimeException("Crash serveur"));

        mockMvc.perform(multipart("/files/upload")
                        .file(mockFile)
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Une erreur est survenue lors de l'upload."));
    }

    // ==========================================
    // TESTS POUR /files/user/files
    // ==========================================

    @Test
    void shouldReturnUserFilesHistory() throws Exception {
        FileEntity file1 = FileEntity.builder().name("photo.jpg").build();
        FileEntity file2 = FileEntity.builder().name("facture.pdf").build();

        Mockito.when(fileService.getUserHistory("test@mail.com")).thenReturn(List.of(file1, file2));

        mockMvc.perform(get("/files/user/files")
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("photo.jpg"));
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingHistoryWithoutLogin() throws Exception {
        mockMvc.perform(get("/files/user/files")) // Pas de principal injecté
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Accès refusé. Connectez-vous."));
    }

    @Test
    void shouldReturnInternalServerErrorWhenHistoryCrashes() throws Exception {
        Mockito.when(fileService.getUserHistory("test@mail.com")).thenThrow(new RuntimeException("Erreur BDD"));

        mockMvc.perform(get("/files/user/files")
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Impossible de récupérer l'historique."));
    }

    // ==========================================
    // TESTS POUR /files/metadata/{token}
    // ==========================================

    @Test
    void shouldReturnMetadataWhenTokenIsValid() throws Exception {
        FileEntity mockFile = FileEntity.builder().name("test.pdf").size(1024L).mimeType("application/pdf").build();
        Mockito.when(fileService.getFileByToken("abc-123")).thenReturn(mockFile);

        mockMvc.perform(get("/files/metadata/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.pdf"))
                .andExpect(jsonPath("$.size").value(1024));
    }

    @Test
    void shouldReturn404WhenTokenIsInvalidForMetadata() throws Exception {
        Mockito.when(fileService.getFileByToken("faux-token"))
                .thenThrow(new NoSuchElementException("Lien invalide ou expiré."));

        mockMvc.perform(get("/files/metadata/faux-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Lien invalide ou expiré."));
    }

    // ==========================================
    // TESTS POUR /files/download/{token}
    // ==========================================

    @Test
    void shouldDownloadFileSuccessfully() throws Exception {
        FileEntity mockFile = FileEntity.builder().name("doc.pdf").mimeType("application/pdf").path("/tmp/doc.pdf").build();
        ByteArrayResource mockResource = new ByteArrayResource("contenu_binaire".getBytes());

        Mockito.when(fileService.getFileByToken("token-xyz")).thenReturn(mockFile);
        Mockito.when(fileStorageService.load("/tmp/doc.pdf")).thenReturn(mockResource);

        mockMvc.perform(get("/files/download/token-xyz"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"doc.pdf\""))
                .andExpect(content().bytes("contenu_binaire".getBytes()));
    }

    @Test
    void shouldReturnNotFoundWhenDownloadingWithInvalidToken() throws Exception {
        Mockito.when(fileService.getFileByToken("faux-token"))
                .thenThrow(new NoSuchElementException("Lien invalide ou expiré."));

        mockMvc.perform(get("/files/download/faux-token"))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // TESTS POUR /files/user/{fileId} (DELETE)
    // ==========================================

    @Test
    void shouldDeleteFileSuccessfully() throws Exception {
        // Le service ne renvoie rien (void), on simule juste que tout se passe bien.
        Mockito.doNothing().when(fileService).deleteSecuredFile(1L, "test@mail.com");

        mockMvc.perform(delete("/files/user/1")
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Fichier supprimé avec succès."));
    }

    @Test
    void shouldReturnUnauthorizedWhenDeletingWithoutLogin() throws Exception {
        mockMvc.perform(delete("/files/user/1")) // Pas de principal
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Accès refusé."));
    }

    @Test
    void shouldReturnForbiddenWhenDeletingOthersFile() throws Exception {
        Mockito.doThrow(new SecurityException("Vous n'avez pas l'autorisation."))
                .when(fileService).deleteSecuredFile(1L, "hacker@mail.com");

        mockMvc.perform(delete("/files/user/1")
                        .principal(() -> "hacker@mail.com"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Vous n'avez pas l'autorisation."));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingInvalidFile() throws Exception {
        Mockito.doThrow(new NoSuchElementException("Fichier introuvable"))
                .when(fileService).deleteSecuredFile(99L, "test@mail.com");

        mockMvc.perform(delete("/files/user/99")
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Fichier introuvable"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenDeletionCrashes() throws Exception {
        Mockito.doThrow(new RuntimeException("Crash disque dur"))
                .when(fileService).deleteSecuredFile(1L, "test@mail.com");

        mockMvc.perform(delete("/files/user/1")
                        .principal(() -> "test@mail.com"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur technique."));
    }
}