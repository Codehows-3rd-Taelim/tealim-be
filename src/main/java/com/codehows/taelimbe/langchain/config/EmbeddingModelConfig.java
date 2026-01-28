package com.codehows.taelimbe.langchain.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class EmbeddingModelConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model.embedding}")
    private String embeddingModelName;

    /**
     * 기본 직렬 임베딩 (Chat / AI Report / RAG)
     */
    @Bean("lcEmbeddingModel")
    public EmbeddingModel llmFactoryEmbeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * 병렬 임베딩 (CSV / PDF 전용) - 별도 인스턴스
     */
    @Bean
    @Qualifier("parallelEmbeddingModel")
    public EmbeddingModel parallelEmbeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName(embeddingModelName)
                .build();
    }
}
