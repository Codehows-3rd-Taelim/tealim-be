package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.constant.EmbedFileStatus;
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
}
