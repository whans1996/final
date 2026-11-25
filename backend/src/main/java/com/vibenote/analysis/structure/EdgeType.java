package com.vibenote.analysis.structure;

/** 구조 그래프 간선 종류 (F-21a). 선언 순서가 응답 정렬 순서다 (AC12). */
public enum EdgeType {
    /** 코드 참조: Java 타입 이름, JS import (AC1·AC6) */
    IMPORT,
    /** Mapper 인터페이스 ↔ MyBatis XML (AC4) */
    MYBATIS,
    /** 화면·JS → API를 받는 컨트롤러 (AC11) */
    HTTP
}
