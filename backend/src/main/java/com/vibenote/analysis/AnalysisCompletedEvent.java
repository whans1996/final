package com.vibenote.analysis;

import java.time.LocalDate;

/**
 * 분석이 DONE이 됨. 완료 트랜잭션이 커밋된 뒤 분석 챌린지 달성도를 다시 세는 데 쓴다 (F-25 AC7, #105).
 *
 * @param requestDate 분석 요청 시각({@code created_at})의 Asia/Seoul 날짜
 */
public record AnalysisCompletedEvent(long analysisId, long userId, LocalDate requestDate) {}
