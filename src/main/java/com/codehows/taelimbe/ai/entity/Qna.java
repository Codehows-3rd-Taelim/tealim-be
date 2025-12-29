package com.codehows.taelimbe.ai.entity;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "qna")
public class Qna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;


    @Column(columnDefinition = "TEXT", nullable = false)
    private String normalizedText;

    @Column(columnDefinition = "TEXT")
    private String appliedAnswer;

    @Column(columnDefinition = "TEXT")
    private String editingAnswer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QnaStatus status; // nullable

    @Column(nullable = false)
    private boolean resolved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;



    public static Qna create(String questionText, String normalizedText) {
        Qna q = new Qna();
        q.questionText = questionText;
        q.normalizedText = normalizedText;
        q.resolved = false;
        q.status = null;
        q.createdAt = LocalDateTime.now();
        q.updatedAt = q.createdAt;
        return q;
    }



    // 임베딩 요청
    public void requestApply(String answer) {
        this.editingAnswer = answer;
        this.status = QnaStatus.FAILED;
        touch();
    }

    // QnA 적용 성공
    public void applySuccess() {
        this.appliedAnswer = this.editingAnswer;
        this.editingAnswer = null;
        this.status = QnaStatus.APPLIED;
        this.resolved = true;
        touch();
    }

    // QnA 적용 실패
    public void applyFail() {
        this.status = QnaStatus.FAILED;
        touch();
    }


    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
