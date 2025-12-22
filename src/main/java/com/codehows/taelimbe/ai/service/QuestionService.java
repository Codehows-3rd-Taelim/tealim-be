package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.QuestionDTO;
import com.codehows.taelimbe.ai.entity.Question;
import com.codehows.taelimbe.ai.repository.QuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final EmbeddingService embeddingService;

    // 미답 질문 기록
    public void record(String rawQuestion) {
        String normalized = normalize(rawQuestion);

        questionRepository.findByNormalizedText(normalized)
                .orElseGet(() ->
                        questionRepository.save(
                                Question.create(rawQuestion, normalized)
                        )
                );
    }

    @Transactional
    public CompletableFuture<Void> embedQnaAndResolve(
            String question,
            String answer,
            Long questionId
    ) {
        String text = "Q: " + question + "\n" + "A: " + answer;

        CompletableFuture<Void> future =
                embeddingService.embedQna(text, questionId);

        if (questionId != null) {
            markResolved(questionId);
        }

        return future;
    }

    public void markResolved(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Question not found: " + questionId)
                );
        question.resolve();
    }

    public List<QuestionDTO> findAll() {
        return questionRepository.findAll()
                .stream()
                .map(QuestionDTO::new)
                .toList();
    }

    public List<QuestionDTO> findUnresolved() {
        return questionRepository.findByResolvedFalse()
                .stream()
                .map(QuestionDTO::new)
                .toList();
    }

    public List<QuestionDTO> findResolved() {
        return questionRepository.findByResolvedTrue()
                .stream()
                .map(QuestionDTO::new)
                .toList();
    }

    private String normalize(String text) {
        return text
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    public Question get(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Question not found: " + questionId)
                );
    }
}
