package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.EmbedFileDTO;
import com.codehows.taelimbe.ai.entity.EmbedFile;
import com.codehows.taelimbe.ai.service.EmbedFileService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/embed-files")
public class EmbedFileController {

    private final EmbedFileService embedFileService;
    private final EmbeddingService embeddingService;

    @GetMapping
    public List<EmbedFileDTO> getAllFiles() {
        return embedFileService.getAllFiles();
    }

    @PostMapping
    public EmbedFileDTO upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("embedKey") String embedKey
    ) {
        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        String extension = getExtension(originalName).toLowerCase();
        String storedName = UUID.randomUUID() + "." + extension;

        EmbedFileDTO dto = embedFileService.createUploaded(
                originalName,
                storedName,
                extension,
                file.getSize(),
                embedKey
        );

        if (extension.equals("pdf")) {
            embeddingService.embedAndStorePdf(file, embedKey);
        } else if (extension.equals("csv")) {
            embeddingService.embedAndStoreCsv(file, embedKey);
        } else {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }

        return dto;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        embedFileService.deleteFile(id);
        return ResponseEntity.ok().build();
    }

    //파일 확장자 추출
    // 확장자는 중복될 수 있으니 이렇게 따로 빼두는거임
    private String getExtension(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
