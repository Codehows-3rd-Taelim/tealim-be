package com.codehows.taelimbe.ai.repository;

// import com.codehows.taelimbe.ai.dto.aiReport.AiReportMetaDTO; ⬅️ DTO 임포트 제거
// import com.codehows.taelimbe.ai.dto.aiReport.RawReportDTO; ⬅️ DTO 임포트 제거

import com.codehows.taelimbe.ai.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    // 💡 Projection을 위한 쿼리 (AS 별칭 사용)
    final String BASE_SELECT_QUERY =
            "SELECT a.aiReportId as aiReportId, a.conversationId as conversationId, a.startTime as startTime, a.endTime as endTime, a.createdAt as createdAt, a.rawMessage as rawMessage, a.user.name as name ";

    // 1. 특정 Store의 모든 리포트 조회 (반환 타입: AiReportMetaProjection)
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a WHERE a.user.store.storeId = :storeId ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findMetaByStoreIdOrderByCreatedAtDesc(@Param("storeId") Long storeId);


    // 2. 모든 리포트 조회 (반환 타입: AiReportMetaProjection)
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findAllMetaOrderByCreatedAtDesc();

    // 3. RawReport만 조회 (반환 타입: RawReportProjection)
    // 쿼리: a.rawReport as rawReport -> RawReportProjection.getRawReport()에 매핑
    @Query("SELECT a.rawReport as rawReport FROM AiReport a WHERE a.aiReportId = :reportId")
    Optional<RawReportProjection> findRawReportById(@Param("reportId") Long reportId);
}