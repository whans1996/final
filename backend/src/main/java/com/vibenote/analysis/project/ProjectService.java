package com.vibenote.analysis.project;

import com.vibenote.analysis.SourceType;
import com.vibenote.analysis.domain.Analysis;
import com.vibenote.analysis.fetch.RepositoryArchiveFetcher;
import com.vibenote.analysis.fetch.RepositoryCommit;
import com.vibenote.analysis.fetch.RepositoryUrl;
import com.vibenote.analysis.pipeline.AnalysisFailedException;
import com.vibenote.analysis.project.dto.LatestCommitResponse;
import com.vibenote.analysis.project.dto.ProjectAnalysisResponse;
import com.vibenote.analysis.project.dto.ProjectDetailResponse;
import com.vibenote.analysis.project.dto.ProjectSummaryResponse;
import com.vibenote.analysis.project.dto.ProjectVersionResponse;
import com.vibenote.analysis.project.dto.UpdateProjectResponse;
import com.vibenote.common.Level;
import com.vibenote.common.config.ClockConfig;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.common.page.PageQuery;
import com.vibenote.common.page.PageResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석을 프로젝트(저장소 단위)·버전(커밋)으로 묶는다 (docs/specs/P-1a-project-versions.md). 분석 요청은 AnalysisService가 이
 * 서비스를 불러 프로젝트를 정하고, 저장소 버전은 FETCHING에서 커밋을 조회한 뒤 정한다.
 */
@Service
public class ProjectService {

    public static final String ZIP_TO_URL_PROJECT = "저장소 주소 프로젝트에는 ZIP을 올릴 수 없습니다.";
    public static final String URL_MISMATCH = "이 프로젝트의 저장소 주소와 다릅니다.";
    public static final String URL_TO_ZIP_PROJECT = "ZIP 프로젝트는 ZIP 파일로만 새 버전을 만들 수 있습니다.";

    private final ProjectMapper projectMapper;
    private final RepositoryArchiveFetcher archiveFetcher;
    private final ProjectProperties properties;
    private final Clock clock;

    public ProjectService(
            ProjectMapper projectMapper,
            RepositoryArchiveFetcher archiveFetcher,
            ProjectProperties properties,
            Clock clock) {
        this.projectMapper = projectMapper;
        this.archiveFetcher = archiveFetcher;
        this.properties = properties;
        this.clock = clock;
    }

    // ───── 프로젝트 API (AC6~AC10). 내 프로젝트이고 삭제하지 않은 분석이 있어야 보인다 (없으면 PROJECT_404) ─────

    /** 내 프로젝트 목록 (AC6). 마지막 분석 요청 최신순. */
    @Transactional(readOnly = true)
    public PageResponse<ProjectSummaryResponse> listMine(long userId, PageQuery query) {
        long total = projectMapper.countVisibleByUser(userId);
        List<ProjectListRow> rows =
                total == 0
                        ? List.of()
                        : projectMapper.findVisibleByUser(userId, query.size(), query.offset());
        if (rows.isEmpty()) {
            return PageResponse.of(List.of(), query, total);
        }
        List<Long> ids = rows.stream().map(ProjectListRow::getId).toList();
        Map<Long, List<ProjectVersion>> versions =
                projectMapper.findLiveVersions(ids).stream()
                        .collect(Collectors.groupingBy(ProjectVersion::getProjectId));
        Map<Long, List<Level>> levels =
                projectMapper.findLiveLevels(ids).stream()
                        .collect(
                                Collectors.groupingBy(
                                        Analysis::getProjectId,
                                        Collectors.mapping(
                                                Analysis::getLevel, Collectors.toList())));
        List<ProjectSummaryResponse> items =
                rows.stream()
                        .map(
                                row -> {
                                    // 쿼리가 번호 내림차순으로 주므로 첫 항목이 최신 버전이다
                                    List<ProjectVersion> live =
                                            versions.getOrDefault(row.getId(), List.of());
                                    return new ProjectSummaryResponse(
                                            row.getId(),
                                            row.getName(),
                                            row.getSourceType(),
                                            row.getSourceUrl(),
                                            live.size(),
                                            live.isEmpty()
                                                    ? null
                                                    : ProjectVersionResponse.from(live.get(0)),
                                            levels.getOrDefault(row.getId(), List.of()).stream()
                                                    .distinct()
                                                    .sorted()
                                                    .toList(),
                                            row.getLastAnalyzedAt()
                                                    .atZone(ClockConfig.ZONE)
                                                    .toOffsetDateTime());
                                })
                        .toList();
        return PageResponse.of(items, query, total);
    }

    /** 프로젝트 상세 (AC7). 분석이 모두 삭제된 버전은 뺀다. */
    @Transactional(readOnly = true)
    public ProjectDetailResponse getDetail(long userId, long projectId) {
        Project project = findVisible(projectId, userId);
        return new ProjectDetailResponse(
                project.getId(),
                project.getName(),
                project.getSourceType(),
                project.getSourceUrl(),
                project.getCreatedAt().atZone(ClockConfig.ZONE).toOffsetDateTime(),
                projectMapper.findLiveVersions(List.of(projectId)).stream()
                        .map(ProjectVersionResponse::from)
                        .toList(),
                projectMapper.findLiveAnalyses(projectId).stream()
                        .map(ProjectAnalysisResponse::from)
                        .toList());
    }

    /** 이름 바꾸기 (AC8). 분석의 {@code title}은 그대로다. */
    @Transactional
    public UpdateProjectResponse rename(long userId, long projectId, String name) {
        findVisible(projectId, userId);
        projectMapper.updateName(projectId, name, LocalDateTime.now(clock));
        return new UpdateProjectResponse(projectId, name);
    }

    /**
     * 새 커밋 배지 (AC9~AC11). ZIP은 외부 요청 없이 NOT_SUPPORTED다. 저장해 둔 조회 결과가 {@code latest-commit-ttl} 안이면
     * 그것을 쓰고(실패 포함), 아니면 트랜잭션 밖에서 기본 브랜치 최신 커밋을 조회해 저장한다. 서버 토큰으로 비공개로 바뀐 것이 확인되면(ANALYSIS_004) 조회
     * 실패와 같이 커밋 정보 없이 UNKNOWN이다.
     */
    public LatestCommitResponse latestCommit(long userId, long projectId) {
        Project project = findVisible(projectId, userId);
        if (project.getSourceType() != SourceType.URL) {
            return LatestCommitResponse.notSupported();
        }
        String analyzedSha = projectMapper.findAnalyzedSha(projectId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime checkedAt = project.getLatestCheckedAt();
        if (checkedAt != null && now.isBefore(checkedAt.plus(properties.latestCommitTtl()))) {
            RepositoryCommit cached =
                    project.getLatestCommitSha() == null
                            ? null
                            : new RepositoryCommit(
                                    project.getLatestCommitSha(),
                                    project.getLatestCommitMessage(),
                                    project.getLatestCommittedAt());
            return LatestCommitResponse.of(cached, analyzedSha, checkedAt);
        }
        RepositoryCommit latest = lookUpLatest(project);
        projectMapper.updateLatestCommit(
                projectId,
                latest == null ? null : latest.sha(),
                latest == null ? null : latest.message(),
                latest == null ? null : latest.committedAt(),
                now);
        return LatestCommitResponse.of(latest, analyzedSha, now);
    }

    private RepositoryCommit lookUpLatest(Project project) {
        try {
            return archiveFetcher
                    .latestCommit(RepositoryUrl.parse(project.getSourceUrl()))
                    .orElse(null);
        } catch (AnalysisFailedException e) {
            return null;
        }
    }

    private Project findVisible(long projectId, long userId) {
        return projectMapper
                .findVisible(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404));
    }

    /**
     * 저장소 분석이 붙을 프로젝트 (AC1·AC5). {@code projectId}가 없으면 정규화한 주소가 같은 내 프로젝트를 쓰고 없으면 만든다. 버전은
     * FETCHING에서 정한다. 호출자(분석 요청 트랜잭션)가 회원 행을 잠근 뒤 부르므로 같은 회원의 동시 요청이 프로젝트를 둘 만들지 않는다.
     */
    @Transactional
    public long assignUrl(
            long userId, Long projectId, RepositoryUrl repository, LocalDateTime now) {
        if (projectId != null) {
            Project project = lockOwned(projectId, userId);
            if (project.getSourceType() != SourceType.URL) {
                throw invalidProjectId(URL_TO_ZIP_PROJECT);
            }
            // DB 열 정렬과 같이 대소문자를 구분하지 않는다 (ProjectMapper.findByUrl)
            if (!project.getSourceUrl()
                    .toLowerCase(Locale.ROOT)
                    .equals(repository.normalized().toLowerCase(Locale.ROOT))) {
                throw invalidProjectId(URL_MISMATCH);
            }
            return project.getId();
        }
        return projectMapper
                .findByUrl(userId, repository.normalized())
                .map(Project::getId)
                .orElseGet(
                        () -> {
                            Project project = Project.newUrl(userId, repository, now);
                            projectMapper.insert(project);
                            return project.getId();
                        });
    }

    /**
     * ZIP 분석이 붙을 프로젝트와 버전 (AC4·AC5). {@code projectId}가 없으면 새 프로젝트와 버전 1, 있으면 그 프로젝트의 새 버전이다. ZIP
     * 버전에는 커밋 정보가 없다.
     */
    @Transactional
    public Assignment assignZip(long userId, Long projectId, String name, LocalDateTime now) {
        long id;
        if (projectId == null) {
            Project project = Project.newZip(userId, name, now);
            projectMapper.insert(project);
            id = project.getId();
        } else {
            Project project = lockOwned(projectId, userId);
            if (project.getSourceType() != SourceType.ZIP) {
                throw invalidProjectId(ZIP_TO_URL_PROJECT);
            }
            id = project.getId();
        }
        return new Assignment(id, insertVersion(id, null, now));
    }

    /**
     * FETCHING에서 받은 커밋의 버전 id (AC2·AC3). 같은 커밋 버전이 있으면 그것을, 없으면(커밋을 조회하지 못했으면 늘) 새 버전을 만든다. 프로젝트 행을
     * 잠가 같은 커밋을 두 수준으로 동시에 분석해도 버전은 하나다. 호출자 트랜잭션에 참여한다.
     */
    @Transactional
    public long versionFor(long projectId, RepositoryCommit commit, LocalDateTime now) {
        projectMapper
                .lock(projectId)
                .orElseThrow(() -> new IllegalStateException("프로젝트 없음: " + projectId));
        if (commit != null) {
            var existing = projectMapper.findVersionIdByCommit(projectId, commit.sha());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return insertVersion(projectId, commit, now);
    }

    private long insertVersion(long projectId, RepositoryCommit commit, LocalDateTime now) {
        ProjectVersion version =
                ProjectVersion.of(
                        projectId, projectMapper.nextVersionNumber(projectId), commit, now);
        projectMapper.insertVersion(version);
        return version.getId();
    }

    private Project lockOwned(long projectId, long userId) {
        return projectMapper
                .lockOwned(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_404));
    }

    private static BusinessException invalidProjectId(String message) {
        return new BusinessException(ErrorCode.COMMON_400, Map.of("projectId", message));
    }

    /** 분석이 붙을 프로젝트와 (정해졌으면) 버전. */
    public record Assignment(long projectId, Long versionId) {}
}
