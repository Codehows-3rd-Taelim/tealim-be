package com.codehows.taelimbe.notification.service;

import com.codehows.taelimbe.notification.constant.NotificationType;
import com.codehows.taelimbe.notification.dto.NotificationDTO;
import com.codehows.taelimbe.notification.entity.Notification;
import com.codehows.taelimbe.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;

    // 알림 생성
    public void notify(Long userId, NotificationType type, String message) {

        // DB 저장
        notificationRepository.save(
                Notification.builder()
                        .userId(userId)
                        .type(type)
                        .message(message)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // SSE는 신호
        notificationSseService.send(userId);
    }

    // 조회

    public List<NotificationDTO> getUndelivered(Long userId) {
        return notificationRepository
                .findByUserIdAndDeliveredAtIsNull(userId)
                .stream()
                .map(NotificationDTO::from)
                .toList();
    }

    public List<NotificationDTO> getAll(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::from)
                .toList();
    }

    //상태 변경

    @Transactional
    public void markDelivered(Long id) {
        notificationRepository.findById(id)
                .filter(n -> n.getDeliveredAt() == null)
                .ifPresent(n -> n.setDeliveredAt(LocalDateTime.now()));
    }

    @Transactional
    public void markRead(Long id) {
        notificationRepository.findById(id)
                .filter(n -> n.getReadAt() == null)
                .ifPresent(n -> n.setReadAt(LocalDateTime.now()));
    }
}
