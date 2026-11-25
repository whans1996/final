package com.vibenote.analysis.pipeline;

/**
 * 인덱스 파일 하나와 선별 결과 (F-26a 5.4절).
 *
 * @param selected 설명 대상으로 선별됨
 * @param importance 선별 점수. test·migration·doc은 null
 */
public record RankedFile(
        String path, Layer layer, String content, boolean selected, Integer importance) {}
