FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# src 전체 복사 (application.properties 포함)
COPY src ./src

RUN gradle bootJar --no-daemon -x test

# JAR 내부 확인
RUN echo "=== Checking JAR contents ===" && \
    jar tf /app/build/libs/*.jar | grep application.properties

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
    
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
