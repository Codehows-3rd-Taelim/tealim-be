package com.codehows.taelimbe.robot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class StoreRobotAsyncConfig {

    @Bean(name = "StoreRobotExecutor")
    public Executor storeRobotExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);   // 동시에 처리할 매장 수
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("store-robot-");
        executor.initialize();
        return executor;
    }
}
