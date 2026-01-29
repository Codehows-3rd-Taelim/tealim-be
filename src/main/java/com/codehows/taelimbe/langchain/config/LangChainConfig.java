package com.codehows.taelimbe.langchain.config;

import com.codehows.taelimbe.langchain.converters.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Spring AI 마이그레이션 후 남은 공통 Bean 설정.
 * AI 모델, 벡터 저장소 등은 Spring AI auto-config로 처리됩니다.
 */
@Configuration
public class LangChainConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }
}
