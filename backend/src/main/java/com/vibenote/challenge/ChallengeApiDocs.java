package com.vibenote.challenge;

/** 챌린지 API의 Swagger Tag. 같은 Tag를 쓰는 Controller(목록·상세·참여, 참여한 챌린지)가 설명을 따로 적어 어긋나지 않게 한 곳에 둔다. */
public final class ChallengeApiDocs {

    public static final String TAG = "챌린지";
    public static final String TAG_DESCRIPTION = "학습 챌린지 목록·상세, 참여·취소, 참여한 챌린지와 달성도";

    private ChallengeApiDocs() {}
}
