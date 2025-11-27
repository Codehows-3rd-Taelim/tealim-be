package com.codehows.taelimbe.langchain.config;

import com.codehows.taelimbe.langchain.converters.LocalDateTimeAdapter;

import com.google.genai.Client;
import com.google.gson.Gson; // Gson 임포트
import com.google.gson.GsonBuilder; // GsonBuilder 임포트
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime; // LocalDateTime 임포트

/**
 * LangChain4j와 관련된 모든 Bean 설정을 담당하는 클래스입니다.
 * AI 모델, 임베딩, 벡터 저장소, 대화 메모리 등 AI 서비스의 핵심 구성요소를 설정합니다.
 * `@Configuration` 어노테이션은 이 클래스가 Spring의 설정 클래스임을 나타냅니다.
 */
@Configuration
public class LangChainConfig {

    // Gemini API 키. LLMFactory에서 사용하기 위해 남겨둡니다.
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${langchain.chat.max-messages}")
    private Integer chatMaxMessages;

    // Milvus 벡터 저장소 설정을 위한 프로퍼티
    @Value("${milvus.host}")
    private String milvusHost;

    @Value("${milvus.port}")
    private Integer milvusPort;

    @Value("${milvus.collection-name}")
    private String milvusCollectionName;

    @Value("${langchain.rag.max-results}")
    private Integer langChainRagMaxResults;

    @Value("${langchain.rag.min-score}")
    private Double langChainRagMinScore;

    @Value("${milvus.embedding.dimension}")
    private Integer embeddingDimension;

    /**
     * Gson 인스턴스를 Spring Bean으로 등록합니다.
     * `LocalDateTimeAdapter`를 등록하여 `LocalDateTime` 객체를 ISO 8601 형식으로
     * 직렬화/역직렬화할 수 있도록 구성합니다.
     *
     * @return 구성된 `Gson` 인스턴스
     */
    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * `com.google.genai` 클라이언트를 Bean으로 등록합니다.
     * 이 클라이언트는 Gemini 모델과의 직접적인 통신을 담당합니다。
     * `@Bean` 어노테이션은 이 메서드가 반환하는 객체를 Spring 컨테이너의 빈으로 등록하도록 지시합니다.
     *
     * @return `com.google.genai.Client` 인스턴스
     */
    @Bean
    public Client geminiClient() {
        return Client.builder()
                .apiKey(geminiApiKey)
                .build();
    }

    /**
     * LLMFactory 인터페이스의 구현체를 Bean으로 등록합니다.
     * 여기서는 GeminiLLMFactory를 사용합니다.
     *
     * @param geminiClient `com.google.genai` 클라이언트 (이전에 정의된 빈을 주입받음)
     * @return `LLMFactory` 인스턴스
     */
    @Bean
    public LLMFactory llmFactory(Client geminiClient) {
        return new GeminiLLMFactory(geminiClient);
    }

    /**
     * 스트리밍 채팅 언어 모델을 Bean으로 등록합니다.
     * `LLMFactory`를 통해 모델을 생성합니다.
     *
     * @param llmFactory LLMFactory (이전에 정의된 빈을 주입받음)
     * @return `StreamingChatLanguageModel` 인스턴스
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(LLMFactory llmFactory) {
        return llmFactory.createStreamingChatModel();
    }

    /**
     * 임베딩 모델을 Bean으로 등록합니다.
     * `LLMFactory`를 통해 모델을 생성합니다.
     *
     * @param llmFactory LLMFactory (이전에 정의된 빈을 주입받음)
     * @return `EmbeddingModel` 인스턴스
     */
    @Bean
    public EmbeddingModel embeddingModel(LLMFactory llmFactory) {
        return llmFactory.createEmbeddingModel();
    }

    /**
     * Milvus 임베딩 저장소를 Bean으로 등록합니다.
     * 임베딩된 벡터 데이터를 저장하고 검색하는 데 사용됩니다.
     *
     * @return `EmbeddingStore` 인스턴스
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(milvusHost)
                .port(milvusPort)
                .collectionName(milvusCollectionName)
                .dimension(embeddingDimension) // Gemini 임베딩 모델의 차원 수 (text-embedding-004 기준)
                .build();
    }

    /**
     * 임베딩 저장소를 활용하는 `ContentRetriever`를 Bean으로 등록합니다.
     * RAG(Retrieval-Augmented Generation)의 핵심 요소로, 사용자 질문과 관련된 문서를 벡터 저장소에서 찾아 AI 모델에 제공합니다.
     *
     * @param embeddingStore 임베딩 저장소 (이전에 정의된 빈을 주입받음)
     * @param embeddingModel 임베딩 모델 (이전에 정의된 빈을 주입받음)
     * @return `ContentRetriever` 인스턴스
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(langChainRagMaxResults) // 최대 2개의 관련성 높은 결과를 검색합니다.
                .minScore(langChainRagMinScore) // 최소 유사도 점수가 0.5 이상인 결과만 반환합니다.
                .build();
    }

    /**
     * 대화 메모리 제공자(`ChatMemoryProvider`)를 Bean으로 등록합니다.
     * 사용자별 대화 기록을 관리하여 멀티턴 대화를 가능하게 합니다.
     * `MessageWindowChatMemory`는 최근 N개의 메시지를 기억하는 윈도우 기반 메모리입니다.
     *
     * @return `ChatMemoryProvider` 인스턴스
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId) // 대화 ID를 기반으로 메모리를 관리합니다.
                .maxMessages(chatMaxMessages) // 최근 20개의 메시지를 기억합니다.
                .chatMemoryStore(new InMemoryChatMemoryStore()) // 메모리 저장을 위한 인메모리 스토어를 사용합니다.
                .build();
    }

}
