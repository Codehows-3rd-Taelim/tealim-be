package com.codehows.taelimbe.ai.repository; // Repository와 같은 패키지에 두는 것이 일반적입니다.

import java.time.LocalDateTime;

public interface AiReportMetaProjection {
    // Getter 메소드 이름은 쿼리의 AS 별칭과 정확히 일치해야 합니다.
    Long getAiReportId();
    String getConversationId();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    LocalDateTime getCreatedAt();
    String getRawMessage(); // DTO와 동일하게 포함
    String getName();
}