package com.vibenote.analysis.flow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 실행 흐름 전용 실행기 (core 2, max 2, queue 50). 흐름 1개가 LLM 호출(최대 180초 × 형식 재시도 1회)로 스레드를 최대 360초 잡으므로 분석
 * 파이프라인({@code analysisExecutor})과 나눠 흐름 요청이 몰려도 분석이 대기하지 않게 한다 (#24). 큐가 차면 거절되고 {@link
 * FlowLauncher}가 그 흐름을 FAILED로 표시한다.
 */
@Configuration
public class FlowExecutorConfig {

    public static final String EXECUTOR = "flowExecutor";

    @Bean(EXECUTOR)
    ThreadPoolTaskExecutor flowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("flow-");
        return executor;
    }
}
