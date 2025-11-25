package com.codehows.taelimbe.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 애플리케이션의 비동기 작업을 위한 중앙 스레드 풀을 설정하는 클래스입니다.
 * `@Configuration` 어노테이션은 이 클래스가 Spring의 설정 클래스임을 나타내며,
 * Spring 컨테이너가 이 클래스에서 정의된 `@Bean` 메서드를 통해 빈을 생성하도록 지시합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 비동기 작업을 처리하기 위한 `TaskExecutor` Bean을 생성합니다.
     * 이 `TaskExecutor`는 `@Async` 어노테이션이 붙은 메서드를 실행하는 데 사용될 수 있습니다.
     * `TaskDecorator`를 사용하여 `UserContextHolder`의 컨텍스트를 비동기 스레드로 전파합니다.
     *
     * @return 설정이 완료된 `ThreadPoolTaskExecutor` 인스턴스
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

        // TaskDecorator를 설정하여 부모 스레드의 UserContextHolder 컨텍스트를 자식 스레드로 전파합니다.
        executor.setTaskDecorator(new TaskDecorator() {
            @NotNull
            @Override
            public Runnable decorate(@NotNull Runnable runnable) {
                // 부모 스레드의 사용자 이름을 캡처합니다.
                String userName = UserContextHolder.getUserName();
                return () -> {
                    try {
                        // 자식 스레드에서 사용자 이름을 설정합니다.
                        UserContextHolder.setUserName(userName);
                        runnable.run();
                    } finally {
                        // 작업 완료 후 사용자 이름을 클리어합니다.
                        UserContextHolder.clear();
                    }
                };
            }
        });

        //TODO Spring Security 스펙 추가 후 아래 방식으로 Security Context 전파
        // return new DelegatingSecurityContextExecutor(executor);
        return executor;
    }
}
