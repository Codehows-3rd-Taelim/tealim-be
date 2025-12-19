package com.codehows.taelimbe.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    // conversationId → emitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결 생성 중복 생성 방지)
    public SseEmitter createEmitter(String conversationId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout 없음

        emitters.put(conversationId, emitter);

        log.info("SSE emitter created. conversationId={}, emitter={}",
                conversationId, System.identityHashCode(emitter));

        emitter.onCompletion(() -> {
            emitters.remove(conversationId);
            log.info("SSE completed. conversationId={}", conversationId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(conversationId);
            log.warn("SSE timeout. conversationId={}", conversationId);
        });

        emitter.onError(e -> {
            emitters.remove(conversationId);
            log.warn("SSE error. conversationId={}", conversationId, e);
        });

        return emitter;
    }

    // 토큰 스트리밍 전송 ( event name : message) */
    public void send(String conversationId, String data) {
        sendEvent(conversationId, "message", data);
    }

    // named event 전송
    public void sendEvent(String conversationId, String event, Object data) {
        SseEmitter emitter = emitters.get(conversationId);

        if (emitter == null) {
            log.debug("SSE emitter not found. conversationId={}, event={}", conversationId, event);
            return;
        }

        try {
            log.info("📤 Sending SSE event: {} -> {}", conversationId, event);
            emitter.send(
                    SseEmitter.event()
                            .name(event)
                            .data(data)
            );
        } catch (IOException e) {
            // 연결 끊김 / 네트워크 오류가 대부분
            emitters.remove(conversationId);
            log.warn("SSE send failed (IO). conversationId={}, event={}", conversationId, event, e);
        } catch (IllegalStateException e) {
            // 이미 complete 된 emitter에 send 시도할 때
            emitters.remove(conversationId);
            log.warn("SSE send failed (state). conversationId={}, event={}", conversationId, event, e);
        } catch (Exception e) {
            emitters.remove(conversationId);
            log.error("SSE send failed. conversationId={}, event={}", conversationId, event, e);
        }
    }

    // SSE 정상 종료
    public void complete(String conversationId) {
        SseEmitter emitter = emitters.remove(conversationId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        }
    }

    // SSE 에러 종료
    public void completeWithError(String conversationId, Throwable error) {
        SseEmitter emitter = emitters.remove(conversationId);
        if (emitter != null) {
            try {
                emitter.completeWithError(error);
            } catch (Exception ignored) {}
        }
    }

    // 한 번 보내고 종료
    public void sendOnceAndComplete(
            String conversationId,
            String event,
            Object data
    ) {
        sendEvent(conversationId, event, data);
        complete(conversationId);
    }

    @Scheduled(fixedDelay = 30000) // 30초
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<String, SseEmitter>> iterator = emitters.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, SseEmitter> entry = iterator.next();
            String conversationId = entry.getKey();

            if (!conversationId.startsWith("report-")) {
                continue;
            }

            try {
                entry.getValue().send(
                        SseEmitter.event()
                                .name("heartbeat")
                                .data("keep-alive")
                );
            } catch (Exception e) {
                iterator.remove();
                log.info("Heartbeat failed, emitter removed. conversationId={}", entry.getKey());
            }
        }
    }

}
