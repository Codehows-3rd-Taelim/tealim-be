package com.codehows.taelimbe.ai.report.repository;

import com.codehows.taelimbe.ai.report.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    /* =========== Entity 기반 조회 (AiReportService에서 사용) =========== */

    // 본인 작성 보고서 조회
    List<AiReport> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

    Optional<AiReport> findByConversationId(String conversationId);

    /* =========== Raw Report 조회 =========== */

    @Query("SELECT a.rawReport as rawReport FROM AiReport a WHERE a.aiReportId = :reportId")
    Optional<RawReportProjection> findRawReportById(@Param("reportId") Long reportId);
}
