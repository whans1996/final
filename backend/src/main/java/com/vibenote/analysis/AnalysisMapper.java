package com.vibenote.analysis;

import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.domain.AnalysisDetail;
import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.explain.AnalysisOverview;
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

    /** 분석의 파일 전체(content 포함), id 오름차순 = 선별 우선순위. 파이프라인 내부용. */
    List<AnalysisFile> findFiles(long analysisId);

    /** 분석의 파일 목록(content 제외), id 오름차순 (F-06 AC10). */
    List<AnalysisFile> findFileSummaries(long analysisId);

    /** 그 분석의 파일 하나(content 포함). 다른 분석의 파일이면 empty (F-06 AC11). */
    Optional<AnalysisFile> findFile(
            @Param("analysisId") long analysisId, @Param("fileId") long fileId);

    /** 볼 수 있는 분석: 미삭제이고 내 것이거나 공개·DONE이고 주인이 ACTIVE (F-19). 아니면 empty. */
    Optional<Analysis> findViewable(@Param("id") long id, @Param("userId") long userId);

    /** 볼 수 있는 분석 + overview + 파일 수 (F-06 AC9) + 주인 닉네임 (F-19 AC9). */
    Optional<AnalysisDetail> findViewableDetail(@Param("id") long id, @Param("userId") long userId);

    /** 내 미삭제 분석의 제목·메모를 바꾼다 (F-19 AC1). 바꾼 행 수. */
    int updateInfo(
            @Param("id") long id,
            @Param("userId") long userId,
            @Param("title") String title,
            @Param("memo") String memo,
            @Param("now") LocalDateTime now);

    /** 내 미삭제 분석의 공개 여부. 공개는 DONE일 때만 바꾼다 (F-19 AC4·AC5). 바꾼 행 수. */
    int updateVisibility(
            @Param("id") long id,
            @Param("userId") long userId,
            @Param("isPublic") boolean isPublic,
            @Param("now") LocalDateTime now);

    /** 내 미삭제 DONE·FAILED 분석에 deleted_at을 채운다 (F-19 AC6·AC7). 바꾼 행 수. */
    int softDelete(
            @Param("id") long id, @Param("userId") long userId, @Param("now") LocalDateTime now);

    /** 진행 중이고 삭제되지 않은 분석이면 행을 잠그고 id를 돌려준다 (#91). */
    Optional<Long> lockInProgress(long id);

    /** 회원의 미삭제 분석 id 전체 (진행 중 포함). 회원 탈퇴 정리용 (#91). */
    List<Long> findUndeletedIds(long userId);

    void deleteConceptLinks(long analysisId);

    void deleteFlows(long analysisId);

    void deleteFiles(long analysisId);

    void updateOverview(@Param("id") long id, @Param("overview") AnalysisOverview overview);

    void updateFileExplanation(
            @Param("analysisId") long analysisId,
            @Param("fileId") long fileId,
            @Param("role") String role,
            @Param("summary") String summary);

    /** 진행 중인 미삭제 분석을 모두 FAILED로 바꾼다 (progress는 그대로, 재시작 복구). */
    int failInterrupted(
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("now") LocalDateTime now);
}
