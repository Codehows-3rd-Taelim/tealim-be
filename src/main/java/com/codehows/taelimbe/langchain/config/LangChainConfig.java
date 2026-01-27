package com.codehows.taelimbe.langchain.config;

import com.codehows.taelimbe.langchain.converters.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * LangChain4j와 관련된 모든 Bean 설정을 담당하는 클래스입니다.
 * AI 모델, 임베딩, 벡터 저장소, 대화 메모리 등 AI 서비스의 핵심 구성요소를 설정합니다.
 * `@Configuration` 어노테이션은 이 클래스가 Spring의 설정 클래스임을 나타냅니다.
 */
@Configuration
public class LangChainConfig {

    /**
     * Gson 인스턴스를 Spring Bean으로 등록합니다.
     * `LocalDateTimeAdapter`를 등록하여 `LocalDateTime` 객체를 ISO 8601 형식으로
     * 직렬화/역직렬화할 수 있도록 구성합니다.
     *
     * @return 구성된 `Gson` 인스턴스
     */
    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

}
