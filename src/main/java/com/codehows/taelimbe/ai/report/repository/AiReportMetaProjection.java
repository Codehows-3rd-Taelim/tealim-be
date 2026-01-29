package com.codehows.taelimbe.ai.report.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface AiReportMetaProjection {

    Long getAiReportId();
    String getConversationId();

    LocalDate getStartTime();
    LocalDate getEndTime();

    LocalDateTime getCreatedAt();
    String getRawMessage();
    String getName();
}
