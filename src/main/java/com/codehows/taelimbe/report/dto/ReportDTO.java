package com.codehows.taelimbe.report.dto;

import com.codehows.taelimbe.report.entity.Report;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Report 응답 DTO
 *
 * 용도:
 * - Report 조회 API의 응답 데이터
 * - Report Entity를 클라이언트로 전달할 때 사용
 *
 * 주요 필드:
 * - reportId: Report 고유 ID
 * - status: Report 상태
 * - startTime/endTime: 청소 시작/종료 시간
 * - cleanTime: 청소 소요 시간
 * - taskArea/cleanArea: 작업 면적/청소 면적
 * - mode: 청소 모드
 * - costBattery/costWater: 배터리/물 소비량
 * - mapName/mapUrl: 맵 이름/URL
 * - robotId: 로봇 ID
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDTO {

    private Long reportId;
    private Long taskId;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Float cleanTime;
    private Float taskArea;
    private Float cleanArea;

    private Integer mode;
    private Long costBattery;
    private Long costWater;

    private String mapName;
    private String mapUrl;

    private Long robotId;

    /**
     * Report Entity를 DTO로 변환하는 정적 팩토리 메서드
     * @param report Report 엔티티
     * @return 변환된 ReportDTO
     */
    public static ReportDTO createReportDTO(Report report) {
        return ReportDTO.builder()
                .reportId(report.getReportId())
                .taskId(report.getTaskId())
                .status(report.getStatus())
                .startTime(report.getStartTime())
                .endTime(report.getEndTime())
                .cleanTime(report.getCleanTime())
                .taskArea(report.getTaskArea())
                .cleanArea(report.getCleanArea())
                .mode(report.getMode())
                .costBattery(report.getCostBattery())
                .costWater(report.getCostWater())
                .mapName(report.getMapName())
                .mapUrl(report.getMapUrl())
                .robotId(report.getRobot().getRobotId())
                .build();
    }
}