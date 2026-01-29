package com.codehows.taelimbe.ai.embedding.repository;

import com.codehows.taelimbe.ai.embedding.dto.EmbedFileDTO;
import com.codehows.taelimbe.ai.embedding.entity.EmbedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmbedFileRepository extends JpaRepository<EmbedFile, Long> {

    Optional<EmbedFile> findByStoredName(String storedName);
    //이미 등록된 파일인지 확인하기 위해서 사용

    Optional<EmbedFile> findByEmbedKey(String embedKey);
    //질문 발생하면 사용중인 embedKey를 만든 파일이 뭐였는지 알수있음

    List<EmbedFileDTO> findAllByOrderByCreatedAtDesc();

    boolean existsByOriginalName(String originalName);
}
