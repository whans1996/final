package com.vibenote.analysis.project;

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
}
