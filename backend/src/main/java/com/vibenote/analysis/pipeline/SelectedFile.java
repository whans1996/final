package com.vibenote.analysis.pipeline;

/**
 * analysis_files에 저장할 인덱스 파일 하나 (F-26a 5.5절).
 *
 * @param selected 설명 대상으로 선별됨
 * @param importance 선별 점수. test·migration·doc은 null
 */
public record SelectedFile(
        String path, Layer layer, String content, boolean selected, Integer importance) {

    /** 선별 파일 (점수 없음). */
    public SelectedFile(String path, Layer layer, String content) {
        this(path, layer, content, true, null);
    }
}
