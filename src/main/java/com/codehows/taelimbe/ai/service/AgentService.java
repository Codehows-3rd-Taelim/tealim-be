package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.langchain.Agent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI 에이전트와의 대화 로직을 캡슐화하는 서비스 클래스입니다.
 * `AgentController`의 복잡성을 줄이고, 대화 처리와 관련된 모든 로직을 이곳에서 관리합니다.
 * `@Service` 어노테이션은 이 클래스가 비즈니스 계층의 컴포넌트임을 나타냅니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 * `@Slf4j`는 Lombok 어노테이션으로, 로깅을 위한 `log` 객체를 자동으로 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {
    private final SseService sseService;
    private final AiChatService aiChatService;

    // LangChain4j Agent 인터페이스의 구현체를 주입받습니다.
    @Qualifier("reportAgent")
    private final Agent reportAgent;

    @Qualifier("chatAgent")
    private final Agent chatAgent;


    @Async
    public void process(String conversationId, String message, Long userId) {

        log.info("🔍 [process] START conversationId={}, userId={}, msg={}", conversationId, userId, message);

        // 1) 사용자 메시지 저장
        aiChatService.saveUserMessage(conversationId, userId, message);

        // 2) TokenStream 가져오기
        TokenStream stream = chatAgent.chat(message, conversationId);

        StringBuilder aiBuilder = new StringBuilder();

        // 3) 토큰 스트리밍 시작
        stream.onNext(token -> {
                    log.info("🔍 token = {}", token);
                    aiBuilder.append(token);
                    sseService.send(conversationId, token);
                })
                .onComplete(finalResponse -> {
                    log.info("🔍 [process] onComplete 호출됨");
                    aiChatService.saveAiMessage(conversationId, userId, aiBuilder.toString());
                })
                .onError(e -> {
                    log.error("AI 스트림 오류", e);
                })
                .start();  
    }
    public SseEmitter report(ChatPromptRequest req, Long userId) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 현재 스레드에 사용자 이름을 설정하여, 도구 호출 등에서 사용자 컨텍스트를 활용할 수 있도록 합니다.
        // 대화 ID가 요청에 포함되어 있지 않다면 새로운 ID를 생성합니다.
        String convId = (req.getConversationId() == null || req.getConversationId().isBlank())
                ? UUID.randomUUID().toString()
                : req.getConversationId();

        PromptTemplate template = PromptTemplate.from("""
                  제공받은 데이터셋을 분석하여, 전체 요약과 상세 보고서를 모두 포함하는 마크다운 형식의 리포트를 생성하세요.\\n\\
                  리포트는 다음 항목을 포함해야 합니다:\\n\\
                  \\n\\
                  # 총괄 요약\\n\\
                  - 데이터의 핵심 인사이트와 결론 요약\\n\\
                  \\n\\
                  # 상세 분석\\n\\
                  - 섹션별 상세 분석\\n\\
                  - 표와 리스트, 필요시 그래프 링크 포함 가능\\n\\
                  \\n\\
                  # 결론 및 제언\\n\\
                  - 데이터 기반의 결론과 향후 조치/추천 사항\\n\\
                  \\n\\
                  **참고**:\\n\\
                  - 항상 Markdown 형식 사용 (헤더, 리스트, 표, 코드블록 등)\\n\\
                  - 요약은 주요 포인트를 간결하게\\n\\
                  - 상세 분석은 항목별로 구체적 내용을 포함\\n\\
                  \\n\\
                  이제 다음의 질문에 답변해주세요.\\n\\
                  {{question}}
                """); // 설정 값 사용

        Prompt prompt = template.apply(Map.of("question", req.getMessage()));

        createEmitter(emitter, convId, reportAgent, prompt.text(), userId);

        return emitter;
    }

    @Async("taskExecutor")
    protected void createChatEmitter(
            SseEmitter emitter,
            String convId,
            Agent agent, // chatAgent
            String prompt) {

        // DB에 저장할 AI 응답 전체를 담을 StringBuilder
        StringBuilder aiResponseBuilder = new StringBuilder();

        try {
            // 1. [전처리] 사용자 질문 저장 및 메모리 로드
            aiChatService.saveMessage(convId, "user", prompt);
            aiChatService.loadChatMemory(convId); // LangChain4j 메모리 복원

            // Agent의 chat 메서드를 호출하여 Gemini 모델과 상호작용합니다.
            TokenStream tokenStream = agent.chat(prompt, convId);

            // 첫 응답으로 대화 ID를 전송합니다.
            emitter.send(SseEmitter.event().name("conversationId").data(convId));

            // 스트리밍 응답의 각 토큰을 처리합니다.
            tokenStream.onNext(token -> {
                        try {
                            // AI 응답을 스트리밍 버퍼에 추가 (후처리용)
                            aiResponseBuilder.append(token);
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            log.error("SSE 토큰 전송 중 오류 발생 : {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    })
                    // 스트리밍 완료 시 emitter를 완료합니다.
                    .onComplete(response -> {
                        // 2. [후처리] AI 응답 전체를 모아서 DB에 저장
                        aiChatService.saveMessage(convId, "ai", aiResponseBuilder.toString());
                        emitter.complete();
                    })
                    // 스트리밍 중 오류 발생 시 emitter를 오류와 함께 완료합니다.
                    .onError(emitter::completeWithError)
                    // 스트리밍을 시작합니다.
                    .start();

        } catch (Exception e) {
            log.error("채팅 처리 중 오류 발생: {}", e.getMessage(), e);
        }finally {
            // 요청 처리 완료 후 스레드 로컬에 저장된 사용자 정보를 제거합니다.
            emitter.complete();
        }
    }

    @Async("taskExecutor")
    protected void createEmitter(
            SseEmitter emitter,
            String convId,
            Agent agent,
            String prompt,
            Long userId) {

        try {
            // Agent의 chat 메서드를 호출하여 Gemini 모델과 상호작용합니다.
            // 스트리밍 방식으로 응답을 받으며, 각 토큰을 클라이언트에게 전송합니다.
            TokenStream tokenStream = agent.chat(prompt, convId);

            // AI 메시지 누적 버퍼
            StringBuilder aiBuilder = new StringBuilder();

            // 첫 응답으로 대화 ID를 전송합니다.
            emitter.send(SseEmitter.event().name("conversationId").data(convId));

            // 스트리밍 응답의 각 토큰을 처리합니다.
            tokenStream.onNext(token -> {
                        try {
                            aiBuilder.append(token);
                            // 각 토큰을 SSE 이벤트로 클라이언트에게 전송합니다.
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            // 토큰 전송 중 오류 발생 시 emitter를 오류와 함께 완료합니다.
                            log.error("SSE 토큰 전송 중 오류 발생: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    })
                    // 스트리밍 완료 시 AI 메시지 저장만 수행합니다.
                    .onComplete(response -> {
                        aiChatService.saveAiMessage(convId, userId, aiBuilder.toString());

                        // 스트림 정상 종료
                        emitter.complete();

                    })
                    // 스트리밍 중 오류 발생 시 emitter를 오류와 함께 완료합니다.
                    .onError(emitter::completeWithError)
                    // 스트리밍을 시작합니다.
                    .start();

        } catch (Exception e) {
            // 예외 발생 시 emitter를 오류와 함께 완료합니다.
            log.error("채팅 처리 중 오류 발생: {}", e.getMessage(), e);
            emitter.completeWithError(e);
//        } finally {
//            // 요청 처리 완료 후 스레드 로컬에 저장된 사용자 정보를 제거합니다.
//            emitter.complete();   // ← 네가 원한 그대로 유지
        }
    }
}
