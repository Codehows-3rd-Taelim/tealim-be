package com.codehows.taelimbe.ai.embedding.repository;

import com.codehows.taelimbe.ai.embedding.entity.Embed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmbedRepository extends JpaRepository<Embed, String> {

    // 특정 QnA에 연결된 임베딩 조회
    Optional<Embed> findByQnaId(Long qnaId);
}
