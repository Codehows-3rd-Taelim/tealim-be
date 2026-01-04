package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.entity.QnaEmbeddingCleanup;
import com.codehows.taelimbe.ai.repository.QnaEmbeddingCleanupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QnaEmbeddingCleanupService {

    private final QnaEmbeddingCleanupRepository cleanupRepository;


    public List<QnaEmbeddingCleanup> findByQnaId(Long qnaId) {
        return cleanupRepository.findByQnaId(qnaId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(QnaEmbeddingCleanup cleanup) {
        cleanupRepository.delete(cleanup);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long qnaId, String embedKey) {
        cleanupRepository.save(
                new QnaEmbeddingCleanup(qnaId, embedKey)
        );
    }
}




