package com.vibenote.team;

/** 팀 API의 Swagger Tag. 같은 Tag를 쓰는 Controller(팀, 분석 공유, 팀 목표)가 설명을 따로 적어 어긋나지 않게 한 곳에 둔다. */
public final class TeamApiDocs {

    public static final String TAG = "팀";
    public static final String TAG_DESCRIPTION =
            "팀 생성, 초대 코드 가입, 멤버 관리 (F-20a), 분석 공유·팀 이해도 (F-20b), 팀 목표 (F-20c)";

    private TeamApiDocs() {}
}
