# 보고서 생성 아키텍처 리팩토링 요약

## Case 1. LLM 통계 계산 부정확

### 1. 발생 문제
LLM이 수백 건의 JSON 데이터를 직접 받아 사칙연산(합계, 평균, 비율)을 수행하면서 총 작업 횟수, 성공률, 평균 배터리 소모 등 수치가 실제와 다르게 출력되는 현상 발생.

### 2. 원인
LLM은 확률 기반 토큰 생성 모델로, 수치 연산에 본질적으로 취약함. 수백 row의 JSON을 컨텍스트에 넣고 집계를 요청하면 row 누락, 반올림 오류, status 분류 혼동 등이 불가피.

### 3. 해결 방법
- `ReportStatisticsService` 신규 생성 — Java에서 `List<PuduReportDTO>`를 받아 총 작업 횟수, 시간 합계, 면적 합계, 배터리 평균, 물 소비량, 로봇별/구역별/상태별 집계를 순수 Java 로직으로 계산.
- `ReportMarkdownBuilder` 신규 생성 — 계산된 통계를 마크다운 테이블로 조립. 수치가 포함된 섹션은 전부 Java가 확정.
- LLM은 확정된 통계 요약 텍스트만 받아 인사이트(2~3문장)와 분석/권장사항 텍스트만 생성.

### 4. 성능 및 개선 효과
- 통계 수치 정확도 100% 보장 (Java 연산)
- LLM 환각에 의한 수치 오류 원천 차단

---

## Case 2. LLM에 과도한 토큰 입출력

### 1. 발생 문제
수백 건의 JSON 전체를 LLM 입력으로 전달하고, LLM이 마크다운 전체(테이블 포함)를 출력하면서 입출력 토큰이 과다 소모. 응답 지연 및 토큰 비용 증가.

### 2. 원인
- Tool 반환값이 `ReportResult(gson.toJson(reportData), ...)` — 전체 JSON 문자열을 LLM 컨텍스트에 삽입.
- `maxOutputTokens: 65536` 설정으로 LLM이 마크다운 전체를 생성.
- ReportAgent 시스템 메시지가 ~360줄로 계산 규칙, 포맷 규칙, 금지 규칙 등을 모두 포함.

### 3. 해결 방법
- `ReportTools` 반환 타입을 `ReportResult` → `String`("조회 완료: N건")으로 변경. 실제 데이터는 `ToolArgsContextHolder.setReportData()`로 Java 측에 저장.
- `ReportAgent` 시스템 메시지를 ~20줄로 축소 (날짜 파싱 + Tool 호출 지시만).
- `maxOutputTokens`를 65536 → 8192로 축소.
- 인사이트 생성 시 `toSummaryText()`로 압축된 통계 요약(수십 토큰)만 LLM에 전달.

### 4. 성능 및 개선 효과
- LLM 입력 토큰: 수천~수만 → 수백 수준으로 감소
- LLM 출력 토큰: 마크다운 전체(수천) → 인사이트+분석 텍스트(수백)로 감소
- 응답 속도 개선, API 비용 절감

---

## Case 3. 역할 분리 부재로 인한 유지보수 어려움

### 1. 발생 문제
보고서 포맷 변경, 통계 항목 추가, 상태 코드 매핑 수정 등 요구사항 변경 시 ReportAgent의 시스템 프롬프트를 수정해야 하며, 프롬프트 변경이 예측 불가능한 출력 변화를 유발.

### 2. 원인
하나의 LLM 프롬프트가 날짜 파싱, 데이터 조회, 수치 계산, 마크다운 포맷팅, 인사이트 생성을 모두 담당하는 단일 책임 위반 구조.

### 3. 해결 방법
6단계 파이프라인으로 분리:

| 단계 | 담당 | 역할 |
|------|------|------|
| Phase 1 | LLM (ReportAgent) | 날짜 파싱 + Tool 호출 |
| Phase 2 | Java (ReportTools) | 데이터 조회 + ToolArgsContextHolder 저장 |
| Phase 3 | Java (ReportStatisticsService) | 통계 계산 |
| Phase 4 | Java (ReportMarkdownBuilder) | 마크다운 조립 |
| Phase 5 | LLM (StreamingChatModel) | 인사이트/분석 텍스트 생성 |
| Phase 6 | Java (AiReportService) | 플레이스홀더 교체 + 저장 + SSE 전송 |

### 4. 성능 및 개선 효과
- 포맷 변경은 `ReportMarkdownBuilder`만 수정
- 계산 로직 변경은 `ReportStatisticsService`만 수정
- 인사이트 톤/스타일 변경은 `generateInsight()` 프롬프트만 수정
- 각 단계가 독립적으로 테스트 가능

---

## Case 4. 전체 Row 조회 후 Java 순회 집계로 인한 성능 저하

### 1. 발생 문제
6개월치 보고서 통계 계산 시 수천 건의 전체 Row를 DB에서 조회하여 Java에서 4회 순회(전체 합계, 로봇별, 구역별, 상태별)하면서 실행 시간 및 메모리 사용량이 과다.

### 2. 원인
`ReportStatisticsService.compute()`가 `List<PuduReportDTO>` 전체를 받아 Java `stream()`으로 GROUP BY, SUM, AVG를 수행. DB → App 간 수천 건의 Row가 네트워크로 전송되고, `ToolArgsContextHolder`에 `List<PuduReportDTO>`를 메모리에 보관.

### 3. 해결 방법
- `PuduReportRepository`에 5종 × 2(전매장/매장별) = 10개의 JPQL 집계 쿼리 추가 (COUNT, SUM, AVG, CASE WHEN, GROUP BY)
- `ReportStatisticsService.compute(LocalDateTime, LocalDateTime, Long)` 신규 메서드 — DB 집계 결과만 받아 파생 메트릭(나눗셈/매핑)만 Java에서 수행
- `ToolArgsContextHolder`에서 `REPORT_DATA` (List 저장) 제거, 날짜/scope/storeId만 저장
- 기존 `computeFromList()` 메서드는 성능 비교 테스트용으로 보존

### 4. 성능 및 개선 효과
3,080건 기준 벤치마크 결과:

| 항목 | Before (Java 순회) | After (DB 집계) |
|------|---------------------|-----------------|
| 실행 시간 | 179.27 ms | 69.02 ms |
| 메모리 변화 | +18,946 KB | +2,050 KB |
| DB 전송량 | 전체 Row (3,080건) | 집계 결과만 (수십 건) |

- **2.6배 속도 향상**, **메모리 약 16.9 MB 절감**
- 단위 테스트(`ReportStatisticsPerformanceTest`) 및 통합 테스트(`ReportStatisticsIntegrationTest`)로 수치 검증 완료

---

## Case 5. 집계 쿼리 반환 타입의 Type Safety 부재

### 1. 발생 문제
JPQL 집계 쿼리가 `Object[]`를 반환하여, Service 계층에서 인덱스 기반 캐스팅(`((Number) row[0]).intValue()`)으로 값을 추출. 컴파일 타임에 타입 오류를 감지할 수 없고, 쿼리 SELECT 순서 변경 시 런타임 `ClassCastException` 발생 위험.

### 2. 원인
JPQL 집계 쿼리의 기본 반환 타입이 `Object[]`이며, 별도 DTO 매핑 없이 사용. 특히 `findOverallSummary`는 `Object[]` 안에 `Object[]`가 중첩되는 문제도 있어 unwrapping 로직이 필요했음.

### 3. 해결 방법
- 5개 집계 전용 DTO 생성: `OverallSummaryDTO`, `StatusCountDTO`, `RobotStatsDTO`, `ZoneStatsDTO`, `RemarkDTO`
- JPQL `new` 생성자 표현식 적용: `SELECT new com.codehows.taelimbe.ai.dto.XxxDTO(...)`
- Repository 반환 타입을 `Object[]` → 각 DTO 타입으로 변경
- Service에서 인덱스 기반 캐스팅 제거, DTO getter로 직접 접근
- `Object[]` 중첩 unwrapping 로직 제거

### 4. 성능 및 개선 효과
- 컴파일 타임 타입 체크로 런타임 `ClassCastException` 원천 방지
- SELECT 컬럼 순서 변경 시에도 생성자 시그니처로 안전하게 바인딩
- Service 코드 가독성 향상 (`row[0]`, `row[1]` → `dto.getNickname()`, `dto.getCount()`)
