package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.EmbedFileDTO;
import com.codehows.taelimbe.ai.entity.EmbedFile;
import com.codehows.taelimbe.ai.service.EmbedFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/embed-files")
public class EmbedFileController {

    private final EmbedFileService embedFileService;

    @GetMapping
    public List<EmbedFileDTO> getAllFiles() {
        return embedFileService.getAllFiles();
    }

    @PostMapping
    public EmbedFileDTO upload(@RequestParam MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }
        String extension = getExtension(originalName);

        if (!extension.equalsIgnoreCase("pdf") && !extension.equalsIgnoreCase("csv")) {
            throw new IllegalArgumentException("PDF 또는 CSV만 업로드 가능합니다.");
        }

        String storedName = UUID.randomUUID() + "." + extension;

        return embedFileService.createUploaded(
                originalName,
                storedName,
                extension,
                file.getSize()
        );
    }

    //파일 확장자 추출
    // 확장자는 중복될 수 있으니 이렇게 따로 빼두는거임
    private String getExtension(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
