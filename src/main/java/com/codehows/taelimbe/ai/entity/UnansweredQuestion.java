package com.codehows.taelimbe.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class UnansweredQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long unansweredQuestionId;

    // 사용자 원문 질문
    @Column(columnDefinition = "TEXT")
    private String userQuestionText;

    // 문자열 중복 제거용
    private String normalizedText;

    // 동일 문자열 반복 횟수
    private int count;

    // < NEW / RESOLVED >
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime lastAskedAt;



    public static UnansweredQuestion create(String questionText, String normalizedText) {
        UnansweredQuestion q = new UnansweredQuestion();
        q.userQuestionText = questionText;
        q.normalizedText = normalizedText;
        q.count = 1;
        q.status = "NEW";
        q.createdAt = LocalDateTime.now();
        q.lastAskedAt = q.createdAt;
        return q;
    }

    public void increaseCount() {
        this.count++;
        this.lastAskedAt = LocalDateTime.now();
    }

}