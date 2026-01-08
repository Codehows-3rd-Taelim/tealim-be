package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.entity.Embed;
import com.codehows.taelimbe.ai.entity.Qna;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.ai.repository.QnaRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
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

        qna.markDeleted(); // deletedAt = now()

        qnaRepository.delete(qna);
    }

    @Transactional
    public void questionEmbedDelete(Long qnaId) {
        Qna qna = get(qnaId);

        Embed embed = embedRepository.findByQnaId(qnaId).orElse(null);

        if (embed != null) {
            boolean deleted =
                    embeddingService.deleteEmbeddingByKey(embed.getEmbedKey());

            if (!deleted) {
                throw new IllegalStateException(
                        "Milvus delete failed. Abort questionDelete. qnaId=" + qnaId
                );
            }

            embedRepository.delete(embed);
        }

        qnaRepository.delete(qna);
    }


    public Qna get(Long qnaId) {
        return qnaRepository.findById(qnaId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Qna not found")
                );
    }



    public List<Qna> findAll(UserPrincipal user) {
        if (user.isAdmin()) {
            return qnaRepository.findAll();
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






}
