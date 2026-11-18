package com.vibenote.analysis.pipeline;

/** 선별되어 analysis_files에 저장할 파일. */
public record SelectedFile(String path, Layer layer, String content) {}
