package com.codehows.taelimbe.ai.entity;

import com.codehows.taelimbe.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_report")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AiReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_report_id")
    private Long aiReportId;

    @Column(name = "conversation_id", length = 100)
    private String conversationId;

    @Column(name = "start_time")
    private LocalDate startTime;

    @Column(name = "end_time")
    private LocalDate endTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "raw_message")
    private String rawMessage;

    @Column(name = "raw_report", columnDefinition = "TEXT")
    private String rawReport;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
