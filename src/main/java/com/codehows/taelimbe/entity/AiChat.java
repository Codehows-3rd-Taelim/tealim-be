//package com.codehows.taelimbe.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "ai_chat")
//@NoArgsConstructor
//@AllArgsConstructor
//@Getter
//@Setter
//@Builder
//public class AiChat {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "chat_history_id")
//    private Long chatHistoryId;
//
//    @Column(name = "conversation_id", length = 10)
//    private String conversationId;
//
//    @Column(name = "sender_type", length = 10)
//    private String senderType;
//
//    @Column(name = "raw_message", columnDefinition = "TEXT")
//    private String rawMessage;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "message_index")
//    private Long messageIndex;
//
//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
//
//}
