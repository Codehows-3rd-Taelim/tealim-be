package com.codehows.taelimbe.ai.constant;

public enum QnaEmbeddingFailPoint {
    NONE,
    BEFORE_INSERT, // 새 Milvus insert 직전
    AFTER_INSERT,       // Milvus insert 직후
    BEFORE_DELETE,     // 기존 임베딩 삭제 전
    AFTER_DELETE,      // 기존 임베딩 삭제 후


}
