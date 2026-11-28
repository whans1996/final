package com.vibenote.interview;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 면접 질문 설정 ({@code interview.*}, I-1a 5절).
 *
 * @param maxChars 프롬프트에 넣는 코드 합계 상한
 * @param maxFileChars 파일 하나에서 넣는 코드 상한
 * @param fileListMaxChars 파일 목록 상한
 * @param conceptListMaxChars 개념 목록 상한
 * @param maxApis 개요에서 넣는 API 수 상한
 * @param minQuestions 저장하는 질문 수 하한. 검증을 거친 질문이 이보다 적으면 형식 오류로 1회 다시 묻는다 (AC4)
 * @param maxQuestions 저장하는 질문 수 상한
 * @param maxRegenerations DONE 다시 만들기 횟수 (분석당)
 */
@ConfigurationProperties("interview")
public record InterviewProperties(
        @DefaultValue("24000") int maxChars,
        @DefaultValue("4000") int maxFileChars,
        @DefaultValue("6000") int fileListMaxChars,
        @DefaultValue("3000") int conceptListMaxChars,
        @DefaultValue("20") int maxApis,
        @DefaultValue("5") int minQuestions,
        @DefaultValue("12") int maxQuestions,
        @DefaultValue("2") int maxRegenerations) {}
