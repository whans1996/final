package com.vibenote.analysis;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** F-08 분석의 개념 조회 API (docs/specs/F-08-concept-extraction.md AC7). 카드·연결을 직접 넣고 조회한다. */
class AnalysisConceptApiTest extends AnalysisTestSupport {

    @AfterEach
    void cleanUpConcepts() {
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f08-test-%'");
    }

    private long insertConcept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source,"
                        + " wish_count, avg_rating) VALUES (?, 'spring', 'BEGINNER', '요약', ?,"
                        + " 'SEED', 3, 4.5)",
                name,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertFile(long analysisId, String path) {
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?, ?,"
                        + " 'service', 'class X {}')",
                analysisId,
                path);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void link(long analysisId, long conceptId, Long fileId, String hint, String note) {
        jdbc.update(
                "INSERT INTO analysis_concepts (analysis_id, concept_id, file_id, line_hint,"
                        + " context_note) VALUES (?, ?, ?, ?, ?)",
                analysisId,
                conceptId,
                fileId,
                hint,
                note);
    }

    @Test
    void AC7_PUBLISHED_카드만_파일_있는_것_먼저_정렬해_주고_DRAFT_수를_센다() throws Exception {
        long userId = createUser("f08ac7", "JUNIOR");
        long id = insertAnalysis(userId, "DONE", null, false, "todo-app");
        long fileB = insertFile(id, "svc/B.java");
        long fileA = insertFile(id, "web/A.java");
        long noFile = insertConcept("f08-test-파일 없음", "PUBLISHED");
        long onB = insertConcept("f08-test-B", "PUBLISHED");
        long onA = insertConcept("f08-test-A", "PUBLISHED");
        long draft1 = insertConcept("f08-test-초안1", "DRAFT");
        long draft2 = insertConcept("f08-test-초안2", "DRAFT");
        long hidden = insertConcept("f08-test-숨김", "HIDDEN");
        link(id, noFile, null, null, "설정에서 씁니다.");
        link(id, onA, fileA, null, null);
        link(id, onB, fileB, "L8-L15", "생성자로 받습니다.");
        link(id, draft1, fileA, "L1", "초안");
        link(id, draft2, null, null, null);
        link(id, hidden, fileA, null, null);

        mockMvc.perform(
                        get("/api/analyses/{id}/concepts", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].concept.id").value(onB))
                .andExpect(jsonPath("$.items[0].concept.name").value("f08-test-B"))
                .andExpect(jsonPath("$.items[0].concept.category").value("spring"))
                .andExpect(jsonPath("$.items[0].concept.difficulty").value("BEGINNER"))
                .andExpect(jsonPath("$.items[0].concept.summary").value("요약"))
                .andExpect(jsonPath("$.items[0].concept.avgRating").value(4.5))
                .andExpect(jsonPath("$.items[0].concept.wishCount").value(3))
                .andExpect(jsonPath("$.items[0].fileId").value(fileB))
                .andExpect(jsonPath("$.items[0].filePath").value("svc/B.java"))
                .andExpect(jsonPath("$.items[0].lineHint").value("L8-L15"))
                .andExpect(jsonPath("$.items[0].contextNote").value("생성자로 받습니다."))
                .andExpect(jsonPath("$.items[1].concept.id").value(onA))
                .andExpect(jsonPath("$.items[1].lineHint").value(nullValue()))
                .andExpect(jsonPath("$.items[1].contextNote").value(nullValue()))
                .andExpect(jsonPath("$.items[2].concept.id").value(noFile))
                .andExpect(jsonPath("$.items[2].fileId").value(nullValue()))
                .andExpect(jsonPath("$.items[2].filePath").value(nullValue()))
                .andExpect(jsonPath("$.items[2].contextNote").value("설정에서 씁니다."))
                .andExpect(jsonPath("$.pendingCount").value(2));
    }

    @Test
    void AC7_추출_전이거나_연결이_없으면_빈_목록과_0() throws Exception {
        long userId = createUser("f08ac7e", "JUNIOR");
        long id = insertAnalysis(userId, "EXPLAINING", null, false, "todo-app");

        mockMvc.perform(
                        get("/api/analyses/{id}/concepts", id)
                                .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.pendingCount").value(0));
    }

    @Test
    void AC7_토큰이_없으면_401() throws Exception {
        long userId = createUser("f08ac7a", "JUNIOR");
        long id = insertAnalysis(userId, "DONE", null, false, "todo-app");

        mockMvc.perform(get("/api/analyses/{id}/concepts", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void AC7_없는_분석_남의_분석_삭제한_분석은_404() throws Exception {
        long me = createUser("f08ac7me", "JUNIOR");
        long other = createUser("f08ac7other", "JUNIOR");
        long othersAnalysis = insertAnalysis(other, "DONE", null, false, "other");
        long deleted = insertAnalysis(me, "DONE", null, true, "deleted");

        for (long id : new long[] {othersAnalysis, deleted, 999_999_999L}) {
            mockMvc.perform(
                            get("/api/analyses/{id}/concepts", id)
                                    .header(HttpHeaders.AUTHORIZATION, bearer(me)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ANALYSIS_404"));
        }
    }
}
