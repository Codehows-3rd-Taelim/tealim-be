package com.codehows.taelimbe.ai.embedding.dto;

import com.codehows.taelimbe.ai.embedding.constant.EmbedFileStatus;
import com.codehows.taelimbe.ai.embedding.entity.EmbedFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class EmbedFileDTO {

    private Long id;
    private String originalName;
    private String storedName;
    private String extension;
    private long fileSize;
    private EmbedFileStatus status;
    private String embedKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static EmbedFileDTO from(EmbedFile file) {
        return EmbedFileDTO.builder()
                .id(file.getId())
                .originalName(file.getOriginalName())
                .storedName(file.getStoredName())
                .extension(file.getExtension())
                .fileSize(file.getFileSize())
                .status(file.getStatus())
                .embedKey(file.getEmbedKey())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
