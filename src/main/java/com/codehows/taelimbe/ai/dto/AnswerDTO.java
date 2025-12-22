package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.entity.Answer;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AnswerDTO {

    private final Long questionId;
    private final String answerText;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AnswerDTO(Answer answer) {
        this.questionId = answer.getQuestionId();
        this.answerText = answer.getAnswerText();
        this.createdAt = answer.getCreatedAt();
        this.updatedAt = answer.getUpdatedAt();
    }
}
