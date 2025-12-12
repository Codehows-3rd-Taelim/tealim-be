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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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

            try {
                // 1. 텍스트 분할 전략을 사용하여 텍스트를 작은 `TextSegment`들로 분할합니다.
                List<TextSegment> segments = textSplitterStrategy
                        .split(text)
                        .stream()
                        .map(TextSegment::from)
                        .toList();

                log.info("Segments size = {}", segments.size());
                if (segments.isEmpty()) {
                    log.warn("⚠ 분할된 세그먼트가 없습니다. 처리 중단.");
                    return;
                }

                // 2. `EmbeddingModel`을 사용하여 각 `TextSegment`를 임베딩 벡터로 변환합니다.
                Response<List<Embedding>> embedding = embeddingModel.embedAll(segments);


                // 3. 임베딩된 `TextSegment`와 해당 임베딩 벡터를 `EmbeddingStore`에 추가합니다.
                embeddingStore.addAll(embedding.content(), segments);

            } catch (Exception e) {
                log.error("임베딩 중 오류 발생!", e);
                throw new RuntimeException(e);
            }

            log.info("텍스트 임베딩 및 저장 완료.");
        }, taskExecutor);
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

    /**
     * CSV 파일을 받아 파싱하고 내용을 임베딩하여 벡터 저장소에 추가합니다.
     * @param file 임베딩할 데이터가 포함된 CSV 파일
     * @return 비동기 작업의 완료를 나타내는 `CompletableFuture<Void>`
     */
    public CompletableFuture<Void> embedAndStoreCsv(MultipartFile file) {
        return CompletableFuture.runAsync(() -> {
            log.info("CSV 파일 임베딩 및 저장 시작: {}", file.getOriginalFilename());

            // 🌟 BOMInputStream을 사용하여 BOM 문제를 해결하도록 로직 수정
            try (BOMInputStream bomIn = new BOMInputStream(file.getInputStream());
                 Reader reader = new InputStreamReader(bomIn, StandardCharsets.UTF_8)) { // UTF-8로 지정

                // 1. CSV 파일 파싱 (여기서는 Apache Commons CSV를 가정)
                // BOMInputStream 덕분에 헤더 파싱 시 BOM 문자가 제거됩니다.
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);

                for (CSVRecord record : records) {
                    // 2. 임베딩할 텍스트 추출/결합
                    // (이 부분은 파일의 실제 헤더 이름이 'column1', 'column2'라고 가정합니다.)
                    // 오류 로그에서 "expected one of [﻿column1, column2]"라고 했으므로
                    // BOM이 제거되면 순수하게 "column1"과 "column2"를 찾을 수 있습니다.
                    String documentText = String.format("제목: %s, 내용: %s",
                            record.get("column1"),
                            record.get("column2"));

                    // 3. 텍스트 분할 및 임베딩 로직 실행
                    List<TextSegment> segments = textSplitterStrategy.split(documentText).stream().map(TextSegment::from).toList();

                    Response<List<Embedding>> embedding = embeddingModel.embedAll(segments);

                    embeddingStore.addAll(embedding.content(), segments);
                }

                log.info("CSV 파일 임베딩 및 저장 완료.");
            } catch (Exception e) {
                log.error("CSV 파일 처리 중 오류 발생", e);
                // 오류가 CompletableFuture 밖으로 전파되도록 처리
                throw new RuntimeException("CSV 파일 처리 및 임베딩 실패", e);
            }
        }, taskExecutor);
    }
}
