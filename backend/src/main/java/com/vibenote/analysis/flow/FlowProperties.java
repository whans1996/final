package com.vibenote.analysis.flow;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 실행 흐름 설정 ({@code analysis.flow.*}, F-07 5절).
 *
 * @param maxChars 프롬프트에 넣는 코드 합계 상한 (파일당 상한은 {@code analysis.explain.max-file-chars})
 * @param maxFiles 입력 파일 수 상한
 * @param maxPerAnalysis 분석당 FAILED가 아닌 흐름 수 상한
 * @param maxSteps 저장하는 단계 수 상한
 */
@ConfigurationProperties("analysis.flow")
public record FlowProperties(
        @DefaultValue("24000") int maxChars,
        @DefaultValue("8") int maxFiles,
        @DefaultValue("3") int maxPerAnalysis,
        @DefaultValue("12") int maxSteps) {}
