package com.codehows.taelimbe.ai.entity;

import com.codehows.taelimbe.ai.constant.EmbedFileStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "embed_file")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EmbedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자가 업로드한 원본 파일명 */
    private String originalName;   // CC1 사용법 가이드.pdf

    /** 서버에 저장된 파일명 (UUID 기반) */
    private String storedName;     // uuid.pdf

    /** 파일 확장자 */
    private String extension;      // pdf / csv

    /** 파일 크기 */
    private long fileSize;

    /** 임베딩 처리 상태 */
    @Enumerated(EnumType.STRING)
    private EmbedFileStatus status;

    /** 생성된 Embed 묶음 키 */
    private String embedKey;       // Embed.embedKey

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* ================= 생성 메서드 ================= */

    public static EmbedFile uploaded(
            String originalName,
            String storedName,
            String extension,
            long fileSize
    ) {
        LocalDateTime now = LocalDateTime.now();
        return EmbedFile.builder()
                .originalName(originalName)
                .storedName(storedName)
                .extension(extension)
                .fileSize(fileSize)
                .status(EmbedFileStatus.UPLOADED)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /* ================= 상태 변경 ================= */

    public void markEmbedding() {
        this.status = EmbedFileStatus.EMBEDDING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDone(String embedKey) {
        this.status = EmbedFileStatus.DONE;
        this.embedKey = embedKey;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = EmbedFileStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
}
