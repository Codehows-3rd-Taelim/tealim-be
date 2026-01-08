package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.entity.Embed;
import com.codehows.taelimbe.ai.entity.Qna;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.ai.repository.QnaRepository;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
import com.codehows.taelimbe.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final EmbeddingService embeddingService;
    private final EmbedRepository embedRepository;
    private final QnaEmbeddingFailService qnaEmbeddingFailService;
    private final UserService userService;



    @Transactional
    public void apply(Long qnaId, String answer) {
        Qna qna = get(qnaId);

        qna.requestApply(answer);

        try {
            embeddingService.replaceQnaEmbedding(qnaId, answer);
            qna.applySuccess();
        } catch (Exception e) {
            qnaEmbeddingFailService.markFail(qnaId, answer);
            throw e;
        }
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

    // 챗봇 답변 철회
    @Transactional
    public void deleteAppliedAnswer(Long qnaId) {
        Qna qna = get(qnaId);

        // 1. appliedAnswer 삭제 (지식 철회)
        qna.deleteAppliedAnswer();

        // 2. Embed 조회 (단건)
        Embed embed = embedRepository.findByQnaId(qnaId).orElse(null);

        if (embed != null) {
            boolean deleted =
                    embeddingService.deleteEmbeddingByKey(embed.getEmbedKey());

            if (!deleted) {
                throw new IllegalStateException(
                        "Milvus delete failed. Abort embed delete. qnaId=" + qnaId
                );
            }

            // 3. Embed row 삭제
            embedRepository.delete(embed);
        }
    }


    public Qna get(Long qnaId) {
        return qnaRepository.findByIdAndDeletedAtIsNull(qnaId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Active Qna not found")
                );
    }


    public List<Qna> findAll(UserPrincipal user) {
        if (user.isAdmin()) {
            return qnaRepository.findByDeletedAtIsNull();
        }
        return qnaRepository.findByUser_UserIdAndDeletedAtIsNull(user.userId());
    }


    public List<Qna> findByResolved(boolean resolved, UserPrincipal user) {
        if (user.isAdmin()) {
            return qnaRepository.findByResolvedAndDeletedAtIsNull(resolved);
        }
        return qnaRepository.findByUser_UserIdAndResolvedAndDeletedAtIsNull(
                user.userId(), resolved
        );
    }

    public List<Qna> findByStatus(QnaStatus status, UserPrincipal user) {
        if (user.isAdmin()) {
            return qnaRepository.findByStatusAndDeletedAtIsNull(status);
        }
        return qnaRepository.findByUser_UserIdAndStatusAndDeletedAtIsNull(
                user.userId(), status
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
    @Transactional
    public Long createQuestion(String questionText, Long userId) {
        User user = userService.getUser(userId);

        Qna qna = Qna.create(questionText, user);
        qnaRepository.save(qna);

        return qna.getId();
    }












}
