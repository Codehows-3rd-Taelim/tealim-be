package com.codehows.taelimbe.ai.embedding.repository;

import com.codehows.taelimbe.ai.embedding.entity.QnaEmbeddingCleanup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaEmbeddingCleanupRepository
        extends JpaRepository<QnaEmbeddingCleanup, Long> {

    List<QnaEmbeddingCleanup> findByQnaId(Long qnaId);
}
