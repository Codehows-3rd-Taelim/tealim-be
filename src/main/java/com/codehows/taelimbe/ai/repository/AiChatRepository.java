package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.AiChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatRepository extends JpaRepository<AiChat, Long> {

    // 특정 대화의 모든 메시지 조회 (메시지 순서대로)
    List<AiChat> findByConversationIdOrderByMessageIndex(String conversationId);

    // 특정 사용자의 모든 대화 조회
    List<AiChat> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // 특정 매장의 모든 대화 조회
    @Query("SELECT c FROM AiChat c WHERE c.user.store.storeId = :storeId ORDER BY c.createdAt DESC")
    List<AiChat> findByStoreIdOrderByCreatedAtDesc(@Param("storeId") Long storeId);

    // 특정 대화의 마지막 메시지 인덱스 조회
    @Query("SELECT COALESCE(MAX(c.messageIndex), 0) FROM AiChat c WHERE c.conversationId = :conversationId")
    Long findMaxMessageIndexByConversationId(@Param("conversationId") String conversationId);

    @Query("""
    SELECT c.conversationId
    FROM AiChat c
    WHERE c.user.userId = :userId
    GROUP BY c.conversationId
    ORDER BY MAX(c.createdAt) DESC
""")
    List<String> findConversationIdsByUserId(@Param("userId") Long userId);


    List<AiChat> findByConversationIdOrderByMessageIndexAsc(String conversationId);

    @Query("SELECT DISTINCT a.conversationId FROM AiChat a WHERE a.user.userId = :userId")
    List<String> findConversationIdsByUser(@Param("userId") Long userId);

    long countByConversationId(String conversationId);
}