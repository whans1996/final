package com.vibenote.common;

/** 관리자 API의 Swagger Tag. 관리자 Controller가 여러 도메인(개념 카드, 챌린지)에 나뉘어 있어 설명이 어긋나지 않게 공통 패키지 한 곳에 둔다. */
public final class AdminApiDocs {

    public static final String TAG = "관리자";
    public static final String TAG_DESCRIPTION = "관리자 전용 기능 (개념 카드 검수·관리, 학습 챌린지 관리)";

    private AdminApiDocs() {}
}
