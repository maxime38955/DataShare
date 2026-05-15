package com.datashare.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class FileResponseDTO {
    private Long fileId;
    private String name;
    private long size;
    private String mimeType;
    private String token;
    private LocalDateTime uploadDate;
    private LocalDateTime expirationDate;
    private Set<String> tags;
}