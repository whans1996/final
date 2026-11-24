package com.vibenote.analysis.structure.js;

import java.util.List;

/**
 * 함수 하나 (F-21a AC10). 매개변수 범위 판단과 래퍼 인라인에 쓴다.
 *
 * @param name {@code function f}·{@code const f = (…) =>}의 f. 객체·클래스 메서드와 익명 함수는 null
 * @param params 위치 순 매개변수 이름. 구조 분해·기본값 없는 단순 이름이 아니면 그 자리는 빈 문자열
 * @param start 매개변수 여는 괄호 위치 (원본 offset)
 * @param end 본문 끝 위치 (제외)
 */
public record JsFunction(String name, List<String> params, int start, int end) {

    public boolean contains(int offset) {
        return start <= offset && offset < end;
    }
}
