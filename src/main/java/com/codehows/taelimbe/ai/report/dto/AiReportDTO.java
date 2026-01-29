package com.codehows.taelimbe.ai.report.dto;

import com.codehows.taelimbe.ai.report.entity.AiReport;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiReportDTO {

    private Long aiReportId;
    private String conversationId;
    private LocalDate startTime;
    private LocalDate endTime;
    private LocalDateTime createdAt;
    private String rawReport;
    private String rawMessage;
    private Long userId;
    private String name;

    public static AiReportDTO from(AiReport aiReport) {
        return AiReportDTO.builder()
                .aiReportId(aiReport.getAiReportId())
                .conversationId(aiReport.getConversationId())
                .startTime(aiReport.getStartTime())
                .endTime(aiReport.getEndTime())
                .createdAt(aiReport.getCreatedAt())
                .rawMessage(aiReport.getRawMessage())
                .rawReport(aiReport.getRawReport())
                .userId(aiReport.getUser().getUserId())
                .name(aiReport.getUser().getName())
                .build();
    }
}
