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

    // 질문 원문
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    // 정규화된 질문 (중복 체크/검색용)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String normalizedText;

    // 챗봇에 적용된 답변 (APPLIED 상태)
    @Column(columnDefinition = "TEXT")
    private String appliedAnswer;

    // 관리자 수정 중 답변 (아직 적용 안 됨)
    @Column(columnDefinition = "TEXT")
    private String editingAnswer;



     // null : QnA 미사용(파일 처리 등)
     // EDITING / APPLIED / FAILED : QnA 파이프라인 사용 중
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QnaStatus status; // nullable

    // 운영 기준 종료 여부
    // true  : 더 이상 처리할 필요 없음
    // false : 아직 처리 대상
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
        q.status = null; // 처음엔 QnA 미사용 상태로 두고 시작 (필요하면 EDITING으로 올림)
        q.createdAt = LocalDateTime.now();
        q.updatedAt = q.createdAt;
        return q;
    }

    /* =========================
     * Domain methods
     * ========================= */

    // 관리자 답변 수정 (QnA 경로로 올림)
    public void updateEditingAnswer(String answer) {
        this.editingAnswer = answer;
        this.status = QnaStatus.EDITING;
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

    // 파일/정책 처리로 운영상 종료 (QnA 미사용)
    public void resolveWithoutQna() {
        this.resolved = true;
        this.status = null;        // QnA 미사용 의미 고정
        this.editingAnswer = null; // 혼동 방지(선택)
        touch();
    }

    // 운영상 종료 (QnA든 파일이든 상관 없이 “끝”)
    public void markResolved() {
        this.resolved = true;
        touch();
    }

    // 다시 처리 대상으로
    public void markUnresolved() {
        this.resolved = false;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
