package com.vibenote.analysis.structure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 구조 그래프 설정 ({@code analysis.structure.*}, F-21a 5.1절).
 *
 * @param maxEdges 응답 간선 상한. 넘으면 edgesTruncated
 * @param maxUnresolved 응답 확인하지 못한 연결 상한. 넘으면 unresolvedTruncated
 * @param maxWorkSteps 요청 하나의 작업 단계 상한 ({@link WorkBudget}, 5.3절 3)
 * @param maxWorkTime 요청 하나의 계산 시간 상한. 넘으면 남은 파일은 PARSE_ERROR, edgesTruncated
 * @param cacheSize DONE 분석 결과 캐시에 둘 분석 수 (LRU, 5.3절 4)
 * @param maxConcurrent 동시에 계산하는 요청 수 상한 (5.3절 4)
 * @param queueTimeout 동시 계산 자리를 기다리는 시간. 지나면 ANALYSIS_007 (503)
 */
@ConfigurationProperties("analysis.structure")
public record StructureProperties(
        @DefaultValue("150") int maxEdges,
        @DefaultValue("100") int maxUnresolved,
        @DefaultValue("20000000") long maxWorkSteps,
        @DefaultValue("3s") Duration maxWorkTime,
        @DefaultValue("64") int cacheSize,
        @DefaultValue("2") int maxConcurrent,
        @DefaultValue("10s") Duration queueTimeout) {}
