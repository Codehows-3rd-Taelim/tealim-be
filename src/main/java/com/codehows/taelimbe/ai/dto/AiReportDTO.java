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

    // from으로 가져오면 rawReport까지 get 해야해서 용량이 너무 많아진다 rawReport는 따로 관리
    // 목록 조회 최적화하기 위해 AiReportMetaProjection 라는걸 따로 만들어서 DTO에서 빼오는겨
    public static AiReportDTO fromProjection(AiReportMetaProjection projection) {
        return AiReportDTO.builder()
                .aiReportId(projection.getAiReportId())
                .conversationId(projection.getConversationId())
                .startTime(projection.getStartTime().toLocalDate())
                .endTime(projection.getEndTime().toLocalDate())
                .createdAt(projection.getCreatedAt())
                .rawMessage(projection.getRawMessage())
                .name(projection.getName())
                .build();
    }

}