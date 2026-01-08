package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.entity.Qna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {


    // 운영 기준
    List<Qna> findByResolvedAndDeletedAtIsNull(boolean resolved);

    // 단건
    Optional<Qna> findByIdAndDeletedAtIsNull(Long id);

    // 관리자용 (선택)
    Optional<Qna> findById(Long id);


    // 시스템 기준 (QnA)
    List<Qna> findByStatusAndDeletedAtIsNull(QnaStatus status);

    // 유저 본인 QnA 전체
    List<Qna> findByUser_UserIdAndDeletedAtIsNull(Long userId);

    List<Qna> findByUser_UserIdAndResolvedAndDeletedAtIsNull(Long userId, boolean resolved);

    List<Qna> findByUser_UserIdAndStatusAndDeletedAtIsNull(Long userId, QnaStatus status);

}
