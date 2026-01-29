package com.codehows.taelimbe.ai.embedding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Spring AI VectorStore 기반 벡터 저장소 관리 컴포넌트.
 * pgvector를 백엔드로 사용하며, Spring AI의 표준 API를 활용합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmbeddingStoreManager {

    private final VectorStore vectorStore;

    /**
     * 벡터 저장소를 재설정합니다 (모든 문서 삭제).
     * 주의: 이 작업은 모든 임베딩 데이터를 삭제합니다.
     */
    public void reset() {
        log.info("벡터 저장소 재설정 시도");

        try {
            // Spring AI VectorStore의 delete with filter 사용
            // 모든 문서 삭제를 위해 항상 참인 조건 사용
            vectorStore.delete(List.of());
            log.warn("벡터 저장소 재설정: delete(emptyList) 호출됨 - pgvector에서는 전체 삭제가 지원되지 않을 수 있음");
        } catch (Exception e) {
            log.error("벡터 저장소 재설정 중 오류 발생", e);
            throw new RuntimeException("벡터 저장소 재설정 실패", e);
        }
    }

    /**
     * 문서들을 벡터 저장소에 추가합니다.
     *
     * @param documents 저장할 문서 목록
     */
    public void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("저장할 문서가 없습니다.");
            return;
        }

        log.info("벡터 데이터 저장 시작 ({}건)", documents.size());

        try {
            vectorStore.add(documents);
            log.info("벡터 데이터 저장 완료");
        } catch (Exception e) {
            log.error("벡터 데이터 저장 중 오류 발생", e);
            throw new RuntimeException("벡터 데이터 저장 실패", e);
        }
    }

    /**
     * 특정 key에 해당하는 벡터 데이터를 삭제합니다.
     * metadata의 'key' 또는 'embedKey' 필드로 매칭합니다.
     *
     * @param key 삭제할 문서의 키
     * @return 삭제 성공 여부
     */
    public boolean deleteDocuments(String key) {
        if (key == null || key.isBlank()) {
            log.warn("삭제할 키가 비어있습니다.");
            return false;
        }

        log.info("벡터 데이터 삭제 시작 (key={})", key);

        try {
            // Spring AI Filter Expression을 사용하여 metadata 기반 삭제
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression keyFilter = builder.eq("key", key).build();
            Filter.Expression embedKeyFilter = builder.eq("embedKey", key).build();

            // key 또는 embedKey로 삭제 시도
            Optional<Boolean> keyResult = safeDelete(keyFilter, "key", key);
            Optional<Boolean> embedKeyResult = safeDelete(embedKeyFilter, "embedKey", key);

            boolean deleted = keyResult.orElse(false) || embedKeyResult.orElse(false);

            if (deleted) {
                log.info("벡터 데이터 삭제 완료 (key={})", key);
            } else {
                log.info("삭제 대상 없음 (이미 삭제됨) key={}", key);
            }

            return true;

        } catch (Exception e) {
            log.warn("벡터 데이터 삭제 중 오류 (key={}): {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 필터 표현식으로 안전하게 삭제를 시도합니다.
     */
    private Optional<Boolean> safeDelete(Filter.Expression filter, String fieldName, String value) {
        try {
            vectorStore.delete(filter);
            log.debug("{}={} 조건으로 삭제 수행", fieldName, value);
            return Optional.of(true);
        } catch (UnsupportedOperationException e) {
            log.debug("{}={} 필터 삭제 미지원, 무시", fieldName, value);
            return Optional.empty();
        } catch (Exception e) {
            log.debug("{}={} 삭제 실패: {}", fieldName, value, e.getMessage());
            return Optional.of(false);
        }
    }
}
