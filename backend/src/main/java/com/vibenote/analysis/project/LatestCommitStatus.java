package com.vibenote.analysis.project;

/** 새 커밋 배지 상태 (docs/specs/P-1a-project-versions.md AC9, 3절 표). */
public enum LatestCommitStatus {
    /** 기본 브랜치 최신 커밋 ≠ 분석한 최신 커밋. */
    NEW_COMMIT,
    /** 같다. */
    UP_TO_DATE,
    /** 조회 실패·비공개로 바뀜·분석한 커밋 없음. */
    UNKNOWN,
    /** ZIP 프로젝트. 외부 요청을 하지 않는다. */
    NOT_SUPPORTED
}
