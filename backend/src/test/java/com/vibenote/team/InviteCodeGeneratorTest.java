package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** F-20a AC1: 초대 코드는 헷갈리는 I·O·0·1을 뺀 32자 중 8자다. */
class InviteCodeGeneratorTest {

    @Test
    void AC1_코드는_허용_문자_8자다() {
        InviteCodeGenerator generator = new InviteCodeGenerator();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            String code = generator.generate();
            assertThat(code).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
            codes.add(code);
        }

        assertThat(InviteCodeGenerator.ALPHABET).hasSize(32).doesNotContain("I", "O", "0", "1");
        assertThat(codes).hasSizeGreaterThan(990);
    }
}
