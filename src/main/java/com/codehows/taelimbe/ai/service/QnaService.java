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
    public void apply(Long qnaId) {
        Qna qna = get(qnaId);

        if (qna.getStatus() != QnaStatus.EDITING) {
            throw new IllegalStateException("QnA is not in EDITING state");
        }

        String text =
                "Q: " + qna.getQuestionText() + "\n" +
                        "A: " + qna.getEditingAnswer();

        try {
            embeddingService.embedQna(text, qnaId).join();
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
}
