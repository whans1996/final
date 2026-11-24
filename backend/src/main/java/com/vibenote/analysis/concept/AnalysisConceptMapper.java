package com.vibenote.analysis.concept;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/analysis/AnalysisConceptMapper.xml. analysis_concepts를 쓰고 읽는다 (F-08). */
@Mapper
public interface AnalysisConceptMapper {

    /**
     * links 중 지금 concepts에 남아 있는 카드만 넣는다 (#77). links가 비면 부르지 않는다 (빈 SELECT는 SQL 오류).
     *
     * @return 넣은 행 수
     */
    int insertAll(@Param("analysisId") long analysisId, @Param("links") List<LinkRow> links);

    /**
     * 분석의 연결 전체와 카드 상태·파일 경로. 파일이 있는 것 먼저 {@code file_id} 오름차순, 그다음 {@code concept_id} 오름차순 (AC7).
     */
    List<LinkRow> findByAnalysis(@Param("analysisId") long analysisId);

    /** 퀴즈 프롬프트의 개념 목록 (F-14 AC2). HIDDEN 카드는 빼고 {@code concept_id} 오름차순. */
    List<LinkedConcept> findForQuiz(@Param("analysisId") long analysisId);

    /**
     * analysis_concepts 행 (+ 조회 때 카드 상태·파일 경로).
     *
     * @param status 조회 때만 채운다 ({@code concepts.status})
     * @param filePath 조회 때만 채운다
     */
    record LinkRow(
            long conceptId,
            Long fileId,
            String lineHint,
            String contextNote,
            String status,
            String filePath) {

        static LinkRow of(long conceptId, Long fileId, String lineHint, String contextNote) {
            return new LinkRow(conceptId, fileId, lineHint, contextNote, null, null);
        }
    }
}
