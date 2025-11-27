package com.codehows.taelimbe.repository;


import com.codehows.taelimbe.entity.CleaningData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * `CleaningData` 엔티티에 대한 데이터베이스 접근을 처리하는 JPA 리포지토리 인터페이스입니다.
 * `@Repository` 어노테이션은 이 인터페이스가 데이터 접근 계층의 컴포넌트임을 나타냅니다.
 * `JpaRepository<CleaningData, Long>`을 상속받아 `CleaningData` 엔티티(기본 키 타입은 `Long`)에 대한
 * 기본적인 CRUD(Create, Read, Update, Delete) 및 페이징, 정렬 기능을 자동으로 제공받습니다.
 */
@Repository
public interface CleaningDataRepository extends JpaRepository<CleaningData, Long> {

    /**
     * 특정 시작 시간과 종료 시간 범위 내에 있는 청소 데이터를 페이징하여 조회합니다.
     * Spring Data JPA의 쿼리 메서드 기능을 활용하여 메서드 이름만으로 쿼리가 자동으로 생성됩니다.
     *
     * @param start 조회 시작 `LocalDateTime`
     * @param end 조회 종료 `LocalDateTime`
     * @return 해당 시간 범위 내의 `CleaningData` 엔티티 페이지
     */
    List<CleaningData> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
