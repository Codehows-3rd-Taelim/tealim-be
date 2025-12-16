package com.codehows.taelimbe.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;
    // AI_CHAT_DONE, AI_REPORT_DONE, AI_REPORT_FAILED

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /* ===== 읽음 처리 ===== */
    public void markAsRead() {
        this.isRead = true;
    }
}
