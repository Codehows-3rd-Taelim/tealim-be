
package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByNormalizedText(String normalizedText);

    List<Question> findByResolvedFalse();

    List<Question> findByResolvedTrue();
}
