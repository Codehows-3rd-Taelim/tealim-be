package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.EmbedFileDTO;
import com.codehows.taelimbe.ai.entity.EmbedFile;
import com.codehows.taelimbe.ai.repository.EmbedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmbedFileService {

    private final EmbedFileRepository embedFileRepository;

    @Transactional(readOnly = true)
    public List<EmbedFileDTO> getAllFiles() {
        return embedFileRepository.findAllByOrderByCreatedAtDesc();
    }
    private EmbedFileDTO embedFileDTO(EmbedFile file) {
        return EmbedFileDTO.builder()
                .id(file.getId())
                .originalName(file.getOriginalName())
                .storedName(file.getStoredName())
                .extension(file.getExtension())
                .fileSize(file.getFileSize())
                .status(file.getStatus())
                .embedKey(file.getEmbedKey())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    /** 업로드 직후 */
    public EmbedFileDTO createUploaded(String originalName, String storedName, String extension, long fileSize) {
        EmbedFile file = EmbedFile.uploaded(originalName, storedName, extension, fileSize);
        EmbedFile saved = embedFileRepository.save(file);
        return embedFileDTO(saved);
    }
    /** 임베딩 시작 */
    public void markEmbedding(Long fileId) {
        EmbedFile file = getFile(fileId);
        file.markEmbedding();
    }

    /** 임베딩 완료 */
    public void markDone(Long fileId, String embedKey) {
        EmbedFile file = getFile(fileId);
        file.markDone(embedKey);
    }

    /** 실패 */
    public void markFailed(Long fileId) {
        EmbedFile file = getFile(fileId);
        file.markFailed();
    }

    private EmbedFile getFile(Long id) {
        return embedFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("파일 없음"));
    }


}
