package com.vibenote.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** ErrorCode를 도메인 블록으로 재배치한 뒤 형식이 깨지지 않았는지 확인한다 (F-00b AC5, docs/conventions.md 3절). */
class ErrorCodeTest {

    private static final Pattern NAME =
            Pattern.compile(
                    "(COMMON|AUTH|USER|ANALYSIS|CONCEPT|QUIZ|REVIEW_PLAN|POST|TEAM|LLM|CHALLENGE|PROJECT|INTERVIEW|NOTIFICATION)_\\d{3}");

    @Test
    void AC5_모든_코드가_도메인_형식이고_메시지가_있으며_CONCEPT_404는_404다() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.name()).as("코드 이름").matches(NAME);
            assertThat(code.message()).as(code.name() + " message").isNotBlank();
            assertThat(code.status()).as(code.name() + " status").isNotNull();
        }

        ErrorCode concept404 = ErrorCode.valueOf("CONCEPT_404");
        assertThat(concept404.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(concept404.message()).isEqualTo("개념 카드를 찾을 수 없습니다.");
    }
}
