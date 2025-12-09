package com.codehows.taelimbe.ai.dto;

import com.codehows.taelimbe.ai.entity.AiChat;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiChatDTO {

    private Long aiChatId;
    private String conversationId;
    private LocalDateTime createdAt;
    private String senderType;
    private String rawMessage;
    private Long messageIndex;
    private Long userId;

    public static AiChatDTO from(AiChat aiChat) {
        return AiChatDTO.builder()
                .aiChatId(aiChat.getAiChatId())
                .conversationId(aiChat.getConversationId())
                .createdAt(aiChat.getCreatedAt())
                .senderType(aiChat.getSenderType())
                .rawMessage(aiChat.getRawMessage())
                .messageIndex(aiChat.getMessageIndex())
                .userId(aiChat.getUser().getUserId())
                .build();
    }
}