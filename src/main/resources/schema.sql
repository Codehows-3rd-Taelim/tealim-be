-- CleaningData 테이블의 스키마를 정의하는 SQL 스크립트입니다.
-- 이 스크립트는 H2 데이터베이스에서 CleaningData 엔티티에 매핑되는 테이블을 생성합니다.

CREATE TABLE IF NOT EXISTS cleaning_data (
    cleaning_id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 청소 작업의 고유 ID (자동 증가, 기본 키)
    start_time TIMESTAMP NOT NULL,                 -- 청소 작업 시작 시간 (필수)
    end_time TIMESTAMP NOT NULL,                   -- 청소 작업 종료 시간 (필수)
    location VARCHAR(255) NOT NULL,                -- 청소 작업 위치 (필수)
    duration BIGINT,                               -- 청소 작업 소요 시간 (분 단위)
    area_cleaned DOUBLE,                           -- 청소된 면적 (제곱미터 단위)
    water_usage DOUBLE,                            -- 청소에 사용된 물의 양 (리터 단위)
    power_usage DOUBLE                             -- 청소에 사용된 전력량 (kWh 단위)
);