package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

/** 서버 재시작 시 중단된 분석 정리 (docs/specs/F-05a-analysis-request-zip.md AC11). 복구 메서드를 직접 부른다. */
class AnalysisRecoveryTest extends AnalysisTestSupport {

    private static final String RESTART_MESSAGE = "서버가 다시 시작되어 분석이 중단되었습니다. 다시 요청해 주세요.";

    @Autowired AnalysisRecovery recovery;

    @Test
    void AC11_진행_중이던_분석을_FAILED로_바꾸고_임시_파일을_지운다() throws IOException {
        long userId = createUser("ac11", "JUNIOR");
        Map<String, Long> ids = new LinkedHashMap<>();
        for (String status :
                List.of(
                        "FETCHING",
                        "FILTERING",
                        "SELECTING",
                        "EXPLAINING",
                        "EXTRACTING",
                        "QUIZ",
                        "DONE",
                        "FAILED")) {
            ids.put(status, insertAnalysis(userId, status, null, false, status));
        }
        jdbc.update("UPDATE analyses SET progress = 15 WHERE id = ?", ids.get("FILTERING"));
        jdbc.update(
                "UPDATE analyses SET error_code = 'ANALYSIS_415', error_message = 'old'"
                        + " WHERE id = ?",
                ids.get("FAILED"));
        long deleted = insertAnalysis(userId, "SELECTING", null, true, "deleted");
        Path leftover = tempZip(ids.get("FETCHING"));
        Files.createDirectories(leftover.getParent());
        Files.write(leftover, new byte[] {1});
        Path stray = leftover.resolveSibling("999999.zip");
        Files.write(stray, new byte[] {1});

        recovery.recover();

        for (String status :
                List.of("FETCHING", "FILTERING", "SELECTING", "EXPLAINING", "EXTRACTING", "QUIZ")) {
            Map<String, Object> row = analysisRow(ids.get(status));
            assertThat(row.get("status")).as(status).isEqualTo("FAILED");
            assertThat(row.get("error_code")).as(status).isEqualTo("ANALYSIS_500");
            assertThat(row.get("error_message")).as(status).isEqualTo(RESTART_MESSAGE);
        }
        assertThat(analysisRow(ids.get("FILTERING")).get("progress")).isEqualTo(15);
        assertThat(analysisRow(ids.get("DONE")).get("status")).isEqualTo("DONE");
        assertThat(analysisRow(ids.get("DONE")).get("error_code")).isNull();
        assertThat(analysisRow(ids.get("FAILED")).get("error_code")).isEqualTo("ANALYSIS_415");
        assertThat(analysisRow(ids.get("FAILED")).get("error_message")).isEqualTo("old");
        assertThat(analysisRow(deleted).get("status")).isEqualTo("SELECTING");
        assertThat(leftover).doesNotExist();
        assertThat(stray).doesNotExist();
    }

    /** F-07 AC11: 생성 중(PENDING)이던 실행 흐름도 FAILED·ANALYSIS_500으로 정리한다. */
    @Test
    void AC11_PENDING_실행_흐름을_FAILED_ANALYSIS_500으로_바꾼다() {
        long userId = createUser("ac11flow", "JUNIOR");
        long analysisId = insertAnalysis(userId, "DONE", null, false, "flows");
        jdbc.update(
                "INSERT INTO analysis_flows (analysis_id, endpoint, status, created_at) VALUES"
                        + " (?, 'POST /a', 'PENDING', NOW()), (?, 'GET /a', 'DONE', NOW())",
                analysisId,
                analysisId);

        recovery.recover();

        List<Map<String, Object>> flows =
                jdbc.queryForList(
                        "SELECT endpoint, status, error_code FROM analysis_flows WHERE analysis_id"
                                + " = ? ORDER BY id",
                        analysisId);
        assertThat(flows.get(0).get("status")).isEqualTo("FAILED");
        assertThat(flows.get(0).get("error_code")).isEqualTo("ANALYSIS_500");
        assertThat(flows.get(1).get("status")).isEqualTo("DONE");
        assertThat(flows.get(1).get("error_code")).isNull();
    }

    /** L-1a AC8: 재생성 중(GENERATING)이던 퀴즈를 모두 FAILED로 바꾼다. 다른 퀴즈 상태와 분석 상태는 그대로다. */
    @Test
    void L1a_AC8_GENERATING_퀴즈를_FAILED로_바꾼다() {
        long userId = createUser("l1aac8r", "JUNIOR");
        long generating = insertAnalysis(userId, "DONE", null, false, "generating");
        long deleted = insertAnalysis(userId, "DONE", null, true, "deleted");
        long ready = insertAnalysis(userId, "DONE", null, false, "ready");
        jdbc.update(
                "UPDATE analyses SET quiz_status = 'GENERATING' WHERE id IN (?, ?)",
                generating,
                deleted);
        jdbc.update("UPDATE analyses SET quiz_status = 'READY' WHERE id = ?", ready);

        recovery.recover();

        assertThat(analysisRow(generating).get("quiz_status")).isEqualTo("FAILED");
        assertThat(analysisRow(generating).get("status")).isEqualTo("DONE");
        assertThat(analysisRow(deleted).get("quiz_status")).isEqualTo("FAILED");
        assertThat(analysisRow(ready).get("quiz_status")).isEqualTo("READY");
    }

    /**
     * ApplicationReadyEvent는 웹 서버가 요청을 받기 시작한 뒤에 오므로, 그 사이에 들어온 새 분석까지 "재시작"으로 FAILED 처리하고 임시 ZIP을
     * 지울 수 있다. 모든 싱글톤이 만들어진 직후(웹 서버 시작 전)에 실행되어야 한다.
     */
    @Test
    void AC11_웹_서버가_요청을_받기_전에_복구한다() {
        assertThat(recovery).isInstanceOf(SmartInitializingSingleton.class);
        assertThat(AnalysisRecovery.class.getMethods())
                .noneMatch(m -> m.isAnnotationPresent(EventListener.class));
    }
}
