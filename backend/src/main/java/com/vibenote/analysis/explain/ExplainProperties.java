package com.vibenote.analysis.explain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * EXPLAINING 단계 설정 ({@code analysis.explain.*}, F-06 5절).
 *
 * @param batchSize 파일 설명 1회 호출에 묶는 파일 수
 * @param maxFileChars 파일 묶음 프롬프트에 넣는 파일당 최대 글자 수 (넘으면 생략 줄을 붙인다)
 * @param overviewMaxChars 개요 프롬프트 입력(README + 파일 목록 + API + 코드) 합계 상한
 * @param readmeMaxChars 개요 입력에 넣는 루트 README 앞부분 글자 수 (F-26a AC9)
 */
@ConfigurationProperties("analysis.explain")
public record ExplainProperties(
        @DefaultValue("6") int batchSize,
        @DefaultValue("6000") int maxFileChars,
        @DefaultValue("24000") int overviewMaxChars,
        @DefaultValue("4000") int readmeMaxChars) {}
