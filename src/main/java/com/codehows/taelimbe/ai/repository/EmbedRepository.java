package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.constant.EmbedSourceType;
import com.codehows.taelimbe.ai.entity.Embed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmbedRepository extends JpaRepository<Embed, String> {
    List<Embed> findBySourceQuestionIdAndActiveTrue(Long questionId);
}
