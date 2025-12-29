package com.codehows.taelimbe.ai.constant;


 // QnA 답변 상태
 // EDITING : 관리자 수정 중 (아직 적용 안 됨)
 // APPLIED : 사용자에게 적용 완료
 // FAILED  : 적용 실패 (기존 applied 유지)

public enum QnaStatus {
    APPLIED,
    FAILED
}
