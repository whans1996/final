package com.vibenote.interview;

import io.swagger.v3.oas.annotations.media.Schema;

/** 질문의 근거 코드 한 곳 ({@code interview_questions.evidence} 항목, I-1a 3절). 줄 번호는 1부터, 끝 줄 포함. */
@Schema(description = "근거 코드 위치")
public record InterviewEvidence(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "파일 ID (analysis_files.id)",
                        example = "1201")
                long fileId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "파일 경로",
                        example = "src/main/java/io/spring/api/ArticlesApi.java")
                String path,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "시작 줄", example = "31")
                int startLine,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "끝 줄", example = "48")
                int endLine) {}
