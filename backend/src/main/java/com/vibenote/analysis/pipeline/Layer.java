package com.vibenote.analysis.pipeline;

import java.util.Locale;

/** 파일 layer (analysis_files.layer). 선언 순서가 선별 우선순위다 (F-05a 5.2절). */
public enum Layer {
    CONTROLLER,
    SERVICE,
    MAPPER,
    VUE,
    JS,
    CONFIG,
    DOMAIN,
    OTHER;

    /** DB에 저장하는 소문자 값. */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
