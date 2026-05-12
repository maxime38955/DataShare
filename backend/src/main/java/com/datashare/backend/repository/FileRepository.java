package com.datashare.backend.repository;

import com.datashare.backend.model.FileEntity;
import com.datashare.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByToken(String token);

    List<FileEntity> findByUser(User user);

}