package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.constant.SenderType;
import com.codehows.taelimbe.ai.dto.AiChatDTO;
import com.codehows.taelimbe.ai.entity.AiChat;
import com.codehows.taelimbe.ai.repository.AiChatRepository;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AiChatService {

    private final AiChatRepository aiChatRepository;
    private final UserRepository userRepository;

    // 현재 로그인한 사용자 정보 가져오기
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
    }


    /**
     * AI 응답 메시지 저장
     */
    public AiChat saveAgentMessage(String conversationId, String response) {
        User user = getCurrentUser();
        Long nextMessageIndex = aiChatRepository.findMaxMessageIndexByConversationId(conversationId) + 1;

        AiChat agentChat = AiChat.builder()
                .conversationId(conversationId)
                .senderType(SenderType.AI)
                .rawMessage(response)
                .messageIndex(nextMessageIndex)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        return aiChatRepository.save(agentChat);
    }

    /**
     * 특정 대화의 모든 메시지 조회
     */
    @Transactional(readOnly = true)
    public List<AiChatDTO> getChatHistory(String conversationId) {
        return aiChatRepository.findByConversationIdOrderByMessageIndex(conversationId)
                .stream()
                .map(AiChatDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 대화 목록 조회 (최신순)
     * 각 대화의 첫 번째 메시지를 대화 제목으로 사용
     */
    @Transactional(readOnly = true)
    public List<AiChatDTO> getUserChatList() {
        User user = getCurrentUser();
        List<String> conversationIds = aiChatRepository.findConversationIdsByUserId(user.getUserId());

        // 대화 자체가 하나도 없으면 빈 배열 반환
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }

        return conversationIds.stream()
                .map(convId -> {
                    List<AiChat> messages = aiChatRepository.findByConversationIdOrderByMessageIndex(convId);

                    // 메시지가 하나도 없으면 null 결과 대신 skip
                    if (messages == null || messages.isEmpty()) {
                        return null; // 이 뒤에 filter로 걸러짐
                    }

                    AiChat firstMessage = messages.stream()
                            .filter(msg -> msg.getSenderType() == SenderType.USER)
                            .findFirst()
                            .orElse(messages.get(0)); // 이제 안전함 (messages가 empty 아닌 상태로 들어오기 때문)

                    return AiChatDTO.from(firstMessage);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    /**
     * 특정 매장의 모든 대화 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public List<AiChatDTO> getStoreChatHistory(Long storeId) {
        return aiChatRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream()
                .map(AiChatDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 대화 메시지 삭제 (대화 전체 삭제)
     */
    public void deleteConversation(String conversationId) {
        List<AiChat> chats = aiChatRepository.findByConversationIdOrderByMessageIndex(conversationId);
        aiChatRepository.deleteAll(chats);
        log.info("대화 '{}' 삭제 완료", conversationId);
    }

    /**
     * 특정 메시지 삭제
     */
    public void deleteChatMessage(Long aiChatId) {
        aiChatRepository.deleteById(aiChatId);
        log.info("메시지 '{}' 삭제 완료", aiChatId);
    }

    /** USER 메시지 저장 */
    public void saveUserMessage(String convId, Long userId, String msg) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long idx = aiChatRepository.countByConversationId(convId);

        AiChat chat = AiChat.builder()
                .conversationId(convId)
                .senderType(SenderType.USER)
                .rawMessage(msg)
                .messageIndex(idx)
                .user(user)
                .build();

        aiChatRepository.save(chat);
    }

    /** AI 메시지 저장 */
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

    public List<AiChat> loadConversation(String convId) {
        return aiChatRepository.findByConversationIdOrderByMessageIndexAsc(convId);
    }

    public List<String> loadChatHistory(Long userId) {
        return aiChatRepository.findConversationIdsByUser(userId);
    }
}
