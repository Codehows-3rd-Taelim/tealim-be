package com.codehows.taelimbe.qna.service;


import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.qna.constant.QnaViewType;
import com.codehows.taelimbe.qna.entity.Qna;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.qna.repository.QnaRepository;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import com.codehows.taelimbe.ai.service.QnaEmbeddingFailService;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.security.UserPrincipal;
import com.codehows.taelimbe.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final UserService userService;

    public Page<Qna> findByView(QnaViewType viewType, UserPrincipal user, Pageable pageable) {
        boolean isAdmin = user.isAdmin();

        return switch (viewType) {

            case ALL -> isAdmin
                    ? qnaRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageable)
                    : qnaRepository
                    .findByUser_UserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                            user.userId(), pageable
                    );

            case UNRESOLVED -> isAdmin
                    ? qnaRepository
                    .findByResolvedAndDeletedAtIsNullOrderByCreatedAtDesc(
                            false, pageable
                    )
                    : qnaRepository
                    .findByUser_UserIdAndResolvedAndDeletedAtIsNullOrderByCreatedAtDesc(
                            user.userId(), false, pageable
                    );

            case RESOLVED -> isAdmin
                    ? qnaRepository
                    .findByResolvedAndDeletedAtIsNullOrderByCreatedAtDesc(
                            true, pageable
                    )
                    : qnaRepository
                    .findByUser_UserIdAndResolvedAndDeletedAtIsNullOrderByCreatedAtDesc(
                            user.userId(), true, pageable
                    );



            case INACTIVE -> {
                if (!isAdmin) {
                    throw new SecurityException("Admin only");
                }
                yield qnaRepository
                        .findByDeletedAtIsNotNullOrderByDeletedAtDesc(pageable);
            }
        };
    }





    @Transactional
    public void questionDelete(Long qnaId) {
        Qna qna = get(qnaId);

        // 이미 삭제된 경우
        if (qna.getDeletedAt() != null) {
            return;
        }

        // 챗봇 답변 적용된 (APPLIED) 질문은 삭제 차단
        if (qna.getStatus() == QnaStatus.APPLIED) {
            throw new IllegalStateException("Applied Qna cannot be deleted");
        }


        // 소프트 삭제
        qna.markDeleted();
    }


    public Qna get(Long qnaId) {
        return qnaRepository.findByIdAndDeletedAtIsNull(qnaId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Active Qna not found")
                );
    }




    @Transactional
    public void saveDisplayAnswer(Long qnaId, String answer) {
        Qna qna = get(qnaId);
        qna.updateDisplayAnswer(answer);
    }

    @Transactional
    public void updateDisplayAnswer(Long qnaId, String answer) {
        Qna qna = get(qnaId);
        qna.updateDisplayAnswer(answer);
    }

    @Transactional
    public void deleteDisplayAnswer(Long qnaId) {
        Qna qna = get(qnaId);
        qna.clearDisplayAnswer();
    }


    // 질문 생성
    public Long createQuestion(String title, String questionText, Long userId) {
        User user = userService.getUser(userId);
        Qna qna = Qna.create(title, questionText, user);
        qnaRepository.save(qna);
        return qna.getId();
    }



    // 비활성 질문 완전 삭제
    @Transactional
    public void hardDelete(Long qnaId) {
        Qna qna = getIncludingDeleted(qnaId);

        if (qna.getDeletedAt() == null) {
            throw new IllegalStateException("Active QnA cannot be hard deleted");
        }

        qnaRepository.delete(qna);
    }



    public Qna getIncludingDeleted(Long qnaId) {
        return qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));
    }

    @Transactional
    public void restore(Long qnaId) {
        Qna qna = getIncludingDeleted(qnaId);

        if (qna.getDeletedAt() == null) {
            return; // 이미 활성 상태면 무시
        }

        qna.restore();
    }

    @Transactional
    public void updateQuestionByUser(
            Long qnaId,
            String title,
            String newQuestionText,
            Long userId
    ) {
        Qna qna = qnaRepository
                .findByIdAndDeletedAtIsNull(qnaId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Qna not found")
                );

        // 본인 질문인지 검증
        if (!qna.getUser().getUserId().equals(userId)) {
            throw new SecurityException("Not your question");
        }

        //  displayAnswer 있으면 수정 불가
        if (qna.getDisplayAnswer() != null) {
            throw new IllegalStateException(
                    "Answered Qna cannot be edited"
            );
        }

        qna.updateQuestion(title, newQuestionText);
    }

    @Transactional
    public void deleteQuestionByUser(Long qnaId, Long userId) {
        Qna qna = qnaRepository
                .findByIdAndDeletedAtIsNull(qnaId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Qna not found")
                );

        // 본인 질문인지 검증
        if (!qna.getUser().getUserId().equals(userId)) {
            throw new SecurityException("Not your question");
        }

        //  displayAnswer 있으면 삭제 불가
        if (qna.getDisplayAnswer() != null) {
            throw new IllegalStateException(
                    "Answered Qna cannot be deleted"
            );
        }

        qna.markDeleted();
    }

}











