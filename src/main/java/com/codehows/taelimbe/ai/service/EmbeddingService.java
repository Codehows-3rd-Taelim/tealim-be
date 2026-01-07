package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.entity.Embed;
import com.codehows.taelimbe.ai.entity.QnaEmbeddingCleanup;
import com.codehows.taelimbe.ai.repository.EmbedRepository;
import com.codehows.taelimbe.langchain.converters.PdfEmbeddingNormalizer;
import com.codehows.taelimbe.langchain.embaddings.EmbeddingStoreManager;
import com.codehows.taelimbe.langchain.embaddings.TextSplitterStrategy;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 텍스트 임베딩 및 벡터 저장소 관리를 담당하는 서비스입니다.
 * `@Service` 어노테이션은 이 클래스가 비즈니스 계층의 컴포넌트임을 나타내며,
 * Spring 컨테이너에 의해 관리되는 빈으로 등록됩니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 * `@Slf4j`는 Lombok 어노테이션으로, 로깅을 위한 `log` 객체를 자동으로 생성합니다.
 */
@Service
@Slf4j
public class EmbeddingService {


    // 텍스트를 임베딩 벡터로 변환하는 모델을 주입받습니다.
    private final EmbeddingModel embeddingModel;
    // 병렬 임베딩 모델 (CSV / PDF 전용)
    private final EmbeddingModel parallelEmbeddingModel;
    // 생성된 임베딩 벡터를 저장하고 검색하는 스토어를 주입받습니다.
    private final EmbeddingStore<TextSegment> embeddingStore;
    // 임베딩 스토어의 초기화 및 관리 기능을 제공하는 매니저를 주입받습니다.
    private final EmbeddingStoreManager embeddingStoreManager;
    // 텍스트 분할 전략을 주입받습니다.
    private final TextSplitterStrategy textSplitterStrategy;

    private final EmbedRepository embedRepository;

    private final QnaEmbeddingCleanupService qnaEmbeddingCleanupService;


    // 비동기 작업을 위한 스레드 풀을 주입받습니다.
    @Qualifier("taskExecutor")
    private final TaskExecutor taskExecutor;

    public EmbeddingService(
            @Qualifier("lcEmbeddingModel") EmbeddingModel embeddingModel,
            @Qualifier("parallelEmbeddingModel") EmbeddingModel parallelEmbeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingStoreManager embeddingStoreManager,
            TextSplitterStrategy textSplitterStrategy,
            EmbedRepository embedRepository, QnaEmbeddingCleanupService qnaEmbeddingCleanupService,
            @Qualifier("taskExecutor") TaskExecutor taskExecutor
    ) {
        this.embeddingModel = embeddingModel;
        this.parallelEmbeddingModel = parallelEmbeddingModel;
        this.embeddingStore = embeddingStore;
        this.embeddingStoreManager = embeddingStoreManager;
        this.textSplitterStrategy = textSplitterStrategy;
        this.embedRepository = embedRepository;
        this.qnaEmbeddingCleanupService = qnaEmbeddingCleanupService;
        this.taskExecutor = taskExecutor;
    }


    // csv, pdf 임베딩 각 배치사이즈 값 받아오기
    @Value("${embedding.csv.batch.size}")
    private int embeddingCsvBatchSize;
    @Value("${embedding.pdf.batch.size}")
    private int embeddingPdfBatchSize;

    // 임베딩 batch 실패 시 재시도 설정값 받아오기
    @Value("${embedding.retry.max-attempts}")
    private int maxRetryAttempts;
    @Value("${embedding.retry.initial-delay-ms}")
    private long initialRetryDelayMs;

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



    public void resetOnly() {
        embeddingStoreManager.reset();
    }


    /**
     * CSV 파일을 받아 파싱하고 내용을 임베딩하여 벡터 저장소에 추가합니다.
     * @param file 임베딩할 데이터가 포함된 CSV 파일
     * @return 비동기 작업의 완료를 나타내는 `CompletableFuture<Void>`
     */
    public CompletableFuture<Void> embedAndStoreCsv(MultipartFile file, String embedKey) {
        try {
            byte[] fileBytes = file.getBytes();

            return CompletableFuture.runAsync(() -> {
                log.info("CSV 병렬 임베딩 시작 (batchSize={}): {}", embeddingCsvBatchSize, file.getOriginalFilename());

                try (BOMInputStream bomIn = new BOMInputStream(new ByteArrayInputStream(fileBytes));
                     Reader reader = new InputStreamReader(bomIn, Charset.forName("MS949"))) {

                    Iterable<CSVRecord> records = CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                            .withIgnoreHeaderCase()
                            .withTrim()
                            .withIgnoreEmptyLines()
                            .withAllowMissingColumnNames()
                            .parse(reader);

                    List<TextSegment> buffer = new ArrayList<>(embeddingCsvBatchSize);
                    AtomicInteger chunkCounter = new AtomicInteger(1);

                    for (CSVRecord record : records) {

                        String title = record.get("title").trim();
                        String content = record.get("content").replace("\r\n", "\n").trim();
                        String documentText = String.format("제목: %s\n내용: %s", title, content);

                        List<TextSegment> segments = textSplitterStrategy.split(documentText)
                                .stream()
                                .map(TextSegment::from)
                                .toList();

                        for (TextSegment segment : segments) {
                            buffer.add(segment);

                            if (buffer.size() >= embeddingCsvBatchSize) {
                                flushBatch(buffer, embedKey, "CSV", chunkCounter);
                            }
                        }
                    }

                    if (!buffer.isEmpty()) {
                        flushBatch(buffer, embedKey, "CSV", chunkCounter);
                    }

                    log.info("CSV 병렬 임베딩 완료");

                } catch (Exception e) {
                    log.error("CSV 병렬 임베딩 실패", e);
                    throw new RuntimeException(e);
                }

            }, taskExecutor);

        } catch (Exception e) {
            log.error("CSV 파일 읽기 실패", e);
            throw new RuntimeException(e);
        }
    }



    public CompletableFuture<Void> embedAndStorePdf(MultipartFile file, String embedKey) {
        try {
            byte[] pdfBytes = file.getBytes();

            return CompletableFuture.runAsync(() -> {
                log.info("PDF 병렬 임베딩 시작 (batchSize={}): {}", embeddingPdfBatchSize, file.getOriginalFilename());

                try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {

                    PDFTextStripper stripper = new PDFTextStripper();
                    String rawText = stripper.getText(document);

                    String embeddingText = PdfEmbeddingNormalizer.normalize(rawText, file.getOriginalFilename());

                    List<TextSegment> segments = textSplitterStrategy.split(embeddingText)
                            .stream()
                            .map(TextSegment::from)
                            .toList();

                    List<TextSegment> buffer = new ArrayList<>(embeddingPdfBatchSize);
                    AtomicInteger chunkCounter = new AtomicInteger(1);

                    for (TextSegment segment : segments) {
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

        } catch (Exception e) {
            log.error("PDF 파일 읽기 실패", e);
            throw new RuntimeException(e);
        }
    }


    private void flushBatch(
            List<TextSegment> batch,
            String embedKey,
            String source,
            AtomicInteger chunkCounter

    ) {
        Response<List<Embedding>> embeddings =
                parallelEmbeddingModel.embedAll(batch);

        for (int i = 0; i < batch.size(); i++) {
            Metadata metadata = new Metadata();
            metadata.put("embedKey", embedKey);
            metadata.put("source", source);
            metadata.put("chunk", chunkCounter.getAndIncrement());

            TextSegment segmentWithMeta =
                    TextSegment.from(batch.get(i).text(), metadata);

            embeddingStore.add(
                    embeddings.content().get(i),
                    segmentWithMeta
            );

        }

        batch.clear();
    }



//    private void flushBatch(List<TextSegment> batch) {
//
//        int attempt = 0;
//        long delay = initialRetryDelayMs;
//
//        while (true) {
//            try {
//                log.info("Embedding batch flush size={}, attempt={}", batch.size(), attempt + 1);
//
//                Response<List<Embedding>> embeddings =
//                        parallelEmbeddingModel.embedAll(batch);
//
//                embeddingStore.addAll(embeddings.content(), batch);
//                batch.clear();
//                return;
//
//            } catch (Exception e) {
//                attempt++;
//
//                if (attempt >= maxRetryAttempts) {
//                    log.error(" Embedding batch failed after {} attempts. Skip batch.", attempt, e);
//                    batch.clear();
//                    return;
//                }
//
//                log.warn("⚠ Embedding batch failed. Retry in {} ms (attempt {}/{})",
//                        delay, attempt, maxRetryAttempts);
//
//                try {
//                    Thread.sleep(delay);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    batch.clear();
//                    return;
//                }
//
//                delay *= 2; // exponential backoff
//            }
//        }
//    }



    // QnA 임베딩
    public String embedQna(String text, Long qnaId) {

        String key = UUID.randomUUID().toString();

        try {
            List<TextSegment> segments =
                    textSplitterStrategy.split(text)
                            .stream()
                            .map(TextSegment::from)
                            .toList();

            Response<List<Embedding>> embeddingResponse =
                    embeddingModel.embedAll(segments);

            List<String> ids = new ArrayList<>();
            List<String> texts = new ArrayList<>();
            List<com.alibaba.fastjson.JSONObject> metadatas = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();

            for (int i = 0; i < segments.size(); i++) {
                ids.add(UUID.randomUUID().toString());
                texts.add(segments.get(i).text());

                com.alibaba.fastjson.JSONObject metadata = new com.alibaba.fastjson.JSONObject();
                metadata.put("key", key);
                metadata.put("chunk", i + 1);

                metadatas.add(metadata);
                vectors.add(embeddingResponse.content().get(i).vectorAsList());
            }

            // 1️ Milvus 저장
            embeddingStoreManager.addDocuments(ids, texts, metadatas, vectors);

            // 2️ DB 저장
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

        // 1️ 이전 cleanup 정리
        cleanupByQnaId(qnaId);

        // 2️ 기존 임베딩 조회
        Embed oldEmbed = embedRepository.findByQnaId(qnaId).orElse(null);
        String oldEmbedKey = oldEmbed != null ? oldEmbed.getEmbedKey() : null;

        // 3️ 새 임베딩 생성
        final String newEmbedKey = embedQna(text, qnaId);


        try {
            // 4️ 기존 임베딩 삭제
            if (oldEmbedKey != null) {
                boolean deleted =
                        embeddingStoreManager.deleteDocuments(oldEmbedKey);

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

            boolean deleted =
                    deleteEmbeddingByKey(cleanup.getEmbedKey());

            if (deleted) {
                qnaEmbeddingCleanupService.delete(cleanup);
            }
        }
    }






}
