package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.entity.Qna;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Setter
@RequiredArgsConstructor
@Getter
public class QnaDTO {

    private Long id;

    // Question
    private String questionText;

    // Answer
    private String appliedAnswer;
    private String editingAnswer;

    // States
    private QnaStatus status;
    private boolean resolved;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public QnaDTO(Qna qna) {
        this.id = qna.getId();
        this.questionText = qna.getQuestionText();
        this.appliedAnswer = qna.getAppliedAnswer();
        this.editingAnswer = qna.getEditingAnswer();
        this.status = qna.getStatus();
        this.resolved = qna.isResolved();
        this.createdAt = qna.getCreatedAt();
        this.updatedAt = qna.getUpdatedAt();
    }
}
