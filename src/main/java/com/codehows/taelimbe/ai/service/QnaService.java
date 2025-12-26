package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.entity.Qna;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.ai.repository.QnaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final EmbeddingService embeddingService;
    private final EmbedRepository embedRepository;

    public Qna createQuestion(String rawQuestion) {
        String normalized = normalize(rawQuestion);
        return qnaRepository.findByNormalizedText(normalized)
                .orElseGet(() ->
                        qnaRepository.save(Qna.create(rawQuestion, normalized))
                );
    }

    @Transactional
    public void updateEditingAnswer(Long qnaId, String answer) {
        Qna qna = get(qnaId);
        qna.updateEditingAnswer(answer);
    }

    @Transactional
    public void apply(Long qnaId, String answer) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Qna not found"));

        // 1️⃣ 최신 답변 반영
        qna.updateEditingAnswer(answer);

        try {
            // 2️⃣ 임베딩 교체 (모든 책임은 EmbeddingService)
            embeddingService.replaceQnaEmbedding(qnaId, answer);

            // 3️⃣ 성공 처리
            qna.applySuccess();

        } catch (Exception e) {
            qna.applyFail();
            throw e;
        }
    }



    @Transactional
    public void resolveWithoutQna(Long qnaId) {
        Qna qna = get(qnaId);

        embedRepository.findByQnaId(qnaId)
                .forEach(embed ->
                        embeddingService.deleteByKey(embed.getEmbedKey())
                );

        qna.resolveWithoutQna();
    }

    @Transactional
    public void delete(Long qnaId) {
        Qna qna = get(qnaId);

        embedRepository.findByQnaId(qnaId)
                .forEach(embed ->
                        embeddingService.deleteByKey(embed.getEmbedKey())
                );

        qnaRepository.delete(qna);
    }

    public Qna get(Long qnaId) {
        return qnaRepository.findById(qnaId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Qna not found")
                );
    }

    public List<Qna> findByResolved(boolean resolved) {
        return qnaRepository.findByResolved(resolved);
    }

    public List<Qna> findByStatus(QnaStatus status) {
        return qnaRepository.findByStatus(status);
    }

    public List<Qna> findResolvedWithoutQna() {
        return qnaRepository.findResolvedWithoutQna();
    }

    public List<Qna> findAll() {
        return qnaRepository.findAll();
    }

    private String normalize(String text) {
        return text.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    // 미답 질문 기록
    public void recordQuestion(String rawQuestion) {
        String normalized = normalize(rawQuestion);

        qnaRepository.findByNormalizedText(normalized)
                .orElseGet(() ->
                        qnaRepository.save(
                                Qna.create(rawQuestion, normalized)
                        )
                );
    }
}
