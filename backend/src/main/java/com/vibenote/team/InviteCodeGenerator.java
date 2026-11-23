package com.vibenote.team;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** 초대 코드 생성 (F-20a AC1). 헷갈리는 I·O·0·1을 뺀 32자에서 SecureRandom으로 8자를 뽑는다. 충돌 처리는 TeamService가 한다. */
@Component
public class InviteCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] code = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            code[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(code);
    }
}
