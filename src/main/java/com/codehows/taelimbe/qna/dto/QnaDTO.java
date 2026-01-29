package com.codehows.taelimbe.qna.dto;

import com.codehows.taelimbe.ai.embedding.constant.QnaStatus;
import com.codehows.taelimbe.qna.entity.Qna;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QnaDTO {

    private Long id;

    private String title;
    private String questionText;

    private String appliedAnswer;
    private String displayAnswer;
    private String editingAnswer;

    private QnaStatus status;
    private boolean resolved;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public QnaDTO(Qna qna) {
        this.id = qna.getId();

        this.title = qna.getTitle();
        this.questionText = qna.getQuestionText();

        this.appliedAnswer = qna.getAppliedAnswer();
        this.displayAnswer = qna.getDisplayAnswer();
        this.editingAnswer = qna.getEditingAnswer();

        this.status = qna.getStatus();
        this.resolved = qna.isResolved();

        this.createdAt = qna.getCreatedAt();
        this.updatedAt = qna.getUpdatedAt();
        this.deletedAt = qna.getDeletedAt();
    }
}
