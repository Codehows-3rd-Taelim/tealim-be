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


    final String BASE_SELECT_QUERY =
            "SELECT a.aiReportId as aiReportId, a.conversationId as conversationId, a.startTime as startTime, a.endTime as endTime, a.createdAt as createdAt, a.rawMessage as rawMessage, a.user.name as name ";

    //유저 권한 - 유저는 유저 자신이 쓴것만 볼 수 있다
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findMetaByUserId(@Param("userId") Long userId);



    //매니저 권한 - 자기 매장의 보고서 다 볼 수 있음
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a " +
            "WHERE a.user.store.storeId = :storeId " +
            "AND a.user.role <> 'ADMIN' " +
            "ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findMetaByStoreExcludingAdmin(@Param("storeId") Long storeId);



    //관리자 권한 - 모든 보고서 다 볼 수 있음
    @Query(BASE_SELECT_QUERY +
            "FROM AiReport a ORDER BY a.createdAt DESC")
    List<AiReportMetaProjection> findAllMetaOrderByCreatedAtDesc();

    // RawReport만 조회
    // 쿼리: a.rawReport as rawReport -> RawReportProjection.getRawReport()에 매핑
    @Query("SELECT a.rawReport as rawReport FROM AiReport a WHERE a.aiReportId = :reportId")
    Optional<RawReportProjection> findRawReportById(@Param("reportId") Long reportId);



}