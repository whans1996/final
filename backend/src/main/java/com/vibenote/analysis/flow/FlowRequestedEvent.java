package com.vibenote.analysis.flow;

import java.util.List;

/** 실행 흐름 생성이 요청됨 (새 행 또는 FAILED에서 되돌린 행). 요청 트랜잭션이 커밋된 뒤 생성을 시작하는 데 쓴다. */
public record FlowRequestedEvent(List<Long> flowIds) {}
