package com.vibenote.analysis.structure;

import io.swagger.v3.oas.annotations.media.Schema;

/** 코드만으로 확정하지 못한 연결 하나와 그 사유 (F-21a 3.1절). */
@Schema(description = "확인하지 못한 연결")
public record UnresolvedLink(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "연결이 나온 파일 ID",
                        example = "102")
                long fileId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "줄 번호 (1부터)",
                        example = "5")
                int line,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "종류", example = "IMPORT")
                UnresolvedKind kind,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "사유",
                        example = "NOT_SELECTED")
                UnresolvedReason reason,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "FQN·경로·요청 원문 등 근거",
                        example = "com.example.todo.TodoValidator")
                String detail) {}
