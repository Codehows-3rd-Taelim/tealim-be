package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.UnansweredQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnansweredQuestionRepository
        extends JpaRepository<UnansweredQuestion, Long> {

    /**
     * 정규화된 질문 텍스트로 기존 미답 질문 조회
     */
    Optional<UnansweredQuestion> findByNormalizedText(String normalizedText);
}
