package com.codehows.taelimbe.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Answer {

    @Id
    private Long questionId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answerText;

    // 임베딩 기준 원문 (Q + A)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String embedSourceText;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Answer create(Long questionId, String answerText, String embedSourceText) {
        Answer a = new Answer();
        a.questionId = questionId;
        a.answerText = answerText;
        a.embedSourceText = embedSourceText;
        a.createdAt = LocalDateTime.now();
        a.updatedAt = a.createdAt;
        return a;
    }

    public void update(String answerText, String embedSourceText) {
        this.answerText = answerText;
        this.embedSourceText = embedSourceText;
        this.updatedAt = LocalDateTime.now();
    }
}
