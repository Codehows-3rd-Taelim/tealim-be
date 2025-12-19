package com.codehows.taelimbe.ai.entity;

import com.codehows.taelimbe.ai.constant.EmbedSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Embed {

    @Id
    private String embedKey;

    // TEXT → 답변/지식 원문
    // FILE → 파일 전체 텍스트 or 요약
    @Column(columnDefinition = "TEXT")
    private String embedValue;

    // TEXT / FILE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmbedSourceType sourceType;

    // FILE일 경우만 사용
    private String sourceFileId;
    private String sourceFileName;

    private Long sourceQuestionId;

    // 소프트 삭제용
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static Embed createText(
            String key,
            String value,
            Long sourceQuestionId
    ) {
        LocalDateTime now = LocalDateTime.now();
        return Embed.builder()
                .embedKey(key)
                .embedValue(value)
                .sourceType(EmbedSourceType.TEXT)
                .sourceQuestionId(sourceQuestionId) // null 가능
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Embed createFile(
            String key,
            String value,
            String fileId,
            String fileName,
            Long sourceQuestionId
    ) {
        LocalDateTime now = LocalDateTime.now();
        return Embed.builder()
                .embedKey(key)
                .embedValue(value)
                .sourceType(EmbedSourceType.FILE)
                .sourceFileId(fileId)
                .sourceFileName(fileName)
                .sourceQuestionId(sourceQuestionId) // null 가능
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }



    public void updateValue(String newValue) {
        this.embedValue = newValue;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
