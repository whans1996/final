package com.vibenote.quiz;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * QUIZ 단계 설정 ({@code quiz.*}, F-14 5절).
 *
 * @param minQuestions 유효 문항이 이보다 적으면 형식 오류 (재시도)
 * @param maxQuestions 앞에서부터 이만큼만 저장한다
 * @param conceptListMaxChars 연결 개념 목록 상한
 * @param maxFileChars 코드 파일 하나에 넣는 최대 글자 수
 * @param maxChars 프롬프트 입력(개념 목록 + 코드) 합계 상한
 */
@ConfigurationProperties("quiz")
public record QuizProperties(
        @DefaultValue("5") int minQuestions,
        @DefaultValue("10") int maxQuestions,
        @DefaultValue("3000") int conceptListMaxChars,
        @DefaultValue("3000") int maxFileChars,
        @DefaultValue("24000") int maxChars) {}
