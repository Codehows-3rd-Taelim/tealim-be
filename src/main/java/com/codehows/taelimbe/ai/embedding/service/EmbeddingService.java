package com.codehows.taelimbe.ai.embedding.service;

import com.codehows.taelimbe.ai.embedding.entity.Embed;
import com.codehows.taelimbe.ai.embedding.entity.QnaEmbeddingCleanup;
import com.codehows.taelimbe.ai.embedding.repository.EmbedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 텍스트 임베딩 및 벡터 저장소 관리를 담당하는 서비스입니다.
 * Spring AI의 TikaDocumentReader를 사용하여 다양한 파일 형식을 범용적으로 처리합니다.
 * 지원 형식: PDF, Word(doc/docx), Excel(xls/xlsx), PowerPoint(ppt/pptx), HTML, TXT, CSV 등
 */
@Service
@Slf4j
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final EmbeddingStoreManager embeddingStoreManager;
    private final TokenTextSplitter tokenTextSplitter;
    private final EmbedRepository embedRepository;
    private final QnaEmbeddingCleanupService qnaEmbeddingCleanupService;

    @Qualifier("taskExecutor")
    private final TaskExecutor taskExecutor;

    public EmbeddingService(
            VectorStore vectorStore,
            EmbeddingStoreManager embeddingStoreManager,
            TokenTextSplitter tokenTextSplitter,
            EmbedRepository embedRepository,
            QnaEmbeddingCleanupService qnaEmbeddingCleanupService,
            @Qualifier("taskExecutor") TaskExecutor taskExecutor
    ) {
        this.vectorStore = vectorStore;
        this.embeddingStoreManager = embeddingStoreManager;
        this.tokenTextSplitter = tokenTextSplitter;
        this.embedRepository = embedRepository;
        this.qnaEmbeddingCleanupService = qnaEmbeddingCleanupService;
        this.taskExecutor = taskExecutor;
    }

    @Value("${embedding.file.batch.size:50}")
    private int embeddingFileBatchSize;

    @Value("${embedding.retry.max-attempts}")
    private int maxRetryAttempts;
    @Value("${embedding.retry.initial-delay-ms}")
    private long initialRetryDelayMs;

    /**
     * 주어진 텍스트를 임베딩하여 벡터 저장소에 추가합니다.
     */
    public CompletableFuture<Void> embedAndStore(String text) {
        return CompletableFuture.runAsync(() -> {
            log.info("텍스트 임베딩 및 저장 시작: '{}'", text);

            List<Document> documents = splitText(text);
            vectorStore.add(documents);

            log.info("텍스트 임베딩 및 저장 완료.");
        }, taskExecutor);
    }

    /**
     * 텍스트를 TokenTextSplitter를 사용해 Document 리스트로 분할합니다.
     */
    private List<Document> splitText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Document document = new Document(text);
        return tokenTextSplitter.split(document).stream()
                .filter(doc -> doc.getText() != null && !doc.getText().isBlank())
                .toList();
    }

    public CompletableFuture<Void> reset() {
        return CompletableFuture.runAsync(() -> {
            log.info("임베딩 스토어 재설정 시작.");
            embeddingStoreManager.reset();
            log.info("임베딩 스토어 재설정 완료.");
        }, taskExecutor);
    }

    public CompletableFuture<Void> resetAndEmbed(String text) {
        return CompletableFuture.runAsync(() -> {
            log.info("임베딩 스토어 재설정 및 새 텍스트 임베딩 시작.");
            embeddingStoreManager.reset();
            embedAndStore(text);
            log.info("임베딩 스토어 재설정 및 새 텍스트 임베딩 완료.");
        }, taskExecutor);
    }

    public void resetOnly() {
        embeddingStoreManager.reset();
    }

    /**
     * TikaDocumentReader를 사용하여 다양한 파일 형식을 범용적으로 임베딩합니다.
     * 지원 형식: PDF, Word, Excel, PowerPoint, HTML, TXT, CSV, RTF 등
     */
    public CompletableFuture<Void> embedAndStoreFile(Path filePath, String embedKey) {
        return CompletableFuture.runAsync(() -> {
            String fileName = filePath.getFileName().toString();
            log.info("파일 임베딩 시작 (Tika): {}", fileName);

            try {
                Resource resource = new FileSystemResource(filePath.toFile());
                TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
                List<Document> tikaDocuments = tikaReader.get();

                List<Document> buffer = new ArrayList<>(embeddingFileBatchSize);
                AtomicInteger chunkCounter = new AtomicInteger(1);

                for (Document tikaDoc : tikaDocuments) {
                    String content = tikaDoc.getText();
                    if (content == null || content.isBlank()) {
                        continue;
                    }

                    List<Document> splitDocs = splitText(content);

                    for (Document splitDoc : splitDocs) {
                        Map<String, Object> metadata = new HashMap<>(tikaDoc.getMetadata());
                        metadata.put("embedKey", embedKey);
                        metadata.put("fileName", fileName);
                        metadata.put("chunk", chunkCounter.getAndIncrement());

                        buffer.add(new Document(splitDoc.getText(), metadata));

                        if (buffer.size() >= embeddingFileBatchSize) {
                            flushBatch(buffer);
                        }
                    }
                }

                if (!buffer.isEmpty()) {
                    flushBatch(buffer);
                }

                log.info("파일 임베딩 완료 (embedKey={}, chunks={})", embedKey, chunkCounter.get() - 1);

            } catch (Exception e) {
                log.error("파일 임베딩 실패: {}", fileName, e);
                throw new RuntimeException(e);
            }

        }, taskExecutor);
    }

    private void flushBatch(List<Document> batch) {
        vectorStore.add(new ArrayList<>(batch));
        batch.clear();
    }

    // QnA 임베딩
    public String embedQna(String text, Long qnaId) {
        String key = UUID.randomUUID().toString();

        try {
            List<Document> splitDocs = splitText(text);

            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < splitDocs.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("key", key);
                metadata.put("chunk", i + 1);

                documents.add(new Document(splitDocs.get(i).getText(), metadata));
            }

            // pgvector에 저장
            embeddingStoreManager.addDocuments(documents);

            // DB 저장
            Embed embed = Embed.createText(key, text, qnaId);
            embedRepository.save(embed);

            log.info("QnA embedding completed (key={})", key);
            return key;

        } catch (Exception e) {
            qnaEmbeddingCleanupService.record(qnaId, key);
            throw e;
        }
    }

    public void replaceQnaEmbedding(Long qnaId, String text) {
        // 1. 이전 cleanup 정리
        cleanupByQnaId(qnaId);

        // 2. 기존 임베딩 조회
        Embed oldEmbed = embedRepository.findByQnaId(qnaId).orElse(null);
        String oldEmbedKey = oldEmbed != null ? oldEmbed.getEmbedKey() : null;

        // 3. 새 임베딩 생성
        final String newEmbedKey = embedQna(text, qnaId);

        try {
            // 4. 기존 임베딩 삭제
            if (oldEmbedKey != null) {
                boolean deleted = embeddingStoreManager.deleteDocuments(oldEmbedKey);
                if (deleted) {
                    embedRepository.delete(oldEmbed);
                }
            }
        } catch (Exception e) {
            qnaEmbeddingCleanupService.record(qnaId, newEmbedKey);
            throw e;
        }
    }

    public boolean deleteEmbeddingByKey(String embedKey) {
        return embeddingStoreManager.deleteDocuments(embedKey);
    }

    public void cleanupByQnaId(Long qnaId) {
        List<QnaEmbeddingCleanup> cleanups =
                qnaEmbeddingCleanupService.findByQnaId(qnaId);

        for (QnaEmbeddingCleanup cleanup : cleanups) {
            boolean deleted = deleteEmbeddingByKey(cleanup.getEmbedKey());
            if (deleted) {
                qnaEmbeddingCleanupService.delete(cleanup);
            }
        }
    }
}
