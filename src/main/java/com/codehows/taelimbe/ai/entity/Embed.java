package com.codehows.taelimbe.ai.entity;

import com.codehows.taelimbe.ai.constant.EmbedSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "embed")
public class Embed {

    @Id
    private String embedKey;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String embedValue;

    @Enumerated(EnumType.STRING)
    private EmbedSourceType sourceType; // TEXT

    // QnA 임베딩일 때만 값 있음
    private Long qnaId;

    private LocalDateTime createdAt;

    public static Embed createText(String key, String value, Long qnaId) {
        Embed e = new Embed();
        e.embedKey = key;
        e.embedValue = value;
        e.sourceType = EmbedSourceType.TEXT;
        e.qnaId = qnaId;
        e.createdAt = LocalDateTime.now();
        return e;
    }



}

