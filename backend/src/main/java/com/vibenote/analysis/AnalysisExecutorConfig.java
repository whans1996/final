package com.vibenote.analysis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 분석 전용 실행기 (core 2, max 2, queue 50). 큐가 차면 거절되고 AnalysisPipelineLauncher가 FAILED로 표시한다. */
@Configuration
class AnalysisExecutorConfig {

    static final String EXECUTOR = "analysisExecutor";

    @Bean(EXECUTOR)
    ThreadPoolTaskExecutor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("analysis-");
        return executor;
    }
}
