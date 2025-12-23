package com.codehows.taelimbe.ai.entity;

import com.codehows.taelimbe.ai.dto.QuestionDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String userQuestionText;

    @Column(columnDefinition = "TEXT")
    private String normalizedText;

    // false = 미응답, true = 답변 완료
    private boolean resolved;

    private LocalDateTime createdAt;

    public static Question create(String questionText, String normalizedText) {
        Question q = new Question();
        q.userQuestionText = questionText;
        q.normalizedText = normalizedText;
        q.resolved = false;
        q.createdAt = LocalDateTime.now();
        return q;
    }


    public void resolve() {
        this.resolved = true;
    }


}
