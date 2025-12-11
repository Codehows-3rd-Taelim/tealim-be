package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.entity.AiReport;

import com.codehows.taelimbe.ai.repository.AiReportMetaProjection;
import com.codehows.taelimbe.ai.repository.RawReportProjection;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiReportDTO {

    private Long aiReportId;
    private String conversationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
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

    // from으로 가져오면 rawReport까지 get 해야해서 용량이 너무 많아진다 rawReport는 따로 관리
    public static AiReportDTO fromProjection(AiReportMetaProjection projection) { // 🚨 AiReportMetaDTO -> AiReportMetaProjection
        return AiReportDTO.builder()
                .aiReportId(projection.getAiReportId())
                .conversationId(projection.getConversationId())
                .startTime(projection.getStartTime())
                .endTime(projection.getEndTime())
                .createdAt(projection.getCreatedAt())
                .rawMessage(projection.getRawMessage())
                .name(projection.getName())
                .build();
    }
}