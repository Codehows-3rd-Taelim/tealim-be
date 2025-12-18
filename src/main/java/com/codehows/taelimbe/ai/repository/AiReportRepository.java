package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.AiReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    @Query("""
        SELECT
            a.aiReportId as aiReportId,
            a.conversationId as conversationId,
            a.startTime as startTime,
            a.endTime as endTime,
            a.createdAt as createdAt,
            a.rawMessage as rawMessage,
            a.user.name as name
        FROM AiReport a
        WHERE
            (:userRole = 'ADMIN') OR
            (:userRole = 'MANAGER' AND a.user.store.storeId = :storeId AND a.user.role <> 'ADMIN') OR
            (:userRole = 'USER' AND a.user.userId = :userId)
            AND (:searchText IS NULL OR a.rawMessage LIKE %:searchText%)
            AND (:startDate IS NULL OR a.startTime >= :startDate)
            AND (:endDate IS NULL OR a.endTime <= :endDate)
        ORDER BY a.createdAt DESC
    """)
    Page<AiReportMetaProjection> findMetaPageByRole(
            @Param("userId") Long userId,
            @Param("storeId") Long storeId,
            @Param("userRole") String userRole,
            @Param("searchText") String searchText,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT a.rawReport as rawReport FROM AiReport a WHERE a.aiReportId = :reportId")
    Optional<RawReportProjection> findRawReportById(@Param("reportId") Long reportId);
}
