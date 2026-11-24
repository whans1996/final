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
 */
@ConfigurationProperties("analysis.structure")
public record StructureProperties(
        @DefaultValue("150") int maxEdges,
        @DefaultValue("100") int maxUnresolved,
        @DefaultValue("20000000") long maxWorkSteps,
        @DefaultValue("3s") Duration maxWorkTime) {}
