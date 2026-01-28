# 마이그레이션 리포트: LangChain4j → Spring AI, MySQL+Milvus → PostgreSQL+pgvector

## 1. 마이그레이션 개요

| 항목 | 현재 (Before) | 목표 (After) |
|------|--------------|-------------|
| AI 프레임워크 | LangChain4j 1.10.0-beta18 | Spring AI 1.0.0 |
| 관계형 DB | MySQL 8.0 | PostgreSQL 16 |
| 벡터 DB | Milvus 2.3.1 (별도 서비스) | pgvector (PostgreSQL 확장) |
| AI 모델 | Gemini 2.5-flash + embedding-001 | 동일 (변경 없음) |
| 임베딩 차원 | 3072 | 동일 |

---

## 2. 트레이드오프 분석

### 2.1 AI 프레임워크: LangChain4j vs Spring AI

| 관점 | LangChain4j | Spring AI | 판정 |
|------|------------|-----------|------|
| **Spring 통합** | 별도 starter 필요, 수동 Bean 구성 | Spring Boot auto-configuration 네이티브 | ✅ Spring AI |
| **인터페이스 추상화** | `@SystemMessage`, `@Tool`, `AiServices.builder()` 고수준 추상화 | 저수준 `ChatModel` 직접 호출, `FunctionCallback` 수동 구현 | ⚠️ LangChain4j (편의성) |
| **스트리밍** | `TokenStream` 콜백 패턴 | `Flux<ChatResponse>` 리액티브 스트림 | ✅ Spring AI (표준 리액티브) |
| **도구 호출** | `@Tool` 어노테이션 선언적 | `FunctionCallback` 명시적 등록 | 동등 |
| **RAG** | `ContentRetriever` 자동 주입 | `VectorStore.similaritySearch()` 수동 호출 | ⚠️ 트레이드오프 (제어 vs 편의) |
| **메모리 관리** | `ChatMemoryProvider` + `@MemoryId` 자동 | `ChatMemory` 수동 get/add | ⚠️ 트레이드오프 |
| **생태계 성숙도** | beta18 버전, API 불안정 | 1.0 GA, Spring 공식 프로젝트 | ✅ Spring AI |
| **장기 유지보수** | Pivotal이 아닌 커뮤니티 | Spring 팀 공식 유지보수 | ✅ Spring AI |
| **Gemini 지원** | 전용 모듈 존재 | Vertex AI starter (API키 사용 시 커스텀 필요) | ⚠️ LangChain4j |

**종합 판단**: Spring AI는 편의성 일부를 잃지만, Spring 생태계 네이티브 통합과 장기 안정성에서 우위. LangChain4j beta 의존성 탈피가 핵심 이점.

---

### 2.2 데이터베이스: MySQL vs PostgreSQL

| 관점 | MySQL 8.0 | PostgreSQL 16 | 판정 |
|------|-----------|---------------|------|
| **TEXT 타입** | LONGTEXT (4GB), TEXT (64KB) 구분 | TEXT (무제한, 단일 타입) | ✅ PostgreSQL (단순) |
| **JSON 지원** | JSON (바이너리 아님) | JSONB (바이너리, 인덱싱 가능) | ✅ PostgreSQL |
| **JPQL 호환** | 표준 JPQL 사용 중 | 동일하게 동작 | 동등 |
| **집계 쿼리 성능** | 양호 | 복잡 쿼리에서 일반적으로 우수 | ✅ PostgreSQL |
| **확장 기능** | 제한적 | pgvector, PostGIS 등 풍부 | ✅ PostgreSQL |
| **커넥션 풀링** | 기본 스레드 기반 | 동일 (HikariCP) | 동등 |

---

### 2.3 벡터 스토어: Milvus vs pgvector

| 관점 | Milvus 2.3.1 | pgvector (PostgreSQL 16) | 판정 |
|------|-------------|--------------------------|------|
| **인프라 복잡도** | etcd + MinIO + Milvus (3개 서비스) | PostgreSQL 단일 서비스 | ✅ pgvector |
| **메모리 사용** | 2-4GB (Milvus 전용) | PostgreSQL 공유 메모리 | ✅ pgvector |
| **트랜잭션** | 벡터 저장과 RDB 별도 트랜잭션 | 단일 트랜잭션 (ACID 보장) | ✅ pgvector |
| **벡터 검색 성능 (소규모 <100K)** | FLAT 인덱스: ~1ms | IVFFlat: ~2-5ms, HNSW: ~1-3ms | 동등 |
| **벡터 검색 성능 (대규모 >1M)** | 전용 최적화, 매우 빠름 | 상대적으로 느림 | ⚠️ Milvus |
| **메타데이터 필터링** | JSON 필드 쿼리 | JSONB + GIN 인덱스 | ✅ pgvector |
| **운영 비용** | Docker 3개 서비스 관리 | 단일 DB 관리 | ✅ pgvector |
| **백업/복원** | Milvus 전용 + MySQL 별도 | pg_dump 단일 백업 | ✅ pgvector |
| **일관성** | 벡터-RDB 간 불일치 가능 | 단일 DB, 완전 일관성 | ✅ pgvector |

**핵심 이점**: 현재 프로젝트는 벡터 데이터가 소규모(수천~수만 건)이므로 pgvector 성능이 충분하며, **인프라 단순화**와 **트랜잭션 일관성**이 가장 큰 실질적 이점.

---

## 3. 성능 비교 근거

### 3.1 벡터 검색 벤치마크 (공개 자료 기반)

| 데이터셋 크기 | 차원 | Milvus (FLAT) | pgvector (IVFFlat) | pgvector (HNSW) |
|--------------|------|--------------|-------------------|----------------|
| 10K 벡터 | 1536 | 0.5ms | 1.2ms | 0.8ms |
| 100K 벡터 | 1536 | 3ms | 5ms | 2ms |
| 1M 벡터 | 1536 | 15ms | 45ms | 8ms |

> 출처: pgvector 0.7.0 벤치마크 (Jonathan Katz, 2024), Milvus 공식 벤치마크
> 차원 3072에서는 위 수치의 약 1.5-2배로 예상

### 3.2 현재 프로젝트 예상 데이터 규모

| 데이터 소스 | 예상 벡터 수 | 비고 |
|------------|-------------|------|
| CSV 임베딩 | ~1,000-5,000 | batch 30, 파일당 수백 청크 |
| PDF 임베딩 | ~500-2,000 | batch 50, 파일당 수십~수백 청크 |
| QnA 임베딩 | ~100-500 | 답변당 1-수개 벡터 |
| **합계** | **~1,600-7,500** | 소규모 |

**결론**: 이 규모에서 Milvus와 pgvector의 검색 성능 차이는 **1-3ms 이내**로, 체감 불가능한 수준. pgvector의 인프라 이점이 압도적.

### 3.3 PostgreSQL vs MySQL 쿼리 성능 (JPQL 기준)

| 쿼리 유형 | MySQL 8.0 | PostgreSQL 16 | 비고 |
|-----------|-----------|---------------|------|
| 단순 SELECT | 동등 | 동등 | - |
| JOIN + 집계 | 양호 | 우수 | PostgreSQL 옵티마이저 우위 |
| CASE WHEN 집계 (PuduReport) | 양호 | 우수 | CTE 최적화 가능 |
| COALESCE/GROUP BY | 동등 | 동등 | - |
| TEXT 전문검색 | FTS 제한적 | tsvector 강력 | 미사용 중 |

> `PuduReportRepository`의 복잡한 집계 쿼리(`findOverallSummary`, `findRobotStats`, `findZoneStats`)는 PostgreSQL에서 더 효율적인 실행 계획을 생성할 가능성이 높음.

### 3.4 인프라 리소스 절감

| 리소스 | 현재 (MySQL + Milvus) | 목표 (PostgreSQL + pgvector) | 절감 |
|--------|----------------------|------------------------------|------|
| Docker 컨테이너 수 | 4개 (MySQL, etcd, MinIO, Milvus) | 1개 (PostgreSQL) | **75% 감소** |
| 메모리 사용 | ~4-6GB (Milvus 2-4GB + MySQL 1-2GB) | ~1-2GB | **60-70% 감소** |
| 디스크 | MySQL + Milvus(MinIO) 별도 볼륨 | 단일 볼륨 | **50% 감소** |
| 네트워크 홉 | App→MySQL + App→Milvus (2개) | App→PostgreSQL (1개) | **50% 감소** |

---

## 4. 상세 변경사항

### 4.1 build.gradle

```diff
- implementation 'dev.langchain4j:langchain4j-spring-boot-starter:1.10.0-beta18'
- implementation 'dev.langchain4j:langchain4j-milvus:1.10.0-beta18'
- implementation 'dev.langchain4j:langchain4j-google-ai-gemini:1.10.0'
- implementation 'dev.langchain4j:langchain4j:1.10.0'
- runtimeOnly 'com.mysql:mysql-connector-j'
- implementation 'com.alibaba.fastjson2:fastjson2:2.0.47'

+ implementation platform('org.springframework.ai:spring-ai-bom:1.0.0')
+ implementation 'org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter'
+ implementation 'org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter'
+ runtimeOnly 'org.postgresql:postgresql'
```

### 4.2 docker-compose.yml

```diff
- services:
-   etcd: ...
-   minio: ...
-   milvus: ...

+ services:
+   postgres:
+     image: pgvector/pgvector:pg16
+     environment:
+       POSTGRES_DB: inufleet
+       POSTGRES_USER: postgres
+       POSTGRES_PASSWORD: postgres
+     ports:
+       - "5432:5432"
+     volumes:
+       - postgres-data:/var/lib/postgresql/data
+       - ./init-pgvector.sql:/docker-entrypoint-initdb.d/init.sql
```

### 4.3 application.properties

```diff
- spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
- spring.datasource.url=jdbc:mysql://localhost:3306/inufleet
+ spring.datasource.driver-class-name=org.postgresql.Driver
+ spring.datasource.url=jdbc:postgresql://localhost:5432/inufleet
+ spring.datasource.username=postgres
+ spring.datasource.password=postgres

- milvus.host=localhost
- milvus.port=19530
- milvus.collection-name=gemini_report_embeddings
- milvus.embedding.dimension=3072

+ # Spring AI
+ spring.ai.vertex.ai.gemini.chat.options.model=gemini-2.5-flash
+ spring.ai.vertex.ai.gemini.chat.options.temperature=0.0
+ spring.ai.vertex.ai.gemini.chat.options.max-output-tokens=8192
+ spring.ai.vertex.ai.gemini.embedding.options.model=gemini-embedding-001
+ spring.ai.vectorstore.pgvector.dimensions=3072
+ spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
+ spring.ai.vectorstore.pgvector.index-type=IVFFLAT
```

### 4.4 파일별 변경 상세

#### 삭제 파일 (4개)
| 파일 | 사유 |
|------|------|
| `langchain/Agent.java` | Spring AI는 인터페이스 기반 Agent 미지원. 서비스에서 직접 호출 |
| `ai/agent/ReportAgent.java` | 동일 사유 |
| `langchain/tools/ChatTools.java` | 빈 클래스, 불필요 |
| `langchain/config/LangChainConfig.java` | SpringAiConfig로 대체 |

#### 대폭 재작성 파일 (6개)
| 파일 | 주요 변경 |
|------|-----------|
| `langchain/config/EmbeddingModelConfig.java` | LangChain4j `GoogleAiEmbeddingModel` → Spring AI `EmbeddingModel` qualifier 재구성 |
| `ai/config/AgentConfig.java` | `AiServices.builder()` + 커스텀 `ToolExecutor` → `ChatMemory` Bean만 등록 |
| `ai/service/AgentService.java` | `chatAgent.chat()` → `StreamingChatModel.stream(Prompt)` + `Flux<ChatResponse>` + 수동 RAG/Memory |
| `ai/service/AiReportService.java` | `reportAgent.report()` → `StreamingChatModel` + `FunctionCallback` 도구 호출 + 재시도 로직 유지 |
| `ai/service/EmbeddingService.java` | LangChain4j `EmbeddingModel`/`EmbeddingStore` → Spring AI `EmbeddingModel`/`VectorStore` |
| `langchain/embaddings/EmbeddingStoreManager.java` | Milvus SDK 전체 → `VectorStore` + `JdbcTemplate` (pgvector 네이티브 쿼리) |

#### 소폭 수정 파일 (4개)
| 파일 | 주요 변경 |
|------|-----------|
| `langchain/tools/ReportTools.java` | `@Tool` 어노테이션 제거, 메서드 시그니처 유지. `FunctionCallback`으로 래핑 |
| `langchain/embaddings/SentenceTextSplitterStrategy.java` | LangChain4j import 제거 (자체 로직은 유지) |
| `ai/entity/AiChat.java` | `LONGTEXT` → `TEXT` |
| `ai/entity/AiReport.java` | `LONGTEXT` → `TEXT` |

#### 신규 파일 (2-3개)
| 파일 | 용도 |
|------|------|
| `ai/config/SpringAiConfig.java` | VectorStore(PgVectorStore) Bean, Gson Bean |
| `init-pgvector.sql` | pgvector 확장 활성화 + vector_store 테이블 + 인덱스 |
| (선택) `ai/callback/ReportFunctionCallbacks.java` | ReportTools를 Spring AI FunctionCallback으로 래핑 |

#### 변경 없음 (영향 없는 파일)
| 파일/모듈 | 사유 |
|-----------|------|
| `ai/service/SseService.java` | LangChain4j 의존성 없음 |
| `ai/service/ReportStatisticsService.java` | DB 쿼리만 사용, AI 프레임워크 무관 |
| `ai/service/ReportMarkdownBuilder.java` | 순수 문자열 처리 |
| `ai/config/AsyncConfig.java` | 스레드풀 설정, 프레임워크 무관 |
| `ai/config/ToolArgsContextHolder.java` | 자체 구현, 프레임워크 무관 |
| `langchain/converters/PdfEmbeddingNormalizer.java` | 순수 문자열 처리 |
| `langchain/embaddings/TextSplitterStrategy.java` | 인터페이스, LangChain4j 의존 없음 |
| `store/`, `robot/`, `user/`, `sync/`, `pudureport/`, `qna/` 모듈 전체 | AI/벡터DB 의존성 없음 |
| 모든 Repository 인터페이스 | 표준 JPQL만 사용, DB 엔진 무관 |

---

## 5. 리스크 및 완화 방안

| 리스크 | 심각도 | 완화 방안 |
|--------|--------|-----------|
| Spring AI의 Google AI Studio API 키 미지원 | 높음 | Vertex AI 전환 또는 커스텀 HTTP 클라이언트 구현 |
| Spring AI FunctionCallback이 LangChain4j @Tool과 동작 차이 | 중간 | 도구 호출 통합 테스트 필수 |
| Flux 스트리밍 → SSE 변환 시 에러 처리 차이 | 중간 | AgentService/AiReportService 스트리밍 단위 테스트 |
| pgvector 3072차원 검색 성능 | 낮음 | HNSW 인덱스 전환 옵션 확보 |
| JPQL 쿼리 PostgreSQL 호환성 | 낮음 | 모두 표준 JPQL, 네이티브 쿼리 없음 |

---

## 6. 결론

### 핵심 이점
1. **인프라 단순화**: Docker 4개 → 1개, 메모리 60-70% 절감
2. **데이터 일관성**: 벡터와 관계형 데이터가 단일 트랜잭션으로 관리
3. **프레임워크 안정성**: LangChain4j beta → Spring AI 1.0 GA (Spring 공식)
4. **운영 편의**: 단일 DB 백업/모니터링/스케일링

### 트레이드오프 수용
1. Agent 인터페이스 패턴 상실 → 서비스 레벨 직접 구현 (코드량 증가하나 제어력 향상)
2. 대규모 벡터 검색 시 Milvus 대비 성능 열위 → 현재 규모(<10K)에서 무의미
3. Google AI Studio 키 호환성 불확실 → 구현 시 검증 후 대응
