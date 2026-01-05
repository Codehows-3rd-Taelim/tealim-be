package com.codehows.taelimbe.ai.constant;

public enum EmbedFileStatus {
    UPLOADED,    // 파일 업로드 완료 (아직 임베딩 안함)
    EMBEDDING,   // 임베딩 진행 중
    DONE,        // 임베딩 완료
    FAILED       // 임베딩 실패
}
