package com.codehows.taelimbe.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationService {

    // userId → emitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout 없음
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        log.info("Notification SSE connected. userId={}", userId);
        return emitter;
    }

    // AI 챗 완료 알림
    public void notifyAiChatDone(Long userId, String conversationId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("AI_CHAT_DONE")
                            .data(Map.of("conversationId", conversationId))
            );
        } catch (Exception e) {
            emitters.remove(userId);
        }
    }



    // AI 보고서 완료 알림
    public void notifyAiReportDone(Long userId, String reportId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("AI_REPORT_DONE")
                            .data(Map.of("reportId", reportId))
            );
        } catch (Exception e) {
            emitters.remove(userId);
        }
    }

    // 보고서 생성 실패
    public void notifyAiReportFailed(Long userId, String reason) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("AI_REPORT_FAILED")
                            .data(Map.of(
                                    "message", reason
                            ))
            );
        } catch (Exception e) {
            emitters.remove(userId);
        }
    }

    // heartbeat (연결 유지용)
    @Scheduled(fixedRate = 30000) // 30초
    public void heartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("PING")
                                .data("ping")
                );
            } catch (Exception e) {
                emitters.remove(userId);
            }
        });
    }
}
