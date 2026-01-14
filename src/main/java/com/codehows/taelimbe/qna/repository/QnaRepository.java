package com.codehows.taelimbe.qna.repository;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.qna.entity.Qna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    Optional<Qna> findByIdAndDeletedAtIsNull(Long id);


    // 전체 (활성) 최신순
    Page<Qna> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    // 처리 / 미처리 최신순
    Page<Qna> findByResolvedAndDeletedAtIsNullOrderByCreatedAtDesc(
            boolean resolved,
            Pageable pageable
    );


    // 유저 전체 최신순
    Page<Qna> findByUser_UserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    // 유저 처리 / 미처리 최신순
    Page<Qna> findByUser_UserIdAndResolvedAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            boolean resolved,
            Pageable pageable
    );


    // 비활성 질문 (삭제 최신순)
    Page<Qna> findByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable pageable);

    

    Page<Qna> findByUser_UserIdAndResolvedTrueAndStatusIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

}
