package com.vibenote.analysis.project;

import com.vibenote.analysis.domain.Analysis;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** projects·project_versions (P-1a). SQL은 mapper/analysis/ProjectMapper.xml. */
@Mapper
public interface ProjectMapper {

    void insert(Project project);

    /** 내 프로젝트를 잠근다. 같은 프로젝트의 버전 번호를 차례로 정하려는 것이다. */
    Optional<Project> lockOwned(@Param("id") long id, @Param("userId") long userId);

    /** 프로젝트를 잠근다 (FETCHING 버전 정하기, 회원 확인 없음). */
    Optional<Long> lock(@Param("id") long id);

    Optional<Project> findByUrl(@Param("userId") long userId, @Param("sourceUrl") String sourceUrl);

    /** 그 프로젝트의 다음 버전 번호 (최대 + 1, 처음이면 1). 호출자가 프로젝트 행을 잠근 뒤 부른다. */
    int nextVersionNumber(@Param("projectId") long projectId);

    void insertVersion(ProjectVersion version);

    Optional<Long> findVersionIdByCommit(
            @Param("projectId") long projectId, @Param("commitSha") String commitSha);

    // ── 프로젝트 API (AC6~AC10). "보이는 프로젝트" = 내 프로젝트이고 삭제하지 않은 분석이 하나 이상 있음 ──

    Optional<Project> findVisible(@Param("id") long id, @Param("userId") long userId);

    /** 보이는 내 프로젝트를 마지막 분석 요청 최신순으로 (AC6). */
    List<ProjectListRow> findVisibleByUser(
            @Param("userId") long userId, @Param("size") int size, @Param("offset") long offset);

    long countVisibleByUser(@Param("userId") long userId);

    /** 삭제하지 않은 분석이 붙은 버전만, 프로젝트별 번호 내림차순. */
    List<ProjectVersion> findLiveVersions(@Param("projectIds") List<Long> projectIds);

    /** 삭제하지 않은 분석의 (projectId, level) 중복 없이. */
    List<Analysis> findLiveLevels(@Param("projectIds") List<Long> projectIds);

    /** 삭제하지 않은 분석 (요청 최신순). id·버전·수준·상태·퀴즈 상태·제목·요청 시각만 채운다. */
    List<Analysis> findLiveAnalyses(@Param("projectId") long projectId);

    /** 분석한 최신 커밋: 커밋 정보가 있고 삭제하지 않은 분석이 붙은 버전 중 번호가 가장 큰 것 (AC9). */
    Optional<String> findAnalyzedSha(@Param("projectId") long projectId);

    void updateName(
            @Param("id") long id,
            @Param("name") String name,
            @Param("updatedAt") LocalDateTime updatedAt);

    /** 배지 조회 결과 저장 (AC10). 실패면 커밋 필드가 모두 null이고 {@code checkedAt}만 채운다. */
    void updateLatestCommit(
            @Param("id") long id,
            @Param("sha") String sha,
            @Param("message") String message,
            @Param("committedAt") LocalDateTime committedAt,
            @Param("checkedAt") LocalDateTime checkedAt);
}
