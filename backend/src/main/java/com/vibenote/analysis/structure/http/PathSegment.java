package com.vibenote.analysis.structure.http;

/**
 * 계산한 FE 요청 경로의 세그먼트 하나 (F-21a AC8). 동적 조각이 들어간 세그먼트는 와일드카드다.
 *
 * @param literal 세그먼트 값. 와일드카드면 null
 */
public record PathSegment(String literal) {

    public static final PathSegment WILDCARD = new PathSegment(null);

    public static PathSegment literal(String value) {
        return new PathSegment(value);
    }

    public boolean wildcard() {
        return literal == null;
    }

    @Override
    public String toString() {
        return wildcard() ? "*" : literal;
    }
}
