package com.codehows.taelimbe.ai.embedding.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class QnaEmbeddingRequest {
    private Long QuestionId;
    private String question;
    private String answer;
}
