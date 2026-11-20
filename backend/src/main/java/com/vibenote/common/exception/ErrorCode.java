package com.vibenote.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 목록. 형식은 {@code <도메인>_<HTTP 상태 또는 3자리 번호>} (docs/conventions.md 3절).
 *
 * <p>새 코드는 spec의 "API 계약 &gt; 에러" 표에 먼저 적고 여기에 추가한다. message는 사용자에게 그대로 보여 줄 수 있는 한국어 문장이다.
 *
 * <p>두 사람이 병렬로 추가하므로 <b>자기 도메인 블록 안에만</b> 넣고, 모든 항목 끝에 쉼표를 단다({@code ;}는 맨 아래 단독 줄). 파일 끝에 이어 붙이면
 * 같은 줄에서 충돌한다 (docs/conventions.md 9절, F-00b).
 */
public enum ErrorCode {
    // ── COMMON (공통) ──
    COMMON_400(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    COMMON_404(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    COMMON_405(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    COMMON_413(HttpStatus.CONTENT_TOO_LARGE, "업로드할 수 있는 크기를 넘었습니다."),
    COMMON_415(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    COMMON_500(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    // ── AUTH (공통 F-02) ──
    AUTH_401(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    AUTH_403(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    /** 로그인 실패. 이메일 없음·비밀번호 불일치·탈퇴 회원을 구분하지 않는다 (F-02). */
    AUTH_001(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    /** Refresh 토큰 없음·무효·만료·회전됨. FE는 재시도하지 않고 로그인 화면으로 보낸다 (F-02). */
    AUTH_002(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인하세요."),

    // ── USER (공통 F-01·F-03) ──
    /** 이메일·닉네임 중복. details에 해당 필드를 담는다 (F-01). */
    USER_409(HttpStatus.CONFLICT, "이미 사용 중인 정보가 있습니다."),
    /** 없는 회원 또는 탈퇴 회원의 공개 프로필·통계 (F-03). */
    USER_404(HttpStatus.NOT_FOUND, "존재하지 않거나 탈퇴한 사용자입니다."),
    /** 탈퇴 시 비밀번호 재확인 실패. details.password에 사유를 담는다 (F-03). */
    USER_001(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다."),

    // ── ANALYSIS (A F-05~07·F-19) ──
    /** 없는 분석, 다른 회원의 분석, soft delete된 분석. 존재 여부를 드러내지 않으려고 403 대신 404 (F-05a). */
    ANALYSIS_404(HttpStatus.NOT_FOUND, "분석을 찾을 수 없습니다."),
    /** 일일 분석 한도 초과. {limit}은 AnalysisService가 설정값(analysis.daily-limit)으로 바꾼다 (F-05a). */
    ANALYSIS_429(HttpStatus.TOO_MANY_REQUESTS, "오늘 분석 요청 횟수({limit}회)를 모두 사용했습니다. 내일 다시 시도해 주세요."),
    // 아래 4개는 응답이 아니라 파이프라인 FAILED 사유(analyses.error_code)다. 화면 문구는 analyses.error_message에 따로 저장한다
    // (F-05a 5.3절).
    /** 깨진 ZIP, 루트 밖·절대 경로 엔트리. */
    ANALYSIS_400(HttpStatus.BAD_REQUEST, "ZIP 파일을 읽을 수 없습니다. 손상되었거나 허용되지 않는 경로가 들어 있습니다."),
    /** 지원 파일 200개 초과 또는 합계 10MB 초과. */
    ANALYSIS_413(HttpStatus.CONTENT_TOO_LARGE, "코드 파일이 너무 많거나 큽니다. 줄여서 다시 올려 주세요."),
    /** 지원 스택 코드(.java·.js·.mjs·.vue)가 없음. */
    ANALYSIS_415(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "분석할 수 있는 코드가 없습니다. Java·Spring·MyBatis·JavaScript·Vue 프로젝트만 지원합니다."),
    /** 예기치 못한 오류, 실행기 거절, 서버 재시작. */
    ANALYSIS_500(HttpStatus.INTERNAL_SERVER_ERROR, "분석 중 오류가 발생했습니다. 다시 요청해 주세요."),
    /** 분석은 내 것이지만 fileId가 그 분석의 파일이 아님 (F-06 AC11). */
    ANALYSIS_001(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    /** 실행 흐름을 요청한 API가 개요 apis에 없음 (F-07 AC8). */
    ANALYSIS_002(HttpStatus.BAD_REQUEST, "탐지된 API가 아닙니다. 개요의 API 목록에서 골라 주세요."),
    /** 요청 후 FAILED가 아닌 흐름이 분석당 상한(3개)을 넘음 (F-07 AC7). */
    ANALYSIS_003(HttpStatus.BAD_REQUEST, "실행 흐름은 분석 하나에 3개까지 만들 수 있습니다."),
    /** 분석이 DONE이 아닐 때 실행 흐름 요청 (F-07 AC9). */
    ANALYSIS_409(HttpStatus.CONFLICT, "분석이 끝난 뒤에 실행 흐름을 요청할 수 있습니다."),

    // ── CONCEPT (A F-08 · B F-09~13) ──
    /** 없는 카드, 또는 PUBLISHED가 아닌 카드(DRAFT·HIDDEN)를 일반 사용자가 조회 (F-00b, NF-04). */
    CONCEPT_404(HttpStatus.NOT_FOUND, "개념 카드를 찾을 수 없습니다."),
    /** 수정·삭제할 내 리뷰가 그 카드에 없음 (F-11 AC10). */
    CONCEPT_001(HttpStatus.NOT_FOUND, "작성한 리뷰가 없습니다."),
    /** 한 카드에 두 번째 리뷰 (F-11 AC6, UNIQUE (concept_id, user_id)). */
    CONCEPT_409(HttpStatus.CONFLICT, "이미 이 카드에 리뷰를 남겼습니다."),

    // ── QUIZ (A F-14) ──
    /** 없는 퀴즈, 남의·삭제된 분석의 퀴즈, 또는 내 분석이지만 퀴즈가 없음(생성 실패·QUIZ 전) (F-14 AC5·AC8). */
    QUIZ_404(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다."),
    /** 제출이 그 퀴즈의 문항마다 정확히 한 번씩 답하지 않음 (누락·중복·다른 퀴즈의 문항, F-14 AC8). */
    QUIZ_400(HttpStatus.BAD_REQUEST, "모든 문항에 한 번씩 답해 주세요."),

    // ── REVIEW_PLAN (A F-15) ──
    /** 없는 계획, 다른 회원의 계획, PUBLISHED가 아닌 카드의 계획 (F-15 AC7). */
    REVIEW_PLAN_404(HttpStatus.NOT_FOUND, "복습 계획을 찾을 수 없습니다."),
    /** 그 카드의 TODO 계획이 이미 있음, 같은 날짜의 계획이 있음, DONE→TODO인데 다른 TODO가 있음 (F-15 AC5·AC6). */
    REVIEW_PLAN_409(HttpStatus.CONFLICT, "이미 복습 계획에 있는 카드입니다."),

    // ── POST (B F-17·F-18) ──
    /** 없는 글의 상세·수정·삭제 (F-17 AC4·AC11). */
    POST_404(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    /** 남의 글 수정·삭제 (F-17 AC11). 형식 검증(400)보다 먼저 판단한다. */
    POST_403(HttpStatus.FORBIDDEN, "본인이 쓴 글만 수정·삭제할 수 있습니다."),
    /** 남의 분석·DONE 아님·삭제됨·없는 id를 구분하지 않는다 (F-17 AC8). */
    POST_001(HttpStatus.BAD_REQUEST, "첨부할 수 없는 분석입니다."),
    /** PUBLISHED가 아니거나 없는 카드 (F-17 AC8). */
    POST_002(HttpStatus.BAD_REQUEST, "첨부할 수 없는 개념 카드입니다."),
    /** 없는 댓글이거나 경로의 글에 속하지 않는 댓글 (F-18 AC9). */
    POST_003(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    /** 남의 댓글 수정·삭제 (F-18 AC9). 글 작성자여도 같고, 형식 검증(400)보다 먼저 판단한다. */
    POST_004(HttpStatus.FORBIDDEN, "본인이 쓴 댓글만 수정·삭제할 수 있습니다."),

    // ── LLM (A) ──
    /** 개요 응답이 1회 재시도 후에도 스키마 위반. 파이프라인 FAILED 사유다 (F-06 AC5). */
    LLM_502(HttpStatus.BAD_GATEWAY, "AI 응답 형식이 올바르지 않아 설명을 만들지 못했습니다. 다시 요청해 주세요."),
    /** 설정된 LLM이 폴백까지 모두 응답하지 못함. 호출자는 파이프라인 단계를 FAILED로 표시한다 (F-00). */
    LLM_503(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요."),

// ── CHALLENGE (A F-24·F-25) ──

;

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
