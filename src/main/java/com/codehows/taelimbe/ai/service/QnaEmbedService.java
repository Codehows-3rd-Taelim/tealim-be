package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.entity.Embed;
import com.codehows.taelimbe.qna.entity.Qna;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.qna.repository.QnaRepository;
import com.codehows.taelimbe.qna.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QnaEmbedService {

    private final EmbeddingService embeddingService;
    private final EmbedRepository embedRepository;
    private final QnaEmbeddingFailService qnaEmbeddingFailService;
    private final QnaService qnaService;



    @Transactional
    public void apply(Long qnaId, String answer) {
        Qna qna = qnaService.get(qnaId);

        qna.requestApply(answer);

        try {
            embeddingService.replaceQnaEmbedding(qnaId, answer);
            qna.applySuccess();
        } catch (Exception e) {
            qnaEmbeddingFailService.markFail(qnaId, answer);
            throw e;
        }
    }


    // 챗봇 답변 철회
    @Transactional
    public void deleteAppliedAnswer(Long qnaId) {
        Qna qna = qnaService.get(qnaId);

        // 1. appliedAnswer 삭제
        qna.deleteAppliedAnswer();

        // 2. Embed 조회
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
}











