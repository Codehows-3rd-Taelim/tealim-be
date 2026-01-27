package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.agent.ChatAgent;
import com.codehows.taelimbe.ai.constant.SenderType;
import com.codehows.taelimbe.ai.dto.AiChatDTO;
import com.codehows.taelimbe.ai.entity.AiChat;
import com.codehows.taelimbe.ai.repository.AiChatRepository;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AiChatService {

    private final ChatAgent chatAgent;
    private final AiChatRepository aiChatRepository;
    private final UserRepository userRepository;
    private final VectorStore vectorStore; // New for RAG
    private final SseService sseService;

    public String startNewChat(Authentication authentication) {
        return UUID.randomUUID().toString();
    }

    @Async
    public void process(String conversationId, String message, Long userId) {
        // 1. 사용자 메시지 저장
        this.saveUserMessage(conversationId, userId, message);

        // 3. RAG를 위한 컨텍스트 검색
        List<Document> similarDocuments = vectorStore.similaritySearch(message);
        String ragContext = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 4. System Message 구성 (RAG 컨텍스트 포함)
        String systemMessageContent = """
            You are a helpful AI assistant.
            Answer the question based on the following context and conversation history:
            Context: %s
            """.formatted(ragContext);

        // 5. Prompt 객체 생성
        // 기존 대화 기록, 시스템 메시지, 사용자 메시지를 포함합니다.
        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(systemMessageContent));
        // Using only current message for now, history is empty and will be re-added in step 4
        promptMessages.add(new UserMessage(message));


        // 6. Spring AI ChatClient 스트리밍 호출
        Flux<ChatResponse> stream = chatAgent.chat(promptMessages)
                .stream().chatResponse();

        StringBuilder aiBuilder = new StringBuilder();

        stream.doOnNext(chatResponse -> { // Parameter is chatResponse
                    String token = chatResponse.getResults().getFirst().getOutput().getText(); // Get content from the first Generation using getText()
                    aiBuilder.append(token);
                    sseService.sendEvent(conversationId, "message", token); // Temporarily commented out
                })
                .doOnComplete(() -> {
                    String finalAnswer = aiBuilder.toString();
                    this.saveAiMessage(conversationId, userId, finalAnswer);
                    sseService.sendFinalAndComplete(conversationId, finalAnswer); // Temporarily commented out
                })
                .doOnError(e -> {
                    log.error("AI 스트림 오류", e);
                    sseService.completeWithError(conversationId, e); // Temporarily commented out
                })
                .subscribe(); // 중요: Flux를 활성화하려면 subscribe()를 호출해야 합니다.
    }

    @Transactional(readOnly = true)
    public List<AiChatDTO> getChatHistory(String conversationId) {
        return aiChatRepository.findByConversationIdOrderByMessageIndex(conversationId)
                .stream()
                .map(AiChatDTO::from)
                .collect(Collectors.toList());
    }


    // 사용자의 대화 목록 조회 각 대화의 첫 번째 메시지를 대화 제목으로 사용
    @Transactional(readOnly = true)
    public List<AiChatDTO> getUserChatList(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        List<String> conversationIds = aiChatRepository.findConversationIdsByUserId(userId);

        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }

        return conversationIds.stream()
                .map(convId -> {
                    List<AiChat> messages = aiChatRepository.findByConversationIdOrderByMessageIndex(convId);
                    if (messages == null || messages.isEmpty()) return null;

                    AiChat firstMessage = messages.stream()
                            .filter(msg -> msg.getSenderType() == SenderType.USER)
                            .findFirst()
                            .orElse(messages.get(0));

                    return AiChatDTO.from(firstMessage);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    // USER 메시지 저장
    public void saveUserMessage(String convId, Long userId, String msg) {
        log.info("[saveUserMessage] START convId={}, userId={}, msg={}", convId, userId, msg);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long idx = aiChatRepository.countByConversationId(convId);
        log.info("[saveUserMessage] nextIndex={}", idx);

        AiChat chat = AiChat.builder()
                .conversationId(convId)
                .senderType(SenderType.USER)
                .rawMessage(msg)
                .messageIndex(idx)
                .user(user)
                .build();

        aiChatRepository.save(chat);
    }

    // AI 메시지 저장
    public void saveAiMessage(String convId, Long userId, String msg) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long idx = aiChatRepository.countByConversationId(convId);

        AiChat chat = AiChat.builder()
                .conversationId(convId)
                .senderType(SenderType.AI)
                .rawMessage(msg)
                .messageIndex(idx)
                .user(user)
                .build();

        aiChatRepository.save(chat);
    }
}
