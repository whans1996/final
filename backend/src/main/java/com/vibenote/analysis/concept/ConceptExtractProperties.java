package com.vibenote.analysis.concept;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * EXTRACTING 단계 설정 ({@code analysis.concept.*}, F-08 5절).
 *
 * @param catalogMaxChars 기존 카드 목록 상한
 * @param fileListMaxChars 선별 파일 목록 상한
 * @param maxFileChars 코드 파일 하나에 넣는 최대 글자 수 (줄 번호 포함)
 * @param maxChars 프롬프트 입력(카드 목록 + 파일 목록 + 코드) 합계 상한
 * @param maxLinks 분석당 저장하는 연결 수 상한
 * @param maxDrafts 분석당 새로 만드는 AI 초안 카드 수 상한
 */
@ConfigurationProperties("analysis.concept")
public record ConceptExtractProperties(
        @DefaultValue("6000") int catalogMaxChars,
        @DefaultValue("6000") int fileListMaxChars,
        @DefaultValue("3000") int maxFileChars,
        @DefaultValue("24000") int maxChars,
        @DefaultValue("15") int maxLinks,
        @DefaultValue("5") int maxDrafts) {}
