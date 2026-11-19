package com.vibenote.concept.dto;

import com.vibenote.concept.domain.ConceptAppearance;
import io.swagger.v3.oas.annotations.media.Schema;

/** 로그인 회원의 분석에서 이 카드가 나온 위치 (F-10 AC3). */
@Schema(description = "내 코드에서 이 개념이 등장한 위치")
public record ConceptAppearanceResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분석 ID", example = "31")
                long analysisId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "분석 제목 (없으면 프로젝트 이름, 둘 다 없으면 \"제목 없는 분석\")",
                        example = "쇼핑몰 과제")
                String analysisTitle,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "등장한 파일 ID (파일을 특정하지 못했으면 null)",
                        example = "412")
                Long fileId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "등장한 파일 경로 (fileId가 null이면 null)",
                        example = "src/main/java/com/shop/security/JwtProvider.java")
                String filePath,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "줄 위치 힌트",
                        example = "L24-L40")
                String lineHint,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        description = "이 코드에서 개념이 쓰인 맥락",
                        example = "로그인 성공 시 토큰을 만든다")
                String contextNote) {

    static final String UNTITLED = "제목 없는 분석";

    public static ConceptAppearanceResponse from(ConceptAppearance row) {
        String title = row.getAnalysisTitle();
        return new ConceptAppearanceResponse(
                row.getAnalysisId(),
                title == null || title.isBlank() ? UNTITLED : title,
                row.getFileId(),
                row.getFilePath(),
                row.getLineHint(),
                row.getContextNote());
    }
}
