package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.entity.AiReport;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportDTO {

    private Long aiReportId;
    private String conversationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private String rawMessage;
    private String rawReport;
    private Long userId;  // User 엔티티 대신 ID만 전달

    // Entity -> DTO 변환
    public static AiReportDTO from(AiReport aiReport) {
        return AiReportDTO.builder()
                .aiReportId(aiReport.getAiReportId())
                .conversationId(aiReport.getConversationId())
                .startTime(aiReport.getStartTime())
                .endTime(aiReport.getEndTime())
                .createdAt(aiReport.getCreatedAt())
                .rawMessage(aiReport.getRawMessage())
                .rawReport(aiReport.getRawReport())
                .userId(aiReport.getUser() != null ? aiReport.getUser().getUserId() : null)
                .build();
    }
}