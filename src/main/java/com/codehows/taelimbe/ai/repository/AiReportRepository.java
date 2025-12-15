package com.codehows.taelimbe.ai.repository;


import com.codehows.taelimbe.ai.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    /* =========== Entity 기반 조회 (AiReportService에서 사용) =========== */

    // 관리자 - 전체 보고서 조회
    List<AiReport> findAllByOrderByCreatedAtDesc();

    // 매니저/유저 - 매장 기준 보고서 조회
    @Query("""
        SELECT a
        FROM AiReport a
        WHERE a.user.store.storeId = :storeId
        ORDER BY a.createdAt DESC
    """)
    List<AiReport> findByStoreIdOrderByCreatedAtDesc(@Param("storeId") Long storeId);


    /* =========== Projection 기반 조회 (리스트/메타 정보용) =========== */

    String BASE_SELECT_QUERY =
            "SELECT a.aiReportId as aiReportId, " +
                    "a.conversationId as conversationId, " +
                    "a.startTime as startTime, " +
                    "a.endTime as endTime, " +
                    "a.createdAt as createdAt, " +
                    "a.rawMessage as rawMessage, " +
                    "a.user.name as name ";

    // 유저 권한 - 본인이 작성한 보고서만
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findMetaByUserId(@Param("userId") Long userId);

    // 매니저 권한 - 자기 매장 (ADMIN 제외)
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a " +
            "WHERE a.user.store.storeId = :storeId " +
            "AND a.user.role <> 'ADMIN' " +
            "ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findMetaByStoreExcludingAdmin(@Param("storeId") Long storeId);

    // 관리자 권한 - 전체 메타 조회
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findAllMetaOrderByCreatedAtDesc();


    /* =========== Raw Report 조회 =========== */

    @Query("SELECT a.rawReport as rawReport FROM AiReport a WHERE a.aiReportId = :reportId")
    Optional<RawReportProjection> findRawReportById(@Param("reportId") Long reportId);
}