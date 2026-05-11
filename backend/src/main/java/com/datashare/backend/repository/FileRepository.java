package com.datashare.backend.repository;

import com.datashare.backend.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    // Permettra de retrouver un fichier à partir de son token d'URL (US02)
    Optional<FileEntity> findByToken(String token);
}