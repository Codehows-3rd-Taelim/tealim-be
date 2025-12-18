package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.entity.AiReport;

import com.codehows.taelimbe.ai.repository.AiReportMetaProjection;

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

//지금은 데이버 베이스에 저장하는 용도로 사용중
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