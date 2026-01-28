package com.codehows.taelimbe.langchain.config;

import com.codehows.taelimbe.langchain.converters.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * LangChain4j와 관련된 모든 Bean 설정을 담당하는 클래스입니다.
 * AI 모델, 임베딩, 벡터 저장소, 대화 메모리 등 AI 서비스의 핵심 구성요소를 설정합니다.
 */
@Configuration
public class LangChainConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model.chat}")
    private String chatModelName;

    @Value("${langchain.chat.max-messages}")
    private Integer chatMaxMessages;

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

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * 공식 Google AI Gemini 스트리밍 채팅 모델을 Bean으로 등록합니다.
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(chatModelName)
                .temperature(0.0)
                .topP(0.95)
                .topK(40)
                .maxOutputTokens(8192)
                .build();
    }

    /**
     * Milvus 임베딩 저장소를 Bean으로 등록합니다.
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(milvusHost)
                .port(milvusPort)
                .collectionName(milvusCollectionName)
                .dimension(embeddingDimension)
                .build();
    }

    /**
     * RAG용 콘텐츠 검색기를 Bean으로 등록합니다.
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, @Qualifier("lcEmbeddingModel") EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(langChainRagMaxResults)
                .minScore(langChainRagMinScore)
                .build();
    }

    /**
     * 대화 메모리 제공자를 Bean으로 등록합니다.
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(chatMaxMessages)
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }
}
