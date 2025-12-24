package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.Embed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmbedRepository extends JpaRepository<Embed, String> {


    // 특정 QnA에 연결된 임베딩 조회
    List<Embed> findByQnaId(Long qnaId);
}
