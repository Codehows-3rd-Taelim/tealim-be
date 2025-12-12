package com.codehows.taelimbe.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    // conversationId → emitter 매핑
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();


    // SSE 연결 생성
    public SseEmitter createEmitter(String conversationId) {
        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음
        emitters.put(conversationId, emitter);

        emitter.onCompletion(() -> emitters.remove(conversationId));
        emitter.onTimeout(() -> emitters.remove(conversationId));
        emitter.onError((e) -> emitters.remove(conversationId));

        return emitter;
    }


    // SSE 메시지 전송
    public void send(String conversationId, String data) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (Exception e) {
            emitters.remove(conversationId);
        }
    }
}
