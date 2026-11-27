package com.vibenote.analysis.project;

import com.vibenote.analysis.SourceType;
import com.vibenote.analysis.fetch.RepositoryCommit;
import com.vibenote.analysis.fetch.RepositoryUrl;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
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

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
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
