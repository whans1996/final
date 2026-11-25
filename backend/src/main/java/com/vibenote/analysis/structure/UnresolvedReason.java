package com.vibenote.analysis.structure;

/** 확인하지 못한 연결의 사유 (F-21a 3.1절). */
public enum UnresolvedReason {
    NOT_SELECTED,
    PARSE_ERROR,
    URL_UNKNOWN,
    BASE_URL_UNKNOWN,
    NO_MATCHING_API,
    AMBIGUOUS
}
