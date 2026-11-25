package com.vibenote.analysis.pipeline;

import java.util.Locale;

/**
 * 파일 layer (analysis_files.layer). 선언 순서가 선별 우선순위·FE 표시 순서다 (F-05a 5.2절). TEST·MIGRATION·DOC은 인덱스에만
 * 담고 선별하지 않는다 (F-26a 5.3절).
 */
public enum Layer {
    CONTROLLER,
    SERVICE,
    MAPPER,
    VUE,
    JS,
    CONFIG,
    DOMAIN,
    OTHER,
    TEST,
    MIGRATION,
    DOC;

    /** DB에 저장하는 소문자 값. */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 인덱스에만 담고 선별·개요 파일 목록 후보에서 빼는 layer. */
    public boolean indexOnly() {
        return this == TEST || this == MIGRATION || this == DOC;
    }
}
