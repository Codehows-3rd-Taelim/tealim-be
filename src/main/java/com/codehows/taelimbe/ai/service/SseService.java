package com.codehows.taelimbe.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    // conversationId → emitter 매핑
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();


    // SSE 연결 생성
    public SseEmitter createEmitter(String conversationId) {
        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음
        emitters.put(conversationId, emitter);

        emitter.onCompletion(() -> emitters.remove(conversationId));
        emitter.onTimeout(() -> {
            log.warn("SSE Emitter Timeout: {}", conversationId);
            emitter.complete(); // 타임아웃 시 명시적 종료
            emitters.remove(conversationId);
        });
        // 에러 발생 시 Emitter 제거
        emitter.onError((e) -> {
            log.error("SSE Emitter Error: {}", conversationId, e);
            emitters.remove(conversationId);
        });

        return emitter;
    }


    // SSE 메시지 전송
    public void send(String conversationId, String data) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter == null) return;

        try {
            // LangChain4j의 응답 토큰을 'message' 이벤트로 전송
            emitter.send(SseEmitter.event().data(data));
        } catch (Exception e) {
            log.error("SSE 데이터 전송 중 오류 발생: {}", conversationId, e);
            emitters.remove(conversationId);
        }
    }

    public void sendEvent(String conversationId, String event, Object data) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter == null) return;

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(event)
                            .data(data)
            );
        } catch (Exception e) {
            log.error("SSE event send error", e);
            emitters.remove(conversationId);
        }
    }


    // SSE 연결을 명시적으로 종료
    public void complete(String conversationId) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    // SSE 연결을 오류와 함께 종료
    public void completeWithError(String conversationId, Throwable error) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter != null) {
            emitter.completeWithError(error);
            // onError 핸들러에 의해 emitters 맵에서 제거됩니다.
        }
    }
}
