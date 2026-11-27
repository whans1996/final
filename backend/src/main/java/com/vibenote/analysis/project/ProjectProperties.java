package com.vibenote.analysis.project;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 프로젝트 설정 ({@code analysis.project.*}, docs/specs/P-1a-project-versions.md AC10).
 *
 * @param latestCommitTtl 새 커밋 배지 조회 결과(실패 포함)를 다시 쓰는 시간. 그 안에는 외부 요청을 하지 않는다
 */
@ConfigurationProperties("analysis.project")
public record ProjectProperties(@DefaultValue("5m") Duration latestCommitTtl) {}
