package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.EmbedFileDTO;
import com.codehows.taelimbe.ai.entity.EmbedFile;
import com.codehows.taelimbe.ai.service.EmbedFileService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public EmbedFileDTO upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("embedKey") String embedKey
    ) throws IOException {

        return embedFileService.uploadAndEmbed(file, embedKey);
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

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        EmbedFile file = embedFileService.getFileById(id);


        Path path = Paths.get("./uploads", file.getStoredName());

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(path.toUri());
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + UriUtils.encode(file.getOriginalName(), "UTF-8") + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
