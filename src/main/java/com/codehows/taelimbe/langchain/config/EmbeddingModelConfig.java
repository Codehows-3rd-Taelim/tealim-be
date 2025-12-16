package com.codehows.taelimbe.langchain.config;

import com.codehows.taelimbe.langchain.models.GeminiEmbeddingModel;
import com.codehows.taelimbe.langchain.models.GeminiParallelEmbeddingModel;
import com.google.genai.Client;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class EmbeddingModelConfig {

    @Value("${gemini.model.embedding}")
    private String embeddingModelName;

    /**
     * 기본 직렬 임베딩 (Chat / AI Report / RAG)
     */
    @Bean
    public EmbeddingModel embeddingModel(Client geminiClient) {
        return new GeminiEmbeddingModel(
                geminiClient,
                embeddingModelName
        );
    }

    /**
     * 병렬 임베딩 전용 Executor
     */
    @Bean(name = "embeddingExecutor", destroyMethod = "shutdown")
    public ExecutorService embeddingExecutor() {
        return Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2
        );
    }

    /**
     * 병렬 임베딩 (CSV / PDF 전용)
     */
    @Bean
    @Qualifier("parallelEmbeddingModel")
    public EmbeddingModel parallelEmbeddingModel(
            Client geminiClient,
            @Qualifier("embeddingExecutor") ExecutorService embeddingExecutor
    ) {
        return new GeminiParallelEmbeddingModel(
                geminiClient,
                embeddingModelName,
                embeddingExecutor
        );
    }
}
