package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.QnaEmbeddingCleanup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaEmbeddingCleanupRepository
        extends JpaRepository<QnaEmbeddingCleanup, Long> {

    List<QnaEmbeddingCleanup> findByQnaId(Long qnaId);
}
