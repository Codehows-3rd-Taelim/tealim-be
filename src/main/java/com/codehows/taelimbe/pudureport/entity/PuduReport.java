package com.codehows.taelimbe.pudureport.entity;

import com.codehows.taelimbe.robot.entity.Robot;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pudu_report")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuduReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pudu_report_id")
    private Long puduReportId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "clean_time")
    private Float cleanTime;

    @Column(name = "task_area")
    private Float taskArea;

    @Column(name = "clean_area")
    private Float cleanArea;

    @Column(name = "mode")
    private Integer mode;

    @Column(name = "cost_battery")
    private Long costBattery;

    @Column(name = "cost_water")
    private Long costWater;

    @Column(name = "map_name", length = 255)
    private String mapName;

    @Column(name = "map_url", length = 255)
    private String mapUrl;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @ManyToOne
    @JoinColumn(name = "robot_id")
    private Robot robot;

    public void updateRemark(String remark) {
        this.remark = remark;
    }

}