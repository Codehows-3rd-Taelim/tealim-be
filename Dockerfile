# 멀티 스테이지 빌드
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 캐시 활용
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# 빌드 (테스트 제외)
RUN gradle bootJar --no-daemon -x test

# 빌드 결과 확인 (디버깅용)
RUN echo "=== Build Output ===" && \
    ls -lh /app/build/libs/ && \
    jar tf /app/build/libs/*.jar | grep -E "(application\.properties|MANIFEST)" | head -20

# Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 단순하게 실행
ENTRYPOINT ["java", "-jar", "app.jar"]