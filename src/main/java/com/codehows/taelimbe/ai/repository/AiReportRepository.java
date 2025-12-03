package com.codehows.taelimbe.ai.repository;

import com.codehows.taelimbe.ai.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    // 특정 Store의 모든 리포트 조회 (최신순)
    @Query("SELECT a FROM AiReport a WHERE a.user.store.storeId = :storeId ORDER BY a.createdAt DESC")
    List<AiReport> findByStoreIdOrderByCreatedAtDesc(@Param("storeId") Long storeId);

    // 모든 리포트 조회 (ADMIN용 - 최신순)
    List<AiReport> findAllByOrderByCreatedAtDesc();
}