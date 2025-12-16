package com.codehows.taelimbe.notification.service;

import com.codehows.taelimbe.notification.entity.Notification;
import com.codehows.taelimbe.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // userId → SseEmitter
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

    private void notifyInternal(Long userId, String type, String message) {

        log.info("🔥 NOTIFY TRY userId={}", userId);
        notificationRepository.save(
                Notification.builder()
                        .userId(userId)
                        .type(type)
                        .message(message)
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        SseEmitter emitter = emitters.get(userId);
        log.info("🔥 EMITTER EXISTS = {}", emitter != null);
        if (emitter == null) return;

        try {
            emitter.send(
                    SseEmitter.event().name("NOTIFICATION")   // ⭐ 이거 하나로 통일
            );

            log.info("🔥 SSE SENT userId={}", userId);
        } catch (Exception e) {
            emitters.remove(userId);
        }
    }


    /* ===== 외부에서 쓰는 메서드들 ===== */

    public void notifyAiChatDone(Long userId) {
        notifyInternal(
                userId,
                "AI_CHAT_DONE",
                "AI 챗봇 답변이 도착했습니다"
        );
    }

    public void notifyAiReportDone(Long userId) {
        notifyInternal(
                userId,
                "AI_REPORT_DONE",
                "AI 보고서 생성이 완료되었습니다"
        );
    }

    public void notifyAiReportFailed(Long userId, String reasonMessage) {
        notifyInternal(
                userId,
                "AI_REPORT_FAILED",
                reasonMessage
        );
    }
}
