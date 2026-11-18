package com.vibenote.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * 분석 설정 ({@code analysis.*}, F-05a 5절).
 *
 * @param dailyLimit 하루(Asia/Seoul) 분석 요청 한도. FAILED는 세지 않고 soft delete된 분석은 센다
 * @param maxFiles 필터 후 지원 파일 수 상한. 넘으면 ANALYSIS_413
 * @param maxTotalBytes 읽은 지원 파일 합계 상한. 넘으면 ANALYSIS_413
 * @param maxFileBytes 단일 파일 상한. 넘는 파일은 건너뛴다
 * @param maxSelectedFiles 선별해 저장하는 파일 수
 */
@ConfigurationProperties("analysis")
public record AnalysisProperties(
        @DefaultValue("5") int dailyLimit,
        @DefaultValue("200") int maxFiles,
        @DefaultValue("10MB") DataSize maxTotalBytes,
        @DefaultValue("1MB") DataSize maxFileBytes,
        @DefaultValue("30") int maxSelectedFiles) {}
