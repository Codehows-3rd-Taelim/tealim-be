package com.codehows.taelimbe.ai.repository; // Repository와 같은 패키지에 두는 것이 일반적입니다.

import java.time.LocalDateTime;

public interface AiReportMetaProjection {
    Long getAiReportId();
    String getConversationId();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    LocalDateTime getCreatedAt();
    String getRawMessage(); // DTO와 동일하게 포함
    String getName();
}