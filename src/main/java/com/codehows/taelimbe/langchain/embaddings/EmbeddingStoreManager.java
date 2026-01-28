package com.codehows.taelimbe.langchain.embaddings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * pgvector 기반 벡터 저장소 관리 컴포넌트.
 * 기존 Milvus SDK 대신 Spring AI VectorStore + JdbcTemplate을 사용합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmbeddingStoreManager {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 벡터 저장소를 재설정합니다 (모든 문서 삭제).
     */
    public void reset() {
        log.info("pgvector 벡터 저장소 재설정 시도");

        try {
            // vector_store 테이블의 모든 데이터 삭제
            jdbcTemplate.execute("DELETE FROM vector_store");
            log.info("pgvector 벡터 저장소 재설정 완료");
        } catch (Exception e) {
            log.error("pgvector 벡터 저장소 재설정 중 오류 발생", e);
            throw new RuntimeException("pgvector 벡터 저장소 재설정 실패", e);
        }
    }

    /**
     * 벡터 데이터를 추가합니다 (QnA 임베딩용).
     */
    public void addDocuments(List<Document> documents) {
        log.info("pgvector 벡터 데이터 저장 시작 ({}건)", documents.size());

        try {
            vectorStore.add(documents);
            log.info("pgvector 벡터 데이터 저장 완료");
        } catch (Exception e) {
            log.error("pgvector 벡터 데이터 저장 중 오류 발생", e);
            throw new RuntimeException("pgvector 데이터 저장 실패", e);
        }
    }

    /**
     * 특정 key에 해당하는 벡터 데이터를 삭제합니다.
     */
    public boolean deleteDocuments(String key) {
        log.info("pgvector 데이터 삭제 시작 (key={})", key);

        try {
            // metadata에서 key 또는 embedKey로 ID 조회
            List<String> ids = jdbcTemplate.queryForList(
                    "SELECT id::text FROM vector_store WHERE metadata->>'key' = ? OR metadata->>'embedKey' = ?",
                    String.class,
                    key, key
            );

            if (ids.isEmpty()) {
                log.info("삭제 대상 없음 (이미 삭제됨) key={}", key);
                return true;
            }

            // VectorStore의 delete 메서드로 삭제
            vectorStore.delete(ids);

            log.info("pgvector 데이터 삭제 완료 ({}건, key={})", ids.size(), key);
            return true;

        } catch (Exception e) {
            log.warn("pgvector 데이터 삭제 중 불확실 오류 (key={})", key, e);
            return false;
        }
    }
}
