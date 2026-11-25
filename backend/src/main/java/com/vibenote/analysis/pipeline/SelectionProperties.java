package com.vibenote.analysis.pipeline;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 중요도 선별 설정 ({@code analysis.selection.*}, F-26a 5.1절).
 *
 * @param maxConfig 선별하는 config 파일 수 상한 (AC7)
 * @param maxWorkSteps 선별용 구조 그래프 계산의 작업 단계 상한 (AC11)
 * @param maxWorkTime 선별용 구조 그래프 계산 시간 상한. 백그라운드라 구조 탭 요청(3s)보다 길다
 */
@ConfigurationProperties("analysis.selection")
public record SelectionProperties(
        @DefaultValue("3") int maxConfig,
        @DefaultValue("50000000") long maxWorkSteps,
        @DefaultValue("10s") Duration maxWorkTime) {}
