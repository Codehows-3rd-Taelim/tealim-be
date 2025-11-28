package com.codehows.taelimbe.langchain.models;

import com.codehows.taelimbe.langchain.converters.MessageConverter;
import com.codehows.taelimbe.langchain.converters.ToolConverter;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.google.gson.Gson;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LangChain4j의 `StreamingChatLanguageModel` 인터페이스를 `com.google.genai` 클라이언트를 사용하여 구현한 커스텀 클래스입니다.
 * 이 클래스는 LangChain4j 프레임워크가 Google의 제네릭 Gemini 클라이언트를 통해 스트리밍 채팅 및 함수 호출을 수행할 수 있도록 하는 어댑터 역할을 합니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 * `@Builder`는 Lombok 어노테이션으로, 빌더 패턴을 사용하여 객체를 생성할 수 있도록 합니다.
 */
@RequiredArgsConstructor
@Builder
public class GeminiStreamingChatModel implements StreamingChatLanguageModel {

    // Google Gemini API 클라이언트 주입
    private final Client geminiClient;
    // 사용할 Gemini 채팅 모델의 이름
    private final String modelName;
    // JSON 직렬화/역직렬화를 위한 GSON 인스턴스
    private final Gson gson = new Gson();

    /**
     * LangChain4j 메시지 리스트와 도구 스펙을 받아 스트리밍 방식으로 응답을 생성합니다.
     * 이 메서드는 Gemini API의 `generateContentStream`을 호출하여 실시간으로 응답을 처리합니다.
     *
     * @param messages LangChain4j의 `ChatMessage` 리스트 (대화 기록)
     * @param toolSpecifications LangChain4j의 `ToolSpecification` 리스트 (AI가 사용할 수 있는 도구 명세)
     * @param handler 스트리밍 응답을 처리할 `StreamingResponseHandler<AiMessage>`
     */
    @Override
    public void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) {
        List<Content> googleAiMessages = MessageConverter.toGoogleAiMessages(messages);
        List<Tool> googleAiTools = ToolConverter.toGoogleAiTools(toolSpecifications);

        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(googleAiTools)
                .build();

        AtomicReference<StringBuilder> contentBuilder = new AtomicReference<>(new StringBuilder());
        AtomicReference<ToolExecutionRequest> toolExecutionRequestRef = new AtomicReference<>(null);

        System.out.println("=====[Agent Calling]=====");
        geminiClient.models.generateContentStream(modelName, googleAiMessages, config)
        .forEach(response -> {
            if (response.candidates().isPresent()) {
                Candidate candidate = response.candidates().orElse(Collections.emptyList()).getFirst();
                candidate.content().flatMap(Content::parts).orElse(Collections.emptyList()).forEach(part -> {
                try {
                    if (part.text().isPresent()) {
                        String token = part.text().get();
                        System.out.println(token);
                        contentBuilder.get().append(token);
                        handler.onNext(token);
                    } else if (part.functionCall().isPresent()) {
                        FunctionCall functionCall = part.functionCall().get();
                        if (functionCall.name().isPresent() && functionCall.args().isPresent()) {
                            System.out.println("=====[Tool Calling]=====");
                            System.out.println("=====[Tool Name: " + functionCall.name().get() + "]=====");
                            System.out.println("=====[Tool Args: " + functionCall.args().get() + "]=====");
                            String rawArgs = gson.toJson(functionCall.args().orElse(Collections.emptyMap()));
                            ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                                    .name(functionCall.name().orElse(""))
                                    .arguments(rawArgs)
                                    .build();
                            toolExecutionRequestRef.set(toolExecutionRequest);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Exception while processing streaming content: " + e.getMessage());
                }
                });
            }
        });
        // 스트리밍 완료 후 응답 처리
        if (toolExecutionRequestRef.get() != null) {
            handler.onComplete(Response.from(AiMessage.from(toolExecutionRequestRef.get())));
        } else {
            handler.onComplete(Response.from(AiMessage.from(contentBuilder.get().toString())));
        }
    }

    /**
     * LangChain4j 메시지 리스트를 받아 스트리밍 방식으로 응답을 생성합니다.
     * 이 메서드는 도구 사용이 없는 경우에 호출됩니다.
     * 내부적으로 `toolSpecifications`가 빈 리스트인 `generate` 메서드를 호출합니다.
     *
     * @param messages LangChain4j의 `ChatMessage` 리스트
     * @param handler 스트리밍 응답을 처리할 핸들러
     */
    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        // 도구 명세가 없는 경우, 빈 리스트를 전달하여 기존 `generate` 메서드를 호출합니다.
        generate(messages, Collections.emptyList(), handler);
    }
}