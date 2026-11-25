package com.vibenote.analysis.structure.http;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 코드에서 계산한 FE 요청 (F-21a AC8~AC11).
 *
 * @param method 대문자 HTTP 메서드. 계산하지 못했으면 null
 * @param path baseURL까지 붙인 경로 세그먼트
 */
public record FeRequest(String method, List<PathSegment> path) {

    /** {@code GET /api/todos/*}. 메서드를 모르면 경로만. */
    @Override
    public String toString() {
        String p =
                path.stream().map(PathSegment::toString).collect(Collectors.joining("/", "/", ""));
        return method == null ? p : method + " " + p;
    }
}
