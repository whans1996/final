package com.vibenote.analysis;

/** 분석 API의 Swagger Tag. 같은 Tag를 쓰는 Controller(분석, 구조 그래프, 흐름)가 설명을 따로 적어 어긋나지 않게 한 곳에 둔다. */
public final class AnalysisApiDocs {

    public static final String TAG = "분석";
    public static final String TAG_DESCRIPTION =
            "프로젝트 분석 요청, 내 분석 목록, 진행 상태, 분석 결과(개요·파일·개념·구조 그래프) 조회, 실행 흐름 요청·조회, 수정·공개·삭제";

    private AnalysisApiDocs() {}
}
