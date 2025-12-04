package com.codehows.taelimbe.ai.dto;

import lombok.Data;

/**
 * 임베딩 생성을 위한 요청 데이터 전송 객체(DTO)입니다.
 * 임베딩을 생성할 텍스트 데이터를 캡슐화합니다.
 *
 * `@Data`는 Lombok 어노테이션으로, `@Getter`, `@Setter`, `@EqualsAndHashCode`, `@ToString`을 자동으로 생성합니다.
 */
@Data
public class EmbeddingRequest {
    /**
     * 임베딩을 생성할 원본 텍스트입니다.
     */
    private String text;
}
