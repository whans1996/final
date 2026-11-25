package com.vibenote.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * 분석 설정 ({@code analysis.*}, F-05a 5절, F-26a 5.1절).
 *
 * @param dailyLimit 하루(Asia/Seoul) 분석 요청 한도. FAILED는 세지 않고 soft delete된 분석은 센다
 * @param maxIndexFiles 인덱스에 담는 파일 수 상한. 넘는 파일은 버리고 dropped_files에 센다 (실패시키지 않는다)
 * @param maxIndexBytes 인덱스에 담는 파일 합계 상한. 처음 넘는 파일부터 버린다
 * @param maxFileBytes 단일 파일 상한. 넘는 파일은 건너뛰고 dropped_files에 센다
 * @param maxSelectedFiles 선별해 저장하는 파일 수
 */
@ConfigurationProperties("analysis")
public record AnalysisProperties(
        @DefaultValue("5") int dailyLimit,
        @DefaultValue("2000") int maxIndexFiles,
        @DefaultValue("30MB") DataSize maxIndexBytes,
        @DefaultValue("1MB") DataSize maxFileBytes,
        @DefaultValue("30") int maxSelectedFiles) {}
