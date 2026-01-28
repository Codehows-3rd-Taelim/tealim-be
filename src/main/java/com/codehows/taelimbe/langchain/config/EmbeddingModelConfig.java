package com.codehows.taelimbe.langchain.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring AI auto-config가 EmbeddingModel Bean을 자동 생성합니다.
 * 추가 설정이 필요한 경우 이 클래스에서 관리합니다.
 */
@Configuration
public class EmbeddingModelConfig {
    // Spring AI auto-config가 google-genai EmbeddingModel을 자동 등록합니다.
    // application.properties의 spring.ai.google.genai.embedding.* 설정 참조.
}
