package com.codehows.taelimbe.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    // conversationId → emitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결 생성 중복 생성 방지)
    public SseEmitter createEmitter(String conversationId) {
        return emitters.computeIfAbsent(conversationId, id -> {
            SseEmitter emitter = new SseEmitter(0L); // timeout 없음

            emitter.onCompletion(() -> {
                emitters.remove(id);
                log.debug("SSE completed: {}", id);
            });

            emitter.onTimeout(() -> {
                emitters.remove(id);
                log.warn("SSE timeout: {}", id);
            });

            emitter.onError(e -> {
                emitters.remove(id);
                log.warn("SSE error: {}", id, e);
            });

            return emitter;
        });
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
}
