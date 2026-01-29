package com.codehows.taelimbe.ai.chat.service;

import com.codehows.taelimbe.ai.common.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 채팅 Agent 서비스입니다.
 * VectorStore를 통한 RAG(Retrieval-Augmented Generation)를 수행합니다.
 */
@Service
@Slf4j
public class ChatAgentService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final AiChatService aiChatService;
    private final SseService sseService;

    @Value("${rag.max-results:2}")
    private int ragMaxResults;

    @Value("${rag.min-score:0.5}")
    private double ragMinScore;

    @Value("${chat.max-messages:20}")
    private int chatMaxMessages;

    private static final String SYSTEM_MESSAGE = """
            당신은 사용자의 문제 해결을 돕는 AI입니다.
            사용자의 질문이 범위가 넓거나 질문의 범위가 모호할때는 더 자세하게 질문할 수 있도록 유도하세요.
            확실하지 않은 정보이거나 확인 가능한 자료에 명시적으로 없는 내용에 대해서는
            다음 가이드를 따라 친절히 답변하세요.


            1. 사용자의 질문이 단답형인 경우 답변을 더 하지 않고 더 구체적인 질문을 요청하세요.
            예를들어, "인어스트리"라고만 질문할 경우 "인어스트리의 어떤 점에 대해 알려드릴까요?"
            같이 안내하고 다음 질문을 기다리세요.
            질문이 단답형이 아닌, 의도가 명확한 경우에만 다음 가이드에 따라 답변하세요.

            2. 질문자에 의도가 명확하고 질문에 대해서 명확한 답을 준 경우에는 해당 질문에 대한 정보를 제공하고 맨 마지막에

            "\\n\\n다른 더 궁금하신 정보 있으신가요?" 물어보고,
             해당 주어에 대한 다른 정보가 있는 경우에는
            "\\nㅇㅇ(질문주어)에 대한 @@@@ 정보 등도 있습니다."
            라고 안내해줄수 있는 다른 정보를 길게 늘어놓지말고 키워드만 1~3개 정도 간략하게만 추천해서 답변하세요.

            해당 주어에 대한 다른 정보가 없는 경우에는
            "\\n다른 더 궁금하신 정보 있으신가요?" 라고만 답변하세요.

            3. 질문자의 의도가 명확하나, 제공 가능한 정보가 확실하지 않은 정보이거나
            확인 가능한 자료에 명시적으로 존재하지 않는 내용에 대해서는 단정하지 말고,
            "현재 확인 가능한 정보 기준으로는" 이렇게 표현을 사용하세요.
            질문에 대한 답을 제시할 수 없는 경우에는 간단히 한계를 밝힌 뒤
            질문자가 문제 해결을 위해 일반적으로 다음에 취할 수 있는 행동을 안내하세요.(제품 설명서 참조, 인어스트리에 고객센터에 문의, Q&A에 질문 등록)
            사용자에게 제대로 된 답을 주지 못한 경우에 문장 마지막에 예를 들면 ㅇㅇ(질문주어)에 대한거였으면
            "\\nㅇㅇ의 ㅁㅁㅁㅁ나 ㅁㅁㅁㅁ에 대한 정보 등은 안내해드릴 수 있습니다."
            라고 다른 정보를 길게 늘어놓지 말고 1~3개 정도의 키워드만 간략하게 추천해줘라
            "\\n다른 궁금하신 정보가 있으시다면 질문해주세요"
            질문 주어에 대한 안내해줄 다른 키워드가 없다면
            "\\n다른 궁금하신 정보가 더 있으시다면 질문해주세요"

            4. 추측이 필요한 경우에는 그 사실을 명확히 밝히세요.
            어떤 정보가 제공되어 있는지 말하지 마세요.
            """;

    public ChatAgentService(
            ChatModel chatModel,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            AiChatService aiChatService,
            SseService sseService
    ) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
        this.vectorStore = vectorStore;
        this.aiChatService = aiChatService;
        this.sseService = sseService;
    }

    @Async
    public void process(String conversationId, String message, Long userId) {
        // 사용자 메시지 저장
        aiChatService.saveUserMessage(conversationId, userId, message);

        try {
            // RAG: 벡터 스토어에서 관련 컨텍스트 검색
            String context = retrieveContext(message);

            // 컨텍스트가 있으면 메시지에 추가
            String augmentedMessage = context.isEmpty()
                    ? message
                    : "참고 정보:\n" + context + "\n\n사용자 질문: " + message;

            // Spring AI ChatClient로 스트리밍 호출
            Flux<String> responseFlux = chatClient.prompt()
                    .system(SYSTEM_MESSAGE)
                    .user(augmentedMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content();

            StringBuilder aiBuilder = new StringBuilder();

            responseFlux
                    .doOnNext(aiBuilder::append)
                    .doOnComplete(() -> {
                        String rawAnswer = aiBuilder.toString();

                        aiChatService.saveAiMessage(
                                conversationId,
                                userId,
                                rawAnswer
                        );

                        sseService.sendFinalAndComplete(conversationId, rawAnswer);
                    })
                    .doOnError(e -> {
                        log.error("AI 스트림 오류", e);
                        sseService.completeWithError(conversationId, e);
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("AI 처리 오류", e);
            sseService.completeWithError(conversationId, e);
        }
    }

    /**
     * 벡터 스토어에서 쿼리와 관련된 컨텍스트를 검색합니다.
     */
    private String retrieveContext(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(ragMaxResults)
                    .similarityThreshold(ragMinScore)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            if (results == null || results.isEmpty()) {
                return "";
            }

            return results.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("벡터 검색 실패", e);
            return "";
        }
    }
}
