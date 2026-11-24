package com.vibenote.analysis.structure;

/** 확인하지 못한 연결의 종류 (F-21a 3.1절). */
public enum UnresolvedKind {
    IMPORT,
    PARSE,
    HTTP_CALL
}
