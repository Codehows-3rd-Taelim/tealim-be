package com.codehows.taelimbe.ai.embedding.service;

import com.codehows.taelimbe.ai.embedding.dto.EmbedFileDTO;
import com.codehows.taelimbe.ai.embedding.entity.EmbedFile;
import com.codehows.taelimbe.ai.embedding.repository.EmbedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class EmbedFileService {

    private final EmbedFileRepository embedFileRepository;
    private final EmbeddingStoreManager embeddingStoreManager;
    private final Path uploadDir = Paths.get("./uploads");
    private final EmbeddingService embeddingService;
    private final EmbedFileStatusService embedFileStatusService;

    @Transactional(readOnly = true)
    public Page<EmbedFileDTO> getFiles(Pageable pageable) {
        return embedFileRepository.findAll(pageable)
                .map(this::toDTO);
    }

    private EmbedFileDTO toDTO(EmbedFile file) {
        return EmbedFileDTO.from(file);
    }

    public EmbedFileDTO uploadAndEmbed(MultipartFile file, String embedKey) {

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        if (embedFileRepository.existsByOriginalName(originalName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록된 파일입니다: " + originalName
            );
        }


        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        String storedName = java.util.UUID.randomUUID() + "." + extension;

        Path uploadPath = Paths.get("./uploads");
        Path filePath = uploadPath.resolve(storedName);


        final Path finalFilePath = filePath;

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            file.transferTo(filePath);

            EmbedFile embedFile = EmbedFile.uploaded(
                    originalName,
                    storedName,
                    extension,
                    file.getSize()
            );
            embedFile.markEmbedding();
            embedFileRepository.save(embedFile);

            Long embedFileId = embedFile.getId();

            // Tika를 통해 다양한 파일 형식 범용 처리
            embeddingService.embedAndStoreFile(filePath, embedKey)
                    .whenComplete((r, ex) -> {
                if (ex == null) {
                    embedFileStatusService.markDone(embedFileId, embedKey);
                } else {
                    embedFileStatusService.markFailed(embedFileId, finalFilePath);
                }
            });

            return EmbedFileDTO.from(embedFile); // 바로 응답

        } catch (Exception e) {
            try { Files.deleteIfExists(filePath); } catch (Exception ignore) {}
            throw new RuntimeException("임베딩 처리 중 오류 발생", e);
        }
    }


    @Transactional
    public void deleteFile(Long id) {

        EmbedFile file = embedFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다. id=" + id));

        Path filePath = uploadDir.resolve(file.getStoredName());
        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            throw new RuntimeException("업로드 파일 삭제 실패: " + filePath, e);
        }

        embeddingStoreManager.deleteDocuments(file.getEmbedKey());

        embedFileRepository.delete(file);
    }

    @Transactional(readOnly = true)
    public EmbedFile getFileById(Long id) {
        return embedFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다. id=" + id));
    }
}
