package com.codehows.taelimbe.ai.chat.dto;

import com.codehows.taelimbe.ai.chat.constant.SenderType;
import com.codehows.taelimbe.ai.chat.entity.AiChat;
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
    private SenderType senderType;
    private String rawMessage;
    private LocalDateTime createdAt;
    private Long messageIndex;
    private Long userId;
    private String userName;


    public static AiChatDTO from(AiChat aiChat) {
        return AiChatDTO.builder()
                .aiChatId(aiChat.getAiChatId())
                .conversationId(aiChat.getConversationId())
                .senderType(aiChat.getSenderType())
                .rawMessage(aiChat.getRawMessage())
                .createdAt(aiChat.getCreatedAt())
                .messageIndex(aiChat.getMessageIndex())
                .userId(aiChat.getUser() != null ? aiChat.getUser().getUserId() : null)
                .userName(aiChat.getUser() != null ? aiChat.getUser().getName() : null)
                .build();
    }
}
