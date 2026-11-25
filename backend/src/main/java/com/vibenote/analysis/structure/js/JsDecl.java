package com.vibenote.analysis.structure.js;

import java.util.List;

/**
 * 이름 선언 하나 (F-21a AC8·AC10). 이름 해석은 범위가 가장 안쪽인 선언을 고른다.
 *
 * @param scopeStart 보이는 범위 시작 (원본 offset, 파일 전체면 -1)
 * @param scopeEnd 보이는 범위 끝 (제외, 파일 전체면 Integer.MAX_VALUE)
 * @param init CONST의 초기값 토큰. 그 밖에는 빈 목록
 * @param function PARAM이면 그 함수
 */
public record JsDecl(
        String name,
        Kind kind,
        int scopeStart,
        int scopeEnd,
        List<JsToken> init,
        JsFunction function) {

    public enum Kind {
        CONST,
        VARIABLE,
        PARAM,
        FUNCTION,

        /**
         * 모델링하지 않은 선언 자리의 이름이나 {@code with} 안의 이름 (5.3절 1). 값도 모르고, 이 이름이 가린 바깥 이름이 HTTP 받는 쪽·래퍼였다면
         * 그 호출은 확인하지 못한 연결이다.
         */
        UNKNOWN
    }

    public boolean visibleAt(int offset) {
        return scopeStart <= offset && offset < scopeEnd;
    }
}
