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

    @Column(columnDefinition = "TEXT")
    private String embedValue;

    @Enumerated(EnumType.STRING)
    private EmbedSourceType sourceType;

    private Long sourceQuestionId;
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Embed createText(String key, String value, Long questionId) {
        LocalDateTime now = LocalDateTime.now();
        return Embed.builder()
                .embedKey(key)
                .embedValue(value)
                .sourceType(EmbedSourceType.TEXT)
                .sourceQuestionId(questionId)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
