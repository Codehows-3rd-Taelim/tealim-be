package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.entity.UnansweredQuestion;
import com.codehows.taelimbe.ai.repository.UnansweredQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnansweredQuestionService {

    private final UnansweredQuestionRepository unansweredQuestionRepository;

    public void record(String rawQuestion) {
        String normalized = normalize(rawQuestion);

        unansweredQuestionRepository.findByNormalizedText(normalized)
                .ifPresentOrElse(
                        q -> {
                            q.increaseCount();
                            log.info("미답 질문 카운트 증가: {}", normalized);
                        },
                        () -> {
                            unansweredQuestionRepository.save(
                                    UnansweredQuestion.create(rawQuestion, normalized)
                            );
                            log.info("미답 질문 신규 등록: {}", normalized);
                        }
                );
    }

    private String normalize(String text) {
        return text
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }
}
