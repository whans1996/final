package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * F-26a V12 (docs/specs/F-26a-index-selection.md AC10). V12 전 코드처럼 selected·importance를 적지 않고 넣은 행이
 * 선별 파일로 남는지, 새 분석 열이 비어 있는지 본다.
 */
class AnalysisMigrationTest extends AnalysisTestSupport {

    @Test
    void AC10_V12_전_방식으로_넣은_파일은_selected이고_인덱스_수치는_NULL이다() {
        long id = insertAnalysis(createUser("f26ac10", "JUNIOR"), "DONE", null, false, "old");
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content)"
                        + " VALUES (?, 'A.java', 'controller', 'class A {}')",
                id);

        Map<String, Object> file =
                jdbc.queryForMap(
                        "SELECT selected, importance FROM analysis_files WHERE analysis_id = ?",
                        id);
        assertThat(file.get("selected")).isEqualTo(true);
        assertThat(file.get("importance")).isNull();

        Map<String, Object> analysis = analysisRow(id);
        assertThat(analysis).containsKeys("indexed_files", "dropped_files");
        assertThat(analysis.get("indexed_files")).isNull();
        assertThat(analysis.get("dropped_files")).isNull();
    }
}
