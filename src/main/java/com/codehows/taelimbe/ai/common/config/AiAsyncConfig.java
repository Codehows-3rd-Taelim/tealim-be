package com.codehows.taelimbe.ai.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 모듈의 비동기 작업을 위한 스레드 풀 설정 클래스입니다.
 */
@Configuration
@EnableAsync
public class AiAsyncConfig {

    /**
     * 비동기 작업을 처리하기 위한 TaskExecutor Bean을 생성합니다.
     * @Async 어노테이션이 붙은 메서드를 실행하는 데 사용됩니다.
     *
     * @return 설정이 완료된 ThreadPoolTaskExecutor 인스턴스
     */
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 코어 스레드 풀의 크기를 설정합니다. 이 수만큼의 스레드가 항상 유지됩니다.
        executor.setCorePoolSize(10);
        // 최대 스레드 풀의 크기를 설정합니다. 코어 풀이 가득 차고 큐도 가득 찼을 때 생성될 수 있는 최대 스레드 수입니다.
        executor.setMaxPoolSize(20);
        // 작업 큐의 용량을 설정합니다. 코어 풀의 스레드가 모두 사용 중일 때 작업이 대기하는 공간입니다.
        executor.setQueueCapacity(50);
        // 거부 정책을 설정합니다. 큐까지 가득 찼을 때 새로운 작업이 들어오면 호출자 스레드가 직접 작업을 실행합니다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 생성되는 스레드의 이름 접두사를 설정하여 로그에서 스레드를 쉽게 식별할 수 있도록 합니다.
        executor.setThreadNamePrefix("async-task-");
        // Executor를 초기화합니다.
        executor.initialize();

        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
