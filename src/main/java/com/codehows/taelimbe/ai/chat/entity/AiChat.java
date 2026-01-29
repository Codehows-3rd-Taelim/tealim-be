package com.codehows.taelimbe.ai.chat.entity;

import com.codehows.taelimbe.ai.chat.constant.SenderType;
import com.codehows.taelimbe.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AiChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_chat_id")
    private Long aiChatId;

    @Column(name = "conversation_id", length = 100, nullable = false)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @Column(name = "raw_message", columnDefinition = "TEXT", nullable = false)
    private String rawMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "message_index", nullable = false)
    private Long messageIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 엔티티 생성 시 자동으로 생성 시간 설정
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
