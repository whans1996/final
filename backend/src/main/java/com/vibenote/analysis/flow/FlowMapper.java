package com.vibenote.analysis.flow;

import com.vibenote.analysis.domain.AnalysisDetail;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/analysis/FlowMapper.xml */
@Mapper
public interface FlowMapper {

    /**
     * 본인의 미삭제 분석 + overview를 {@code FOR UPDATE}로 잠근다. 같은 분석의 동시 흐름 요청이 개수 상한을 함께 통과하지 못하게 한다 (AC7).
     * 파일 수는 채우지 않는다.
     */
    Optional<AnalysisDetail> lockOwnedAnalysis(@Param("id") long id, @Param("userId") long userId);

    /** 분석 + overview (삭제 여부와 상관없음). 생성기 내부용. */
    Optional<AnalysisDetail> findAnalysis(long id);

    /** 분석의 흐름 전체, id 오름차순. */
    List<AnalysisFlow> findByAnalysis(long analysisId);

    Optional<AnalysisFlow> findById(long id);

    /** 생성된 id를 {@code flow.id}에 채운다. */
    void insert(AnalysisFlow flow);

    /** FAILED 흐름을 PENDING으로 되돌리고 결과·실패 코드를 비운다 (AC6). */
    int resetFailed(@Param("id") long id, @Param("now") LocalDateTime now);

    /** PENDING 흐름을 DONE으로. 이미 끝났으면 0. */
    int complete(
            @Param("id") long id,
            @Param("summary") String summary,
            @Param("steps") FlowStep[] steps,
            @Param("now") LocalDateTime now);

    /** PENDING 흐름을 FAILED로. 이미 끝났으면 0. */
    int markFailed(
            @Param("id") long id,
            @Param("errorCode") String errorCode,
            @Param("now") LocalDateTime now);

    /** PENDING 흐름을 모두 FAILED로 (서버 재시작, AC11). */
    int failAllPending(@Param("errorCode") String errorCode, @Param("now") LocalDateTime now);
}
