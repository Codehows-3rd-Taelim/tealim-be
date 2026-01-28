# LangChain4j vs Spring AI 기술 비교 분석

> 본 프로젝트(tealim-be)의 실제 코드 기반으로 두 프레임워크의 트레이드오프를 분석한 문서.

---

## 1. 프레임워크 개요

| 항목 | LangChain4j | Spring AI |
|------|-------------|-----------|
| 주도 조직 | LangChain4j 커뮤니티 (Dmytro Liubarskyi 등) | VMware / Pivotal (Spring 팀) |
| 첫 릴리즈 | 2023년 | 2023년 (2024년 1.0 GA) |
| 설계 철학 | LLM 오케스트레이션 전문 프레임워크 | Spring 생태계 통합 AI 추상화 |
| 현재 버전 (프로젝트 사용) | 1.10.0-beta18 | - (미사용) |
| Java 최소 버전 | Java 8+ | Java 17+ |
| Spring Boot 의존성 | 선택적 (standalone 가능) | 필수 |

---

## 2. 현재 프로젝트의 LangChain4j 사용 현황

본 프로젝트에서 사용 중인 LangChain4j 핵심 기능:

```
langchain4j-spring-boot-starter:1.10.0-beta18
langchain4j-milvus:1.10.0-beta18
langchain4j-google-ai-gemini:1.10.0
langchain4j:1.10.0
```

### 사용 기능 목록

| 기능 | 구현 위치 | LangChain4j API |
|------|-----------|-----------------|
| 선언형 AI 에이전트 | `Agent`, `ReportAgent` | `AiServices.builder()`, `@SystemMessage`, `@UserMessage` |
| 스트리밍 응답 | `AgentService`, `AiReportService` | `TokenStream`, `StreamingChatLanguageModel` |
| 함수 호출 (Tool) | `ReportTools`, `ChatTools` | `@Tool`, `ToolSpecification`, `ToolExecutor` |
| RAG 검색 | `Agent` 채팅 | `ContentRetriever`, `EmbeddingStoreContentRetriever` |
| 벡터 저장소 | `EmbeddingStoreManager` | `MilvusEmbeddingStore` |
| 임베딩 모델 | `EmbeddingModelConfig` | `GoogleAiEmbeddingModel` |
| 대화 메모리 | `LangChainConfig` | `MessageWindowChatMemory`, `InMemoryChatMemoryStore` |
| 텍스트 분할 | `SentenceTextSplitterStrategy` | 자체 구현 (LangChain4j의 `DocumentSplitter` 미사용) |

---

## 3. 기능별 상세 비교

### 3.1 모델 추상화 및 제공자 지원

#### LangChain4j (현재)

```java
// LangChainConfig.java
@Bean
StreamingChatLanguageModel streamingChatModel() {
    return GoogleAiGeminiStreamingChatModel.builder()
            .apiKey(geminiApiKey)
            .modelName(chatModelName)
            .temperature(0.0)
            .topP(0.95)
            .topK(40)
            .maxOutputTokens(8192)
            .build();
}
```

- Google Gemini 전용 모듈 (`langchain4j-google-ai-gemini`) 사용
- 모델 제공자별 별도 의존성 추가 필요
- 15개 이상의 LLM 제공자 지원 (OpenAI, Anthropic, Google, Ollama, Hugging Face 등)

#### Spring AI 동일 구현 시

```java
// application.yml 기반 자동 설정
spring:
  ai:
    vertex:
      ai:
        gemini:
          chat:
            options:
              model: gemini-2.0-flash
              temperature: 0.0
              top-p: 0.95
              top-k: 40
              max-output-tokens: 8192
```

```java
// 또는 Java 설정
@Bean
ChatModel chatModel() {
    return VertexAiGeminiChatModel.builder()
            .apiKey(geminiApiKey)
            .defaultOptions(VertexAiGeminiChatOptions.builder()
                    .model("gemini-2.0-flash")
                    .temperature(0.0)
                    .build())
            .build();
}
```

- `spring-ai-vertex-ai-gemini-spring-boot-starter` 자동 설정
- `application.yml`만으로 모델 전환 가능
- 20개 이상의 모델 제공자 지원

**비교 결과**: Spring AI는 Spring Boot 자동 설정 방식으로 더 간결하나, LangChain4j도 Spring Boot Starter를 제공하여 큰 차이 없음. Gemini 지원은 양쪽 모두 제공.

---

### 3.2 선언형 에이전트 (AI Services)

#### LangChain4j (현재)

```java
// Agent.java — 인터페이스 기반 선언
public interface Agent {
    @SystemMessage("""
        당신은 도움을 주는 AI 어시스턴트입니다...
    """)
    TokenStream chat(@UserMessage String userMessage, @MemoryId Object memoryId);
}

// AgentConfig.java — 빌더로 조립
@Bean
Agent chatAgent() {
    return AiServices.builder(Agent.class)
            .streamingChatLanguageModel(streamingChatModel)
            .chatMemoryProvider(chatMemoryProvider)
            .contentRetriever(contentRetriever)
            .build();
}
```

- 인터페이스 + 어노테이션으로 에이전트 선언
- `AiServices.builder()`로 모델, 메모리, 도구, RAG를 조립
- `@MemoryId`로 멀티턴 대화 지원
- `TokenStream` 반환으로 스트리밍 내장

#### Spring AI 동일 구현 시

```java
// Spring AI에는 AiServices 같은 선언형 에이전트 추상화가 없음
// ChatClient (Fluent API)로 구성

@Bean
ChatClient chatClient(ChatModel chatModel, VectorStore vectorStore) {
    return ChatClient.builder(chatModel)
            .defaultSystem("당신은 도움을 주는 AI 어시스턴트입니다...")
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),
                new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults())
            )
            .build();
}

// 호출
Flux<String> stream = chatClient.prompt()
        .user(userMessage)
        .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, memoryId))
        .stream()
        .content();
```

- `ChatClient` Fluent API로 구성 (인터페이스 선언 방식 없음)
- `Advisor` 패턴으로 메모리, RAG 등을 파이프라인에 삽입
- Reactive `Flux<String>` 기반 스트리밍

**비교 결과**:

| 측면 | LangChain4j | Spring AI |
|------|-------------|-----------|
| 선언 방식 | 인터페이스 + 어노테이션 (타입 안전) | Fluent API (런타임 구성) |
| 코드 가독성 | 에이전트 역할이 인터페이스에 명시 | 호출부에서 구성이 흩어질 수 있음 |
| 유연성 | 빌드 타임 바인딩 | 런타임 동적 구성 용이 |
| 학습 곡선 | AiServices 개념 학습 필요 | Spring 개발자에게 익숙한 패턴 |

LangChain4j의 `AiServices` 선언형 패턴은 본 프로젝트의 `Agent`, `ReportAgent`처럼 **역할이 명확한 에이전트를 인터페이스로 표현**하는 데 적합. Spring AI는 이런 선언형 추상화가 없어 동일 구현 시 코드가 더 분산됨.

---

### 3.3 함수 호출 (Tool / Function Calling)

#### LangChain4j (현재)

```java
// ReportTools.java
@Component
public class ReportTools {
    @Tool("""
        관리자 전용 도구. 전체 매장의 로봇 운영 데이터를 조회합니다.
        startDate: 시작일 (YYYY-MM-DD), endDate: 종료일 (YYYY-MM-DD)
    """)
    public String getReport(String startDate, String endDate) {
        // 구현
    }
}

// AgentConfig.java — 커스텀 Tool 실행 컨텍스트 바인딩
ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
DefaultToolExecutor delegate = new DefaultToolExecutor(reportTools, method);
toolMap.put(spec, (request, memoryId) -> {
    if (memoryId != null) ToolArgsContextHolder.bind(memoryId.toString());
    return delegate.execute(request, memoryId);
});
```

- `@Tool` 어노테이션으로 도구 정의
- `ToolExecutor` 인터페이스로 커스텀 실행 로직 래핑 가능
- `ToolSpecification` 자동 생성 (메서드 시그니처 + description)
- 도구 실행 전/후 인터셉트 가능 (본 프로젝트에서 `ToolArgsContextHolder.bind()` 활용)

#### Spring AI 동일 구현 시

```java
// 함수형 Bean 방식
@Bean
@Description("관리자 전용 도구. 전체 매장의 로봇 운영 데이터를 조회합니다.")
Function<ReportRequest, String> getReport() {
    return request -> {
        // 구현
    };
}

// 또는 @Tool 어노테이션 (Spring AI 1.0.0+부터 지원, LangChain4j와 유사)
// 호출 시
chatClient.prompt()
        .user(userMessage)
        .functions("getReport", "getStoreReport", "resolveStore")
        .stream()
        .content();
```

- `java.util.function.Function` Bean으로 등록하거나 메서드 어노테이션 사용
- `@Description`으로 도구 설명 추가
- `ChatClient`의 `.functions()`로 사용할 도구 지정

**비교 결과**:

| 측면 | LangChain4j | Spring AI |
|------|-------------|-----------|
| 도구 정의 | `@Tool` 어노테이션 | `Function` Bean 또는 `@Tool` |
| 커스텀 실행 래핑 | `ToolExecutor` 인터페이스 (세밀한 제어) | 제한적 (Advisor로 간접 제어) |
| 컨텍스트 바인딩 | `ToolExecutor` 래핑으로 자유롭게 가능 | 직접 구현 필요 |
| 도구 선택 | `AiServices` 빌드 시 바인딩 | 호출 시 `.functions()`로 지정 |

본 프로젝트의 `AgentConfig`에서 `ToolExecutor`를 래핑하여 `ToolArgsContextHolder.bind()`를 호출하는 패턴은 **LangChain4j의 세밀한 도구 실행 제어**를 활용한 것. Spring AI에서는 동일 패턴 구현이 더 번거로움.

---

### 3.4 스트리밍 응답 (SSE)

#### LangChain4j (현재)

```java
// AgentService.java
TokenStream tokenStream = chatAgent.chat(prompt, conversationId);
tokenStream
    .onPartialResponse(token -> sseService.sendEvent(convId, "message", token))
    .onCompleteResponse(response -> {
        sseService.sendFinalAndComplete(convId, "complete", "[DONE]");
    })
    .onError(error -> sseService.completeWithError(convId, error))
    .start();
```

- `TokenStream` 콜백 기반 (onPartialResponse, onCompleteResponse, onError)
- 명시적 `.start()` 호출로 스트리밍 시작
- SseEmitter와 수동 연결 필요

#### Spring AI 동일 구현 시

```java
// Reactive Flux 기반
Flux<String> flux = chatClient.prompt()
        .user(prompt)
        .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
        .stream()
        .content();

// SSE와 자연스럽게 통합
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<ServerSentEvent<String>> stream(@RequestParam String prompt) {
    return chatClient.prompt()
            .user(prompt)
            .stream()
            .content()
            .map(token -> ServerSentEvent.builder(token).build());
}
```

- `Flux<String>` 반환으로 Reactor/WebFlux와 네이티브 통합
- SSE 엔드포인트 구현이 선언적
- 별도 SseEmitter 관리 불필요 (WebFlux 사용 시)

**비교 결과**:

| 측면 | LangChain4j | Spring AI |
|------|-------------|-----------|
| 스트리밍 모델 | 콜백 기반 (TokenStream) | Reactive 기반 (Flux) |
| SSE 통합 | SseEmitter 수동 관리 필요 | WebFlux와 네이티브 통합 |
| 에러 처리 | onError 콜백 | Flux.onErrorResume 등 Reactor 연산자 |
| 백프레셔 | 없음 | Reactor 백프레셔 지원 |
| 현재 프로젝트 호환성 | Spring MVC + SseEmitter (현재 사용) | WebFlux 전환 필요 |

본 프로젝트는 **Spring MVC 기반**이므로 `SseEmitter`를 사용. Spring AI는 WebFlux와의 통합이 우수하나, 현재 프로젝트가 MVC 기반이라 전환 시 WebFlux 마이그레이션 고려 필요. 단, Spring AI도 MVC 환경에서 `SseEmitter`와 함께 사용 가능.

---

### 3.5 RAG (Retrieval-Augmented Generation)

#### LangChain4j (현재)

```java
// LangChainConfig.java
@Bean
ContentRetriever contentRetriever() {
    return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(lcEmbeddingModel)
            .maxResults(ragMaxResults)
            .minScore(ragMinScore)
            .build();
}

// Agent에 바인딩
AiServices.builder(Agent.class)
        .contentRetriever(contentRetriever)
        .build();
```

- `ContentRetriever` 인터페이스로 RAG 삽입
- `AiServices`에 빌드 시 바인딩
- 검색된 문서가 자동으로 시스템 메시지에 추가

#### Spring AI 동일 구현 시

```java
// VectorStore 빈 (Milvus)
@Bean
VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return MilvusVectorStore.builder()
            .embeddingModel(embeddingModel)
            .collectionName("collection")
            .build();
}

// Advisor로 RAG 파이프라인 구성
ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(
            new QuestionAnswerAdvisor(vectorStore,
                SearchRequest.builder()
                    .topK(maxResults)
                    .similarityThreshold(minScore)
                    .build())
        )
        .build();
```

- `VectorStore` 추상화 + `QuestionAnswerAdvisor` 패턴
- Advisor 체인으로 RAG 파이프라인 구성
- 다양한 RAG 전략 Advisor 제공 (RetrievalAugmentationAdvisor 등)

**비교 결과**: 양쪽 모두 Milvus를 지원하며 RAG 구현 패턴이 유사. Spring AI는 `Advisor` 체인으로 더 유연한 파이프라인 구성이 가능하고, ETL 파이프라인 (DocumentReader → Transformer → Writer)이 내장.

---

### 3.6 대화 메모리

#### LangChain4j (현재)

```java
@Bean
ChatMemoryProvider chatMemoryProvider() {
    return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(chatMaxMessages)
            .chatMemoryStore(new InMemoryChatMemoryStore())
            .build();
}
```

- `ChatMemoryProvider` → 대화별 `ChatMemory` 인스턴스 생성
- `InMemoryChatMemoryStore`: 인메모리 저장 (재시작 시 소멸)
- `MessageWindowChatMemory`: 최근 N개 메시지 유지

#### Spring AI 동일 구현 시

```java
@Bean
ChatMemory chatMemory() {
    return new InMemoryChatMemory();
}

// Advisor로 메모리 적용
ChatClient.builder(chatModel)
        .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
        .build();
```

- `ChatMemory` 인터페이스 + `MessageChatMemoryAdvisor`
- JDBC, Cassandra, Neo4j 등 영속 저장소 지원 내장
- 대화별 ID는 Advisor 파라미터로 전달

**비교 결과**: 기능적으로 동등. Spring AI는 영속 메모리 저장소 (JDBC 등) 옵션이 더 다양.

---

### 3.7 벡터 저장소 (Embedding Store)

#### LangChain4j (현재)

```java
// Milvus 직접 클라이언트 관리 (EmbeddingStoreManager.java)
MilvusServiceClient client = new MilvusServiceClient(connectParam);
client.createCollection(createCollectionReq);
client.createIndex(createIndexReq);

// LangChain4j EmbeddingStore
MilvusEmbeddingStore.builder()
        .host(milvusHost).port(milvusPort)
        .collectionName(collectionName)
        .dimension(embeddingDimension)
        .build();
```

- `MilvusEmbeddingStore` 제공
- 컬렉션 생성/삭제/인덱스 관리는 Milvus Java SDK 직접 사용
- 지원 벡터DB: Milvus, Pinecone, Chroma, Weaviate, Qdrant, Redis, PgVector 등

#### Spring AI 동일 구현 시

```java
// spring-ai-milvus-store-spring-boot-starter
@Bean
VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return MilvusVectorStore.builder()
            .embeddingModel(embeddingModel)
            .databaseName("default")
            .collectionName(collectionName)
            .initializeSchema(true)  // 자동 스키마 생성
            .build();
}
```

- `VectorStore` 통합 인터페이스
- `initializeSchema(true)`로 자동 컬렉션/인덱스 생성
- 지원 벡터DB: Milvus, PgVector, Chroma, Pinecone, Weaviate, Qdrant, Redis, Elasticsearch, MongoDB Atlas 등

**비교 결과**: Spring AI는 `VectorStore` 통합 인터페이스와 자동 스키마 초기화가 장점. 본 프로젝트의 `EmbeddingStoreManager`처럼 Milvus SDK를 직접 호출하는 코드가 줄어듦.

---

## 4. 트레이드오프 요약

### LangChain4j 장점

| 장점 | 본 프로젝트에서의 의미 |
|------|----------------------|
| **선언형 AiServices** | `Agent`, `ReportAgent` 인터페이스로 에이전트 역할이 코드에 명시됨 |
| **세밀한 Tool 실행 제어** | `ToolExecutor` 래핑으로 `ToolArgsContextHolder.bind()` 컨텍스트 바인딩 구현 |
| **Spring 비의존 가능** | 단위 테스트 시 Spring Context 없이 `new ReportStatisticsService(null)` 가능 |
| **성숙한 LLM 오케스트레이션** | 다양한 고급 패턴 (Structured Output, Guardrails, etc.) |
| **Python LangChain과 개념 공유** | LangChain 경험자에게 익숙한 API 설계 |

### LangChain4j 단점

| 단점 | 본 프로젝트에서의 영향 |
|------|----------------------|
| **Beta 버전 의존성** | `1.10.0-beta18` — API 변경 위험 (실제로 이전 버전과 호환성 문제 경험) |
| **Spring 통합 간접적** | `SseEmitter` 수동 관리, 자동 설정 제한적 |
| **콜백 기반 스트리밍** | `TokenStream` 콜백이 Reactive 체인보다 에러 처리/합성 불편 |
| **문서/커뮤니티 규모** | Spring 생태계 대비 상대적으로 작은 커뮤니티 |
| **벡터DB 스키마 관리** | 컬렉션 생성/삭제를 Milvus SDK 직접 호출로 구현 |

### Spring AI 장점

| 장점 | 전환 시 기대 효과 |
|------|------------------|
| **Spring 네이티브 통합** | 자동 설정, Actuator 모니터링, Spring Security 연계 |
| **Advisor 파이프라인** | RAG, 메모리, 로깅을 체인으로 조합 — 관심사 분리 우수 |
| **Reactive 스트리밍** | `Flux<String>`으로 SSE 자연스럽게 통합, 백프레셔 지원 |
| **VectorStore 통합** | 자동 스키마 초기화, 통합 인터페이스로 벡터DB 교체 용이 |
| **ETL 파이프라인** | DocumentReader/Transformer/Writer로 PDF/CSV 임베딩 파이프라인 내장 |
| **안정적 릴리즈** | Spring 팀 주도, GA 릴리즈 (1.0.0 GA 출시 완료) |
| **Spring 개발자 친화** | 기존 Spring 패턴과 일관된 API |

### Spring AI 단점

| 단점 | 전환 시 리스크 |
|------|--------------|
| **선언형 에이전트 부재** | `Agent`, `ReportAgent` 인터페이스 패턴을 ChatClient 호출 코드로 대체 필요 |
| **Tool 실행 커스터마이징 제한** | `ToolExecutor` 같은 세밀한 래핑이 어려워 `ToolArgsContextHolder` 패턴 재설계 필요 |
| **MVC → WebFlux 고려** | Reactive 스트리밍의 장점을 완전히 누리려면 WebFlux 전환 필요 |
| **상대적으로 젊은 생태계** | 고급 패턴(Guardrails, Agent Loop 등)이 LangChain4j보다 덜 성숙 |
| **전환 비용** | 에이전트 2개, 도구 3개, RAG, 임베딩, SSE 모두 재작성 |

---

## 5. 본 프로젝트 관점의 기술 선택 가이드

### LangChain4j를 유지해야 하는 경우

1. **현재 에이전트 구조가 안정적일 때**
   - `Agent`와 `ReportAgent`의 인터페이스 기반 선언형 패턴이 이미 잘 동작하며, 에이전트 추가/변경이 빈번하지 않은 경우.

2. **Tool 실행 컨텍스트 바인딩이 핵심일 때**
   - `ToolArgsContextHolder.bind()`를 통한 비동기 컨텍스트 전파가 프로젝트 전반에 사용되고 있어, 이 패턴을 유지하는 것이 안전.

3. **전환 비용 대비 효과가 낮을 때**
   - 프로젝트가 이미 production에 배포되었고 기능 추가보다 안정성이 우선인 경우. 프레임워크 전환은 전체 AI 모듈 재작성을 의미.

4. **Python LangChain과 개념을 공유할 때**
   - 팀에 Python LangChain 경험자가 있거나, 향후 Python 서비스와 아키텍처를 통일할 계획이 있는 경우.

### Spring AI로 전환해야 하는 경우

1. **Spring 생태계 통합이 우선일 때**
   - Spring Security, Actuator, Config Server 등과의 통합이 중요하고, 운영/모니터링 인프라를 Spring 기반으로 통일하고 싶은 경우.

2. **WebFlux 전환을 계획 중일 때**
   - 본 프로젝트의 `build.gradle`에 주석 처리된 `spring-boot-starter-webflux`가 있음. Reactive 전환을 계획한다면 Spring AI의 `Flux` 기반 스트리밍이 더 자연스러움.

3. **벡터DB 교체/확장 가능성이 있을 때**
   - Milvus에서 PgVector나 Elasticsearch로 전환 가능성이 있다면, Spring AI의 `VectorStore` 통합 인터페이스가 교체 비용을 줄여줌.

4. **Beta 의존성을 제거하고 싶을 때**
   - `langchain4j-spring-boot-starter:1.10.0-beta18`이 Beta 상태. API 변경 리스크를 줄이려면 Spring AI 1.0 GA로의 전환이 안정성 측면에서 유리.

5. **팀이 Spring 중심 기술 스택일 때**
   - 팀원 대다수가 Spring 패턴에 익숙하고, LangChain4j의 `AiServices`/`ToolExecutor` 개념이 추가 학습 비용인 경우.

---

## 6. 전환 시 영향 범위 분석

Spring AI 전환 시 수정이 필요한 파일과 난이도:

| 파일 | 변경 내용 | 난이도 |
|------|-----------|--------|
| `build.gradle` | langchain4j → spring-ai 의존성 교체 | 낮음 |
| `LangChainConfig.java` | ChatModel, VectorStore, ChatMemory Bean 재정의 | 중간 |
| `EmbeddingModelConfig.java` | EmbeddingModel Bean 교체 | 낮음 |
| `AgentConfig.java` | AiServices → ChatClient 전환, ToolExecutor 래핑 재설계 | **높음** |
| `Agent.java` | 인터페이스 삭제, ChatClient 호출로 대체 | 중간 |
| `ReportAgent.java` | 인터페이스 삭제, ChatClient + Function Calling으로 대체 | **높음** |
| `ReportTools.java` | `@Tool` → `Function` Bean 또는 Spring AI `@Tool` | 중간 |
| `ChatTools.java` | 동일하게 Function Bean 전환 | 낮음 |
| `AgentService.java` | TokenStream → Flux 전환 | 중간 |
| `AiReportService.java` | TokenStream 콜백 → Flux 체인, ToolArgsContextHolder 재설계 | **높음** |
| `SseService.java` | WebFlux 전환 시 제거 가능, MVC 유지 시 그대로 | 조건부 |
| `EmbeddingStoreManager.java` | MilvusEmbeddingStore → MilvusVectorStore 전환 | 중간 |
| `EmbeddingService.java` | 임베딩 API 교체 | 중간 |
| `SentenceTextSplitterStrategy.java` | Spring AI TextSplitter로 교체 가능 | 낮음 |

**총 영향 파일**: 약 14개
**고난이도 파일**: 3개 (AgentConfig, ReportAgent, AiReportService)

---

## 7. 결론

| 기준 | 추천 |
|------|------|
| **현재 프로젝트 유지** | **LangChain4j 유지** — 이미 안정적으로 동작하는 에이전트 구조와 Tool 실행 패턴을 재작성할 이유 없음 |
| **신규 프로젝트 시작** | **Spring AI** — Spring 생태계 통합, GA 안정성, Advisor 패턴이 장기적으로 유리 |
| **점진적 전환** | RAG/임베딩 모듈부터 Spring AI로 전환하고, 에이전트는 LangChain4j 유지 (두 프레임워크 공존 가능) |

본 프로젝트의 핵심 차별점인 **선언형 에이전트 + ToolExecutor 컨텍스트 바인딩**은 LangChain4j에서만 깔끔하게 구현 가능하며, 이 패턴이 보고서 생성 파이프라인의 근간이므로 현 시점에서 전환의 실익은 크지 않음.
