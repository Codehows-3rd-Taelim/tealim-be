# Inufleet Backend

AI 멀티 툴 에이전트 기반 청소 로봇 효율 모니터링 플랫폼의 백엔드 서버입니다.

## 프로젝트 정보

| 항목      | 내용                                                                            |
| --------- | ------------------------------------------------------------------------------- |
| 개발 기간 | 2025.09 ~ 2026.01                                                               |
| 구성 인원 | 4명 (PM/PL 1명, FE/BE 3명)                                                      |
| GitHub    | [Codehows-3rd-Taelim](https://github.com/orgs/Codehows-3rd-Taelim/repositories) |

## 기술 스택

**Backend**

- Java 21
- Spring Boot 3.2.5
- Spring Security (JWT)
- Spring Data JPA
- LangChain4j + Google Gemini

**Database**

- MySQL
- Milvus (Vector DB)

**DevOps**

- Docker
- Jenkins
- Nginx

## 프로젝트 구조

```
src/main/java/com/codehows/taelimbe/
├── ai/          # AI 에이전트, 임베딩, 채팅, 리포트
├── robot/       # 로봇 관리
├── store/       # 매장 관리
├── user/        # 사용자 인증/인가
├── pudureport/  # Pudu 로봇 리포트
├── qna/         # Q&A 기능
└── sync/        # 데이터 동기화
```

## 주요 기능

- 청소 로봇 실시간 모니터링
- AI 기반 운영 리포트 자동 생성
- RAG 기반 Q&A 챗봇
- 매장별 로봇 성능 분석
- 데이터 동기화 스케줄러
