package com.codehows.taelimbe.notification.dto;

import com.codehows.taelimbe.notification.constant.NotificationType;
import com.codehows.taelimbe.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;



@Getter
@AllArgsConstructor
public class NotificationDTO {

    private Long notificationId;
    private NotificationType type;
    private String message;
    private LocalDateTime createdAt;
    // deliveredAt: 토스트가 사용자 화면에 실제 표시된 시점 (Toast = deliveredAt만 설정)
    private LocalDateTime deliveredAt;
    // readAt: 사용자가 알림 목록에서 명시적으로 확인한 시점
    private LocalDateTime readAt;

    public static NotificationDTO from(Notification n) {
        return new NotificationDTO(
                n.getNotificationId(),
                n.getType(),
                n.getMessage(),
                n.getCreatedAt(),
                n.getDeliveredAt(),
                n.getReadAt()
        );
    }
}
