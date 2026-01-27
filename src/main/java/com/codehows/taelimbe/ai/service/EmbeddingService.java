package com.codehows.taelimbe.ai.service;

import com.alibaba.fastjson2.JSONObject;
import com.codehows.taelimbe.ai.entity.Embed;
import com.codehows.taelimbe.ai.entity.QnaEmbeddingCleanup;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.langchain.converters.PdfEmbeddingNormalizer;
import com.codehows.taelimbe.langchain.embaddings.EmbeddingStoreManager;
import com.codehows.taelimbe.langchain.embaddings.TextSplitterStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;  // ✅ 변경
import org.springframework.ai.vectorstore.VectorStore;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    // ✅ EmbeddingClient → EmbeddingModel 변경
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final EmbeddingStoreManager embeddingStoreManager;
    private final TextSplitterStrategy textSplitterStrategy;
    private final EmbedRepository embedRepository;
    private final QnaEmbeddingCleanupService qnaEmbeddingCleanupService;
    @Qualifier("taskExecutor")
    private final TaskExecutor taskExecutor;


    @Value("${embedding.csv.batch.size}")
    private int embeddingCsvBatchSize;
    @Value("${embedding.pdf.batch.size}")
    private int embeddingPdfBatchSize;

    @Value("${embedding.retry.max-attempts}")
    private int maxRetryAttempts;
    @Value("${embedding.retry.initial-delay-ms}")
    private long initialRetryDelayMs;

    public CompletableFuture<Void> embedAndStore(String text) {
        return CompletableFuture.runAsync(() -> {
            log.info("텍스트 임베딩 및 저장 시작: '{}'", text);

            List<Document> documents = textSplitterStrategy.split(text).stream()
                    .map(Document::new)
                    .toList();

            // ✅ VectorStore가 자동으로 임베딩 생성
            vectorStore.add(documents);

            log.info("텍스트 임베딩 및 저장 완료.");
        }, taskExecutor);
    }

    public CompletableFuture<Void> reset() {
        return CompletableFuture.runAsync(() -> {
            log.info("임베딩 스토어 재설정 및 새 텍스트 임베딩 시작.");
            embeddingStoreManager.reset();
            log.info("임베딩 스토어 재설정 및 새 텍스트 임베딩 완료.");
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

    public CompletableFuture<Void> embedAndStoreCsv(Path csvPath, String embedKey) {
        return CompletableFuture.runAsync(() -> {
            log.info("CSV 병렬 임베딩 시작 (batchSize={}): {}",
                    embeddingCsvBatchSize, csvPath.getFileName());

            try (InputStream is = Files.newInputStream(csvPath);
                 BOMInputStream bomIn = new BOMInputStream(
                         is, ByteOrderMark.UTF_8,
                         ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE)) {

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

                    String documentText = String.format("제목: %s\n내용: %s", title, content);

                    List<Document> segments = textSplitterStrategy.split(documentText)
                            .stream()
                            .map(Document::new)
                            .toList();

                    for (Document segment : segments) {
                        buffer.add(segment);
                        if (buffer.size() >= embeddingCsvBatchSize) {
                            flushBatch(buffer, embedKey, "CSV", chunkCounter);
                        }
                    }
                }

                if (!buffer.isEmpty()) {
                    flushBatch(buffer, embedKey, "CSV", chunkCounter);
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

                List<Document> segments = textSplitterStrategy.split(embeddingText)
                        .stream()
                        .map(Document::new)
                        .toList();

                List<Document> buffer = new ArrayList<>(embeddingPdfBatchSize);
                AtomicInteger chunkCounter = new AtomicInteger(1);

                for (Document segment : segments) {
                    buffer.add(segment);
                    if (buffer.size() >= embeddingPdfBatchSize) {
                        flushBatch(buffer, embedKey, "PDF", chunkCounter);
                    }
                }

                if (!buffer.isEmpty()) {
                    flushBatch(buffer, embedKey, "PDF", chunkCounter);
                }

                log.info("PDF 병렬 임베딩 완료 (embedKey={})", embedKey);

            } catch (Exception e) {
                log.error("PDF 임베딩 실패", e);
                throw new RuntimeException(e);
            }
        }, taskExecutor);
    }

    private void flushBatch(
            List<Document> batch,
            String embedKey,
            String source,
            AtomicInteger chunkCounter
    ) {
        // ✅ 메타데이터 추가 후 VectorStore에 추가 (자동 임베딩)
        for (int i = 0; i < batch.size(); i++) {
            Document doc = batch.get(i);
            doc.getMetadata().put("embedKey", embedKey);
            doc.getMetadata().put("source", source);
            doc.getMetadata().put("chunk", chunkCounter.getAndIncrement());
        }

        vectorStore.add(batch);
        batch.clear();
    }

    public String embedQna(String text, Long qnaId) {
        String key = UUID.randomUUID().toString();

        try {
            List<Document> documents = textSplitterStrategy.split(text)
                    .stream()
                    .map(Document::new)
                    .toList();

            // ✅ 수동으로 임베딩이 필요한 경우
            List<String> ids = new ArrayList<>();
            List<String> texts = new ArrayList<>();
            List<JSONObject> metadatas = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();

            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);

                // ✅ getText() 또는 getFormattedContent() 사용
                float[] embeddingArray = embeddingModel.embed(doc.getFormattedContent());

                // float[]를 List<Float>로 변환
                List<Float> vector = new ArrayList<>(embeddingArray.length);
                for (float f : embeddingArray) {
                    vector.add(f);
                }

                ids.add(UUID.randomUUID().toString());
                texts.add(doc.getFormattedContent());  // ✅ 여기도 수정

                com.alibaba.fastjson2.JSONObject metadata = new com.alibaba.fastjson2.JSONObject();
                metadata.put("key", key);
                metadata.put("chunk", i + 1);
                metadata.putAll(doc.getMetadata());

                metadatas.add(metadata);
                vectors.add(vector);
            }

            embeddingStoreManager.addDocuments(ids, texts, metadatas, vectors);

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
        cleanupByQnaId(qnaId);

        Embed oldEmbed = embedRepository.findByQnaId(qnaId).orElse(null);
        String oldEmbedKey = oldEmbed != null ? oldEmbed.getEmbedKey() : null;

        final String newEmbedKey = embedQna(text, qnaId);

        try {
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
        List<QnaEmbeddingCleanup> cleanups = qnaEmbeddingCleanupService.findByQnaId(qnaId);

        for (QnaEmbeddingCleanup cleanup : cleanups) {
            boolean deleted = deleteEmbeddingByKey(cleanup.getEmbedKey());
            if (deleted) {
                qnaEmbeddingCleanupService.delete(cleanup);
            }
        }
    }
}