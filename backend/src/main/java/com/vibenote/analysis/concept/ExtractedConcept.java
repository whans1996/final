package com.vibenote.analysis.concept;

import com.vibenote.common.Level;

/**
 * 검증을 통과한 응답 항목 하나. 기존 카드 연결이면 {@code conceptId}, 새 초안이면 {@code draft}가 있다 (둘 중 하나만).
 *
 * @param fileId 개념이 쓰인 {@code analysis_files.id} (없으면 null)
 * @param lineHint {@code L12} 또는 {@code L12-L30} (없으면 null)
 * @param contextNote 그 코드에서 개념이 어떻게 쓰였는지 (없으면 null)
 */
public record ExtractedConcept(
        Long conceptId, NewConcept draft, Long fileId, String lineHint, String contextNote) {

    /** AI 초안 카드의 내용 (F-08 AC5). */
    public record NewConcept(
            String name, String category, Level difficulty, String summary, String body) {}
}
