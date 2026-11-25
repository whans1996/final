package com.vibenote.analysis.pipeline;

import java.util.List;

/**
 * ZIP에서 읽은 인덱스 (F-26a 5.2절).
 *
 * @param files 인덱스에 담은 파일. (묶음, 경로) 순서
 * @param dropped 필터는 통과했지만 상한(개수·합계·단일 파일 크기)으로 버린 파일 수
 */
public record IndexResult(List<SourceFile> files, int dropped) {}
