# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소의 코드를 다룰 때 참고하는 안내 문서입니다.

## 빌드 및 실행 명령어

```bash
./gradlew build          # 프로젝트 빌드
./gradlew bootRun        # 애플리케이션 실행
./gradlew test           # 전체 테스트 실행
./gradlew clean build    # 클린 후 재빌드
```

Java 21 필수. Spring Boot 3.2.5 + Gradle 기반.

## 인프라 의존성

- **MySQL 8.0** localhost:3306 (데이터베이스: `inufleet`)
- **Milvus 2.3.1** localhost:19530 (RAG용 벡터 데이터베이스)
- **Docker Compose**로 Milvus 스택(etcd + MinIO + Milvus)과 MySQL 구동

`docker-compose up` 명령으로 인프라 서비스를 시작합니다.

## 아키텍처

레스토랑 로봇 관리 및 AI 분석을 위한 Spring Boot 백엔드. 루트 패키지: `com.codehows.taelimbe`

### 도메인 모듈

각 모듈은 controller/dto/entity/repository/service 계층 구조를 따릅니다:

- **ai/** - LangChain4j + Gemini를 활용한 AI 채팅(멀티턴 대화) 및 보고서 생성. SSE 스트리밍으로 실시간 응답. Milvus 벡터 검색 기반 RAG.
- **langchain/** - LangChain4j 설정, 임베딩 모델(Gemini embedding-001, 3072차원), 텍스트 분할 전략, AI 도구 정의(`ChatTools`, `ReportTools`).
- **pudureport/** - 푸두 로봇 플랫폼에서 보고서 동기화. 과거 데이터 비동기 배치 처리.
- **robot/** - HMAC-SHA1 서명 요청을 통해 푸두 API에서 로봇 기기 정보 동기화 및 관리.
- **store/** - 업종별 매장 CRUD.
- **user/** - Spring Security 기반 JWT 인증(HS256). 무상태 세션, 역할 기반 접근 제어.
- **qna/** - 벡터 검색용 임베딩을 지원하는 Q&A 관리.
- **sync/** - `@Scheduled` 기반 정기 데이터 동기화 작업.
- **client/PuduAPIClient** - HMAC-SHA1 서명을 사용하는 푸두 플랫폼 외부 API 클라이언트.

### 주요 패턴

- **AI 도구 재시도**: LangChain4j 도구 호출 실패 시 최대 3회 자동 재시도.
- **비동기 처리**: 도메인별 `ThreadPoolTaskExecutor` 설정(core:10, max:20, queue:50). 거부 정책은 `CallerRunsPolicy`.
- **파일 임베딩**: CSV(배치 30건) 및 PDF(배치 50건) 업로드 후 Milvus 임베딩으로 처리. 최대 업로드 크기: 100MB.
- **QueryDSL 5.1.0**으로 Spring Data JPA와 함께 동적 쿼리 구성.
- **Lombok** 전역 사용 (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`).

### 환경 설정

- Gemini API 키는 `.env` 파일의 `GEMINI_API_KEY`로 설정
- 소스 코드 내 주석은 한국어로 작성
