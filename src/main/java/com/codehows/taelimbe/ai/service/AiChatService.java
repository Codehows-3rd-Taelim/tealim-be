package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.AiChatDTO;
import com.codehows.taelimbe.ai.entity.AiChat;
import com.codehows.taelimbe.ai.repository.AiChatRepository;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiChatService {

    private final AiChatRepository aiChatRepository;
    private final UserRepository userRepository;
    private final ChatMemoryProvider chatMemoryProvider;

    // 현재 로그인한 유저 정보 가져오기
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    // 본인 리포팅 조회
    public List<AiChatDTO> getAllChats() {
        Long userId = getCurrentUser().getUserId();

        return aiChatRepository.findByUser_UserId(userId)
                .stream()
                .map(AiChatDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 새로운 메시지를 데이터베이스에 저장합니다.
     * messageIndex를 결정하기 위해 현재 conversationId의 메시지 수를 계산합니다.
     *
     * @param conversationId 대화 ID
     * @param senderType     송신자 타입 ("user" 또는 "ai")
     * @param rawMessage     원본 메시지 텍스트
     * @return 저장된 AiChat 엔티티
     */
    @Transactional
    public AiChat saveMessage(String conversationId, String senderType, String rawMessage) {
        User currentUser = getCurrentUser();
        System.out.println(currentUser.getUsername());
        LocalDateTime now = LocalDateTime.now();

        // 현재 대화 ID의 메시지 수를 세어 새로운 messageIndex를 결정합니다.
        Long nextMessageIndex = aiChatRepository.countByConversationIdAndUser_UserId(conversationId, currentUser.getUserId()) + 1;

        AiChat aiChat = AiChat.builder()
                .conversationId(conversationId)
                .senderType(senderType)
                .rawMessage(rawMessage)
                .createdAt(now)
                .messageIndex(nextMessageIndex)
                .user(currentUser)
                .build();

        return aiChatRepository.save(aiChat);
    }

    /**
     * 특정 대화 ID의 기존 채팅 기록을 불러와 LangChain4j의 ChatMemory에 복원합니다.
     * * @param conversationId 대화 ID
     * @return 복원된 ChatMemory (MessageWindowChatMemory)
     */
    public MessageWindowChatMemory loadChatMemory(String conversationId) {
        // ChatMemoryProvider를 통해 ChatMemory 인스턴스를 가져옵니다.
        MessageWindowChatMemory chatMemory = (MessageWindowChatMemory) chatMemoryProvider.get(conversationId);

        // 현재 LangChain4j 메모리에 메시지가 없다면 DB에서 불러와 채웁니다.
        if (chatMemory.messages().isEmpty()) {
            Long userId = getCurrentUser().getUserId();
            List<AiChat> chats = aiChatRepository.findByConversationIdAndUser_UserIdOrderByMessageIndexAsc(conversationId, userId);

            for (AiChat chat : chats) {
                ChatMessage message;

                // senderType에 따라 LangChain4j 메시지 객체로 변환
                if ("user".equalsIgnoreCase(chat.getSenderType())) {
                    message = UserMessage.from(chat.getRawMessage());
                } else if ("ai".equalsIgnoreCase(chat.getSenderType())) {
                    message = AiMessage.from(chat.getRawMessage());
                } else {
                    // System 메시지 등 다른 타입은 기본적으로 User 메시지로 처리하거나 무시
                    log.warn("Unknown senderType: {} in conversation {}", chat.getSenderType(), conversationId);
                    continue;
                }

                // ChatMemory에 메시지를 추가합니다.
                chatMemory.add(message);
            }
        }
        return chatMemory;
    }
}