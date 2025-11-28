package com.codehows.taelimbe.langchain.config;

import com.codehows.taelimbe.langchain.models.GeminiEmbeddingModel;
import com.codehows.taelimbe.langchain.models.GeminiStreamingChatModel;
import com.google.genai.Client;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Google Gemini LLM을 위한 `LLMFactory` 구현체입니다.
 * 이 팩토리는 `com.google.genai.Client`를 사용하여 Gemini 스트리밍 채팅 모델과 임베딩 모델을 생성합니다.
 * `@Component` 어노테이션을 통해 Spring 컨테이너에 의해 관리되는 빈으로 등록됩니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 */
@Component
@RequiredArgsConstructor
public class GeminiLLMFactory implements LLMFactory {

    // `com.google.genai` 클라이언트 주입
    private final Client geminiClient;

    // application.properties에서 Gemini 채팅 모델 이름을 주입받습니다.
    @Value("${gemini.model.chat}")
    private String chatModelName;

    // application.properties에서 Gemini 임베딩 모델 이름을 주입받습니다.
    @Value("${gemini.model.embedding}")
    private String embeddingModelName;

    /**
     * Gemini 스트리밍 채팅 모델을 생성하여 반환합니다.
     * `GeminiStreamingChatModel`의 빌더를 사용하여 모델을 구성합니다.
     *
     * @return `GeminiStreamingChatModel` 인스턴스
     */
    @Override
    public StreamingChatLanguageModel createStreamingChatModel() {
        return GeminiStreamingChatModel.builder()
                .geminiClient(geminiClient)
                .modelName(chatModelName)
                .build();
    }

    /**
     * Gemini 임베딩 모델을 생성하여 반환합니다.
     * `GeminiEmbeddingModel`의 생성자를 사용하여 모델을 구성합니다.
     *
     * @return `GeminiEmbeddingModel` 인스턴스
     */
    @Override
    public EmbeddingModel createEmbeddingModel() {
        return new GeminiEmbeddingModel(geminiClient, embeddingModelName);
    }
}
