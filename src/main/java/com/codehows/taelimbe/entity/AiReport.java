package com.codehows.taelimbe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_report")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AiReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_history_id")
    private Long reportHistoryId;

    @Column(name = "conversation_id", length = 10)
    private String conversationId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "raw_report", columnDefinition = "TEXT")
    private String rawReport;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
