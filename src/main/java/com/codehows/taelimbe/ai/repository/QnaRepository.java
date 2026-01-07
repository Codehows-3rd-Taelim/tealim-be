package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.entity.Qna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    // 질문 중복 방지
    Optional<Qna> findByNormalizedText(String normalizedText);


    // 운영 기준
    List<Qna> findByResolved(boolean resolved);

    // 시스템 기준 (QnA)
    List<Qna> findByStatus(QnaStatus status);

    // 파일/정책 처리된 질문
    @Query("""
        select q
        from Qna qQ
        where q.resolved = true
          and (q.status is null or q.status <> 'APPLIED')
    """)
    List<Qna> findResolvedWithoutQna();

    // 유저 본인 QnA 전체
    List<Qna> findByUserId(Long userId);

    // 유저 본인 + resolved 조건
    List<Qna> findByUserIdAndResolved(Long userId, boolean resolved);

    // 유저 본인 + status 조건
    List<Qna> findByUserIdAndStatus(Long userId, QnaStatus status);
}
