package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.qna.entity.Qna;
import com.codehows.taelimbe.qna.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QnaEmbeddingFailService {

    private final QnaRepository qnaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFail(Long qnaId, String answer) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow();
        qna.applyFail(answer);
    }
}
