package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.entity.Question;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class QuestionDTO {

    private final Long QuestionId;
    private final String userQuestionText;
    private final String normalizedText;
    private final boolean resolved;
    private final LocalDateTime createdAt;

    public QuestionDTO(Question q) {
        this.QuestionId = q.getQuestionId();
        this.userQuestionText = q.getUserQuestionText();
        this.normalizedText = q.getNormalizedText();
        this.resolved = q.isResolved();
        this.createdAt = q.getCreatedAt();
    }
}