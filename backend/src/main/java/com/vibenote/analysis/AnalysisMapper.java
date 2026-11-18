package com.vibenote.analysis;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL: resources/mapper/analysis/AnalysisMapper.xml */
@Mapper
public interface AnalysisMapper {

    /** 생성된 id를 {@code analysis.id}에 채운다. created_at·updated_at은 서비스의 Clock 값으로 넣는다. */
    void insert(Analysis analysis);

    /** since 이후 만든 분석 중 FAILED가 아닌 것의 수. 일일 한도용이라 soft delete된 분석도 센다 (V2 규칙). */
    long countRequestedSince(@Param("userId") long userId, @Param("since") LocalDateTime since);

    /** 내 미삭제 분석, created_at 최신순(같으면 id 내림차순). */
    List<Analysis> findByUser(
            @Param("userId") long userId, @Param("size") int size, @Param("offset") long offset);

    long countByUser(long userId);

    /** 본인의 미삭제 분석. 없거나 남의 것이거나 삭제됐으면 empty. */
    Optional<Analysis> findOwned(@Param("id") long id, @Param("userId") long userId);

    /** 진행 중인 분석을 FAILED로 바꾼다 (progress는 그대로). 이미 DONE·FAILED면 바꾸지 않는다. */
    int markFailed(
            @Param("id") long id,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("now") LocalDateTime now);

    /** id로 찾는다 (삭제 여부와 상관없음). 파이프라인 내부용. */
    Optional<Analysis> findById(long id);

    /** 단계 진입. 이미 DONE·FAILED면 바꾸지 않는다 (0 반환). */
    int updateStage(
            @Param("id") long id,
            @Param("status") AnalysisStatus status,
            @Param("progress") int progress,
            @Param("now") LocalDateTime now);

    void insertFiles(@Param("files") List<AnalysisFile> files);

    /** 진행 중인 미삭제 분석을 모두 FAILED로 바꾼다 (progress는 그대로, 재시작 복구). */
    int failInterrupted(
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("now") LocalDateTime now);
}
