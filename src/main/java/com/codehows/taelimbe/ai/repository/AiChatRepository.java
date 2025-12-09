package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.AiChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatRepository extends JpaRepository<AiChat, Long> {

    List<AiChat> findByUser_UserId(Long userId);

    // 특정 대화 ID와 사용자 ID로 메시지 수를 계산합니다.
    Long countByConversationIdAndUser_UserId(String conversationId, Long userId);

    // 특정 대화 ID와 사용자 ID로 메시지를 순서대로 조회합니다.
    List<AiChat> findByConversationIdAndUser_UserIdOrderByMessageIndexAsc(String conversationId, Long userId);

}