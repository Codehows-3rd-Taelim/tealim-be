package com.codehows.taelimbe.notification.service;

import com.codehows.taelimbe.notification.constant.NotificationType;
import com.codehows.taelimbe.notification.entity.Notification;
import com.codehows.taelimbe.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /* ===== SSE 연결 ===== */
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }

    /* ===== 알림 생성  ===== */
    public void notify(Long userId, NotificationType type, String message) {

        // 1️ DB 저장
        notificationRepository.save(
                Notification.builder()
                        .userId(userId)
                        .type(type)
                        .message(message)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // 2⃣ SSE는 신호만
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("NOTIFICATION")
                            .data("ping")
            );
        } catch (Exception e) {
            emitters.remove(userId);
        }
    }

    /* ===== 토스트 노출 완료 ===== */
    @Transactional
    public void markDelivered(Long notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(n -> {
                    if (n.getDeliveredAt() == null) {
                        n.setDeliveredAt(LocalDateTime.now());
                    }
                });
    }
}
