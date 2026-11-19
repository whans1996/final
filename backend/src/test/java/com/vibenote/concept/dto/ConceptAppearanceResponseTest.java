package com.vibenote.concept.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.concept.domain.ConceptAppearance;
import org.junit.jupiter.api.Test;

/** F-10 AC3 분석 제목 대체. 제목·프로젝트 이름 선택은 SQL COALESCE가, 둘 다 없을 때만 여기서 채운다. */
class ConceptAppearanceResponseTest {

    private static ConceptAppearance row(String title) {
        ConceptAppearance row = new ConceptAppearance();
        row.setAnalysisId(7);
        row.setAnalysisTitle(title);
        return row;
    }

    @Test
    void AC3_제목_대체() {
        assertThat(ConceptAppearanceResponse.from(row("todo-app")).analysisTitle())
                .isEqualTo("todo-app");
        assertThat(ConceptAppearanceResponse.from(row(null)).analysisTitle()).isEqualTo("제목 없는 분석");
        assertThat(ConceptAppearanceResponse.from(row("  ")).analysisTitle()).isEqualTo("제목 없는 분석");
    }

    @Test
    void AC3_파일이_없으면_fileId와_filePath가_null이다() {
        ConceptAppearanceResponse response = ConceptAppearanceResponse.from(row("t"));

        assertThat(response.analysisId()).isEqualTo(7);
        assertThat(response.fileId()).isNull();
        assertThat(response.filePath()).isNull();
    }
}
