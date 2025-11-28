package com.codehows.taelimbe.langchain.config;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * 다양한 LLM(Large Language Model) 제공자로부터 스트리밍 채팅 모델과 임베딩 모델을 생성하기 위한 팩토리 인터페이스입니다.
 * 이 인터페이스를 통해 특정 LLM 구현체에 대한 의존성을 줄이고,
 * 새로운 LLM 제공자를 쉽게 통합할 수 있도록 확장성을 제공합니다.
 */
public interface LLMFactory {

    /**
     * 스트리밍 채팅 언어 모델을 생성하여 반환합니다.
     *
     * @return 스트리밍 채팅 언어 모델의 인스턴스
     */
    StreamingChatLanguageModel createStreamingChatModel();

    /**
     * 임베딩 모델을 생성하여 반환합니다.
     *
     * @return 임베딩 모델의 인스턴스
     */
    EmbeddingModel createEmbeddingModel();
}
