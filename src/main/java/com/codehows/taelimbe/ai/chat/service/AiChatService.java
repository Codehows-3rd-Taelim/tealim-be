package com.codehows.taelimbe.ai.chat.service;

import com.codehows.taelimbe.ai.chat.constant.SenderType;
import com.codehows.taelimbe.ai.chat.dto.AiChatDTO;
import com.codehows.taelimbe.ai.chat.entity.AiChat;
import com.codehows.taelimbe.ai.chat.repository.AiChatRepository;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AiChatService {

    private final AiChatRepository aiChatRepository;
    private final UserRepository userRepository;

    public String startNewChat(Authentication authentication) {
        return UUID.randomUUID().toString();
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
