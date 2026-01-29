package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.entity.Embed;
import com.codehows.taelimbe.ai.entity.QnaEmbeddingCleanup;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.langchain.converters.PdfEmbeddingNormalizer;
import com.codehows.taelimbe.langchain.embaddings.EmbeddingStoreManager;
import com.codehows.taelimbe.langchain.embaddings.TextSplitterStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 텍스트 임베딩 및 벡터 저장소 관리를 담당하는 서비스입니다.
 * Spring AI의 EmbeddingModel과 VectorStore를 사용합니다.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final EmbeddingStoreManager embeddingStoreManager;
    private final TextSplitterStrategy textSplitterStrategy;
    private final EmbedRepository embedRepository;
    private final QnaEmbeddingCleanupService qnaEmbeddingCleanupService;

    @Qualifier("taskExecutor")
    private final TaskExecutor taskExecutor;

    public EmbeddingService(
            EmbeddingModel embeddingModel,
            VectorStore vectorStore,
            EmbeddingStoreManager embeddingStoreManager,
            TextSplitterStrategy textSplitterStrategy,
            EmbedRepository embedRepository,
            QnaEmbeddingCleanupService qnaEmbeddingCleanupService,
            @Qualifier("taskExecutor") TaskExecutor taskExecutor
    ) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
        this.embeddingStoreManager = embeddingStoreManager;
        this.textSplitterStrategy = textSplitterStrategy;
        this.embedRepository = embedRepository;
        this.qnaEmbeddingCleanupService = qnaEmbeddingCleanupService;
        this.taskExecutor = taskExecutor;
    }

    @Value("${embedding.csv.batch.size}")
    private int embeddingCsvBatchSize;
    @Value("${embedding.pdf.batch.size}")
    private int embeddingPdfBatchSize;

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

            List<String> segments = textSplitterStrategy.split(text);
            List<Document> documents = segments.stream()
                    .map(Document::new)
                    .toList();

            vectorStore.add(documents);

            log.info("텍스트 임베딩 및 저장 완료.");
        }, taskExecutor);
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
     * CSV 파일을 받아 파싱하고 내용을 임베딩하여 벡터 저장소에 추가합니다.
     */
    public CompletableFuture<Void> embedAndStoreCsv(Path csvPath, String embedKey) {
        return CompletableFuture.runAsync(() -> {
            log.info("CSV 병렬 임베딩 시작 (batchSize={}): {}",
                    embeddingCsvBatchSize, csvPath.getFileName());

            try (InputStream is = Files.newInputStream(csvPath);
                 BOMInputStream bomIn = new BOMInputStream(
                         is,
                         ByteOrderMark.UTF_8,
                         ByteOrderMark.UTF_16LE,
                         ByteOrderMark.UTF_16BE
                 )) {
                Reader reader;
                if (bomIn.hasBOM(ByteOrderMark.UTF_16LE)) {
                    reader = new InputStreamReader(bomIn, StandardCharsets.UTF_16LE);
                } else if (bomIn.hasBOM(ByteOrderMark.UTF_16BE)) {
                    reader = new InputStreamReader(bomIn, StandardCharsets.UTF_16BE);
                } else if (bomIn.hasBOM(ByteOrderMark.UTF_8)) {
                    reader = new InputStreamReader(bomIn, StandardCharsets.UTF_8);
                } else {
                    reader = new InputStreamReader(bomIn, Charset.forName("MS949"));
                }
                Iterable<CSVRecord> records = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreHeaderCase()
                        .withTrim()
                        .withIgnoreEmptyLines()
                        .withAllowMissingColumnNames()
                        .parse(reader);

                List<Document> buffer = new ArrayList<>(embeddingCsvBatchSize);
                AtomicInteger chunkCounter = new AtomicInteger(1);

                for (CSVRecord record : records) {
                    String title = record.get("title").trim();
                    String content = record.get("content")
                            .replace("\r\n", "\n")
                            .trim();

                    String documentText =
                            String.format("제목: %s\n내용: %s", title, content);

                    List<String> segments = textSplitterStrategy.split(documentText);

                    for (String segment : segments) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("embedKey", embedKey);
                        metadata.put("source", "CSV");
                        metadata.put("chunk", chunkCounter.getAndIncrement());

                        buffer.add(new Document(segment, metadata));

                        if (buffer.size() >= embeddingCsvBatchSize) {
                            flushBatch(buffer);
                        }
                    }
                }

                if (!buffer.isEmpty()) {
                    flushBatch(buffer);
                }

                log.info("CSV 병렬 임베딩 완료 (embedKey={})", embedKey);

            } catch (Exception e) {
                log.error("CSV 병렬 임베딩 실패: {}", csvPath, e);
                throw new RuntimeException(e);
            }

        }, taskExecutor);
    }

    public CompletableFuture<Void> embedAndStorePdf(Path pdfPath, String embedKey) {
        return CompletableFuture.runAsync(() -> {
            log.info("PDF 병렬 임베딩 시작: {}", pdfPath.getFileName());

            try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String rawText = stripper.getText(document);

                String embeddingText = PdfEmbeddingNormalizer.normalize(
                        rawText, pdfPath.getFileName().toString());

                List<String> segments = textSplitterStrategy.split(embeddingText);

                List<Document> buffer = new ArrayList<>(embeddingPdfBatchSize);
                AtomicInteger chunkCounter = new AtomicInteger(1);

                for (String segment : segments) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("embedKey", embedKey);
                    metadata.put("source", "PDF");
                    metadata.put("chunk", chunkCounter.getAndIncrement());

                    buffer.add(new Document(segment, metadata));

                    if (buffer.size() >= embeddingPdfBatchSize) {
                        flushBatch(buffer);
                    }
                }

                if (!buffer.isEmpty()) {
                    flushBatch(buffer);
                }

                log.info("PDF 병렬 임베딩 완료 (embedKey={})", embedKey);

            } catch (Exception e) {
                log.error("PDF 임베딩 실패", e);
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
            List<String> segments = textSplitterStrategy.split(text);

            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("key", key);
                metadata.put("chunk", i + 1);

                documents.add(new Document(segments.get(i), metadata));
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
