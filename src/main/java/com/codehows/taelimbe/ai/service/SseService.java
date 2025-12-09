package com.codehows.taelimbe.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    // conversationId → emitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * conversationId 기준으로 emitter 생성 + 저장
     */
    public SseEmitter createEmitter(String conversationId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout 무제한
        emitters.put(conversationId, emitter);

        emitter.onCompletion(() -> emitters.remove(conversationId));
        emitter.onTimeout(() -> emitters.remove(conversationId));
        emitter.onError((e) -> emitters.remove(conversationId));

        return emitter;
    }

    /**
     * emitter 가져오기
     */
    public SseEmitter get(String conversationId) {
        return emitters.get(conversationId);
    }

    /**
     * SSE 이벤트 전송
     */
    public void send(String conversationId, String data) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (Exception e) {
                emitters.remove(conversationId);
            }
        }
    }
}
