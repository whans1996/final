package com.vibenote.analysis.structure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 구조 그래프 설정 ({@code analysis.structure.*}, F-21a 5.1절).
 *
 * @param maxEdges 응답 간선 상한. 넘으면 edgesTruncated
 * @param maxUnresolved 응답 확인하지 못한 연결 상한. 넘으면 unresolvedTruncated
 */
@ConfigurationProperties("analysis.structure")
public record StructureProperties(
        @DefaultValue("150") int maxEdges, @DefaultValue("100") int maxUnresolved) {}
