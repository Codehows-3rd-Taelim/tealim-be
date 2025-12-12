package com.codehows.taelimbe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class PuduReportAsyncConfig {

    @Bean(name = "PuduReportSyncExecutor")
    public Executor PuduReportSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);   // 동시 처리 기본
        executor.setMaxPoolSize(20);    // 부하 확장
        executor.setQueueCapacity(100); // 트래픽 버퍼
        executor.setThreadNamePrefix("pudu-report-");
        executor.initialize();
        return executor;
    }
}
