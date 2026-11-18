package com.vibenote.analysis.pipeline;

/** ZIP에서 읽은 지원 파일. path는 공통 최상위 폴더를 뗀 상대 경로('/' 구분). */
public record SourceFile(String path, String content) {}
