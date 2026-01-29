package com.codehows.taelimbe.ai.embedding.service;

import com.codehows.taelimbe.ai.embedding.entity.EmbedFile;
import com.codehows.taelimbe.ai.embedding.repository.EmbedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmbedFileStatusService {

    private final EmbedFileRepository embedFileRepository;
    private final EmbeddingStoreManager embeddingStoreManager;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDone(Long id, String embedKey) {
        EmbedFile file = embedFileRepository.findById(id).orElseThrow();
        file.markDone(embedKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, Path filePath) {
        EmbedFile file = embedFileRepository.findById(id).orElseThrow();
        file.markFailed();  // 상태를 FAILED로 표시

        if (file.getEmbedKey() != null) {
            embeddingStoreManager.deleteDocuments(file.getEmbedKey());
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", filePath, e);
        }

        log.warn("임베딩 실패: 파일={} (id={}) 상태를 FAILED로 설정", file.getOriginalName(), file.getId());
    }
}
