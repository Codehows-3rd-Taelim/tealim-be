package com.codehows.taelimbe.ai.embedding.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "qna_embedding_cleanup")
@Getter
@NoArgsConstructor
public class QnaEmbeddingCleanup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long qnaId;

    private String embedKey;

    public QnaEmbeddingCleanup(Long qnaId, String embedKey) {
        this.qnaId = qnaId;
        this.embedKey = embedKey;
    }
}
