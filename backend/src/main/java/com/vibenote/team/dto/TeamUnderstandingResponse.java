package com.vibenote.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 팀 공유 분석의 모듈별·멤버별 이해도 (F-20b AC6·AC7). 멤버마다 가장 최근 풀이만 센다. */
@Schema(description = "팀 이해도")
public record TeamUnderstandingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "42")
                long analysisId,
        @Schema(description = "분석 제목 (없으면 null)", example = "spring-realworld") String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "공유한 사람")
                SharedByResponse sharedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "퀴즈 문항 수", example = "4")
                int questionCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "지금 멤버 수", example = "3")
                int memberCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "푼 멤버 수", example = "2")
                int solvedMemberCount,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "레이어별 이해도 (controller, service, mapper, vue, js, config, domain,"
                                        + " other 순서, 문항이 있는 레이어만)")
                List<ModuleUnderstandingResponse> modules,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "멤버별 결과 (팀장 먼저, 가입 순)")
                List<MemberUnderstandingResponse> members) {}
