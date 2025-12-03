package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.langchain.embaddings.EmbeddingStoreManager;
import com.codehows.taelimbe.langchain.embaddings.TextSplitterStrategy;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 텍스트 임베딩 및 벡터 저장소 관리를 담당하는 서비스입니다.
 * `@Service` 어노테이션은 이 클래스가 비즈니스 계층의 컴포넌트임을 나타내며,
 * Spring 컨테이너에 의해 관리되는 빈으로 등록됩니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 * `@Slf4j`는 Lombok 어노테이션으로, 로깅을 위한 `log` 객체를 자동으로 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    // 텍스트를 임베딩 벡터로 변환하는 모델을 주입받습니다.
    private final EmbeddingModel embeddingModel;
    // 생성된 임베딩 벡터를 저장하고 검색하는 스토어를 주입받습니다.
    private final EmbeddingStore<TextSegment> embeddingStore;
    // 임베딩 스토어의 초기화 및 관리 기능을 제공하는 매니저를 주입받습니다.
    private final EmbeddingStoreManager embeddingStoreManager;
    // 텍스트 분할 전략을 주입받습니다.
    private final TextSplitterStrategy textSplitterStrategy;

    // 비동기 작업을 위한 스레드 풀을 주입받습니다.
    @Qualifier("taskExecutor")
    private final TaskExecutor taskExecutor;

    /**
     * 주어진 텍스트를 임베딩하여 벡터 저장소에 추가합니다.
     * 이 메서드는 RAG(Retrieval-Augmented Generation)를 위한 지식 기반을 구축하는 데 사용됩니다.
     * 작업은 비동기적으로 실행되어 호출 스레드를 블로킹하지 않습니다.
     *
     * @param text 임베딩하고 저장할 텍스트
     * @return 비동기 작업의 완료를 나타내는 `CompletableFuture<Void>`
     */
    public CompletableFuture<Void> embedAndStore(String text) {
        return CompletableFuture.runAsync(() -> {
            log.info("텍스트 임베딩 및 저장 시작: '{}'", text);

            // 1. 텍스트 분할 전략을 사용하여 텍스트를 작은 `TextSegment`들로 분할합니다.
            List<TextSegment> segments = textSplitterStrategy.split(text).stream().map(TextSegment::from).toList();

            // 2. `EmbeddingModel`을 사용하여 각 `TextSegment`를 임베딩 벡터로 변환합니다.
            Response<List<Embedding>> embedding = embeddingModel.embedAll(segments);

            // 3. 임베딩된 `TextSegment`와 해당 임베딩 벡터를 `EmbeddingStore`에 추가합니다.
            embeddingStore.addAll(embedding.content(), segments);

            log.info("텍스트 임베딩 및 저장 완료.");
        }, taskExecutor); // 지정된 `taskExecutor` 스레드 풀에서 실행
    }

    /**
     * 기존 벡터 저장소의 모든 데이터를 삭제하고, 주어진 텍스트로 새로 임베딩하여 저장합니다.
     * 지식 기반을 완전히 초기화하고 새로운 데이터로 교체할 때 사용됩니다.
     * 작업은 비동기적으로 실행되어 호출 스레드를 블로킹하지 않습니다.
     *
     * @param text 새로 임베딩하고 저장할 텍스트
     * @return 비동기 작업의 완료를 나타내는 `CompletableFuture<Void>`
     */
    public CompletableFuture<Void> resetAndEmbed(String text) {
        return CompletableFuture.runAsync(() -> {
            log.info("임베딩 스토어 재설정 및 새 텍스트 임베딩 시작.");

            // 1. `EmbeddingStoreManager`를 사용하여 Milvus 컬렉션을 재설정(삭제 후 재생성)합니다.
            embeddingStoreManager.reset();

            // 2. 새로운 텍스트로 임베딩 및 저장을 수행합니다.
            embedAndStore(text);

            log.info("임베딩 스토어 재설정 및 새 텍스트 임베딩 완료.");
        }, taskExecutor); // 지정된 `taskExecutor` 스레드 풀에서 실행
    }
}
