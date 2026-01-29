package com.codehows.taelimbe.qna.entity;

import com.codehows.taelimbe.ai.embedding.constant.QnaStatus;
import com.codehows.taelimbe.user.entity.User;
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

    @Column(length = 50, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String appliedAnswer;

    @Column(columnDefinition = "TEXT")
    private String displayAnswer;

    @Column(columnDefinition = "TEXT")
    private String editingAnswer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QnaStatus status;

    @Column(nullable = false)
    private boolean resolved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;



    public static Qna create(String title, String questionText, User user) {
        Qna q = new Qna();
        q.title = title;
        q.questionText = questionText;
        q.resolved = false;
        q.status = null;
        q.createdAt = LocalDateTime.now();
        q.updatedAt = q.createdAt;
        q.user = user;
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
        touch();
    }

    // QnA 적용 실패
    public void applyFail(String answer) {
        this.status = QnaStatus.FAILED;
        this.editingAnswer = answer;
        touch();
    }


    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }


    public void updateDisplayAnswer(String answer) {
        this.displayAnswer = answer;
        this.resolved = (answer != null);
        touch();
    }

    public void clearDisplayAnswer() {
        this.displayAnswer = null;
        this.resolved = false;
        touch();
    }

    public void deleteAppliedAnswer() {
        this.appliedAnswer = null;
        this.status = null;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateQuestion(String title, String questionText) {
        this.title = title;
        this.questionText = questionText;
        touch();
    }

}
