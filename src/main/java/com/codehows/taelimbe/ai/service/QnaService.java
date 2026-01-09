package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.entity.Embed;
import com.codehows.taelimbe.ai.entity.Qna;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.ai.repository.QnaRepository;
import com.codehows.taelimbe.user.entity.User;
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
        return qnaRepository.findByUserId(user.userId());
    }

    public List<Qna> findByResolved(boolean resolved, UserPrincipal user) {
        if (user.isAdmin()) {
            return qnaRepository.findByResolved(resolved);
        }
        return qnaRepository.findByUserIdAndResolved(
                user.userId(), resolved
        );
    }

    public List<Qna> findByStatus(QnaStatus status, UserPrincipal user) {
        if (user.isAdmin()) {
            return qnaRepository.findByStatus(status);
        }
        return qnaRepository.findByUserIdAndStatus(
                user.userId(), status
        );
    }



    private String normalize(String text) {
        return text.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    // 미답 질문 기록
    public void recordQuestion(String rawQuestion, User user) {
        String normalized = normalize(rawQuestion);

        qnaRepository.findByNormalizedText(normalized)
                .orElseGet(() ->
                        qnaRepository.save(
                                Qna.create(rawQuestion, normalized, user)
                        )
                );
    }
}
