package com.vibenote.analysis.concept;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.concept.ExtractedConcept.NewConcept;
import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.Level;
import com.vibenote.user.UserService;
import com.vibenote.user.dto.SignupRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** F-08 AC5 저장 (DRAFT·AI INSERT, 같은 이름이 있으면 그 카드에 연결). 시드 카드에 기대지 않는다. */
class ConceptLinkServiceTest extends IntegrationTestBase {

    @Autowired ConceptLinkService service;
    @Autowired UserService userService;
    @Autowired JdbcTemplate jdbc;

    private long analysisId;
    private long fileId;

    @BeforeEach
    void setUp() {
        long userId =
                userService.signup(
                        new SignupRequest("link@f08.test", "vibe1234", "f08link", "JUNIOR"));
        jdbc.update(
                "INSERT INTO analyses (user_id, source_type, project_name, title, level, status)"
                        + " VALUES (?, 'ZIP', 'p', 'p', 'JUNIOR', 'EXTRACTING')",
                userId);
        analysisId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO analysis_files (analysis_id, path, layer, content) VALUES (?,"
                        + " 'A.java', 'service', 'class A {}')",
                analysisId);
        fileId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f08.test'");
        jdbc.update("DELETE FROM concepts WHERE name LIKE 'f08-test-%'");
    }

    private long insertConcept(String name, String status) {
        jdbc.update(
                "INSERT INTO concepts (name, category, difficulty, summary, status, source)"
                        + " VALUES (?, 'java', 'JUNIOR', '요약', ?, 'SEED')",
                name,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static ExtractedConcept draft(String name, Long fileId, String hint, String note) {
        return new ExtractedConcept(
                null,
                new NewConcept(name, "spring", Level.SENIOR, "요약입니다", "## 핵심\n본문"),
                fileId,
                hint,
                note);
    }

    private List<Map<String, Object>> links() {
        return jdbc.queryForList(
                "SELECT concept_id, file_id, line_hint, context_note FROM analysis_concepts WHERE"
                        + " analysis_id = ? ORDER BY concept_id",
                analysisId);
    }

    @Test
    void AC5_새_개념은_DRAFT_AI_카드로_만들고_연결한다() {
        long existing = insertConcept("f08-test-기존", "PUBLISHED");

        service.save(
                analysisId,
                List.of(
                        new ExtractedConcept(existing, null, null, null, "기존 카드"),
                        draft("f08-test-새 개념", fileId, "L1-L3", "맥락")));

        Map<String, Object> created =
                jdbc.queryForMap("SELECT * FROM concepts WHERE name = 'f08-test-새 개념'");
        assertThat(created)
                .containsEntry("category", "spring")
                .containsEntry("difficulty", "SENIOR")
                .containsEntry("summary", "요약입니다")
                .containsEntry("body", "## 핵심\n본문")
                .containsEntry("status", "DRAFT")
                .containsEntry("source", "AI")
                .containsEntry("example_code", null)
                .containsEntry("reference_url", null);
        long createdId = ((Number) created.get("id")).longValue();
        assertThat(links())
                .extracting(
                        r ->
                                r.get("concept_id")
                                        + " "
                                        + r.get("file_id")
                                        + " "
                                        + r.get("line_hint")
                                        + " "
                                        + r.get("context_note"))
                .containsExactly(
                        existing + " null null 기존 카드", createdId + " " + fileId + " L1-L3 맥락");
    }

    @Test
    void AC5_같은_이름의_카드가_있으면_HIDDEN이라도_만들지_않고_그_카드에_연결한다() {
        long hidden = insertConcept("f08-test-숨김", "HIDDEN");

        service.save(analysisId, List.of(draft("f08-test-숨김", null, null, null)));

        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM concepts WHERE name = 'f08-test-숨김'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM concepts WHERE id = ?", String.class, hidden))
                .isEqualTo("HIDDEN");
        assertThat(links()).extracting(r -> r.get("concept_id")).containsExactly(hidden);
    }

    @Test
    void AC5_UNIQUE_충돌로_같은_카드를_두_번_가리키면_첫_항목만_연결한다() {
        long existing = insertConcept("f08-test-dup", "PUBLISHED");

        service.save(
                analysisId,
                List.of(
                        new ExtractedConcept(existing, null, null, null, "첫"),
                        draft("F08-TEST-DUP", null, null, "둘")));

        assertThat(links()).extracting(r -> r.get("context_note")).containsExactly("첫");
    }

    /** 동시 분석끼리 잠금 순서를 맞춰 데드락을 피하도록 초안은 이름 순으로 INSERT한다. */
    @Test
    void AC5_새_초안은_응답_순서와_관계없이_이름_순으로_만든다() {
        service.save(
                analysisId,
                List.of(
                        draft("f08-test-다", null, null, "1"),
                        draft("f08-test-가", null, null, "2"),
                        draft("f08-test-나", null, null, "3")));

        assertThat(
                        jdbc.queryForList(
                                "SELECT name FROM concepts WHERE name LIKE 'f08-test-%' ORDER BY"
                                        + " id",
                                String.class))
                .containsExactly("f08-test-가", "f08-test-나", "f08-test-다");
        assertThat(links()).hasSize(3);
    }

    /** 카탈로그 스냅샷 뒤 관리자가 초안을 지워도(F-13) FK 위반 없이 남은 카드만 연결한다. */
    @Test
    void issue77_추출_뒤_지워진_카드는_건너뛰고_남은_카드만_연결한다() {
        long kept = insertConcept("f08-test-남음", "PUBLISHED");
        long deleted = insertConcept("f08-test-지워짐", "DRAFT");
        jdbc.update("DELETE FROM concepts WHERE id = ?", deleted);

        service.save(
                analysisId,
                List.of(
                        new ExtractedConcept(deleted, null, fileId, "L1", "지워진 초안"),
                        new ExtractedConcept(kept, null, fileId, "L2", "남은 카드"),
                        draft("f08-test-새 초안", null, null, "새")));

        long created =
                jdbc.queryForObject(
                        "SELECT id FROM concepts WHERE name = 'f08-test-새 초안'", Long.class);
        assertThat(links())
                .extracting(r -> ((Number) r.get("concept_id")).longValue())
                .containsExactlyInAnyOrder(kept, created);
    }

    @Test
    void issue77_연결할_카드가_모두_지워졌으면_아무것도_넣지_않는다() {
        long deleted = insertConcept("f08-test-지워짐", "DRAFT");
        jdbc.update("DELETE FROM concepts WHERE id = ?", deleted);

        service.save(analysisId, List.of(new ExtractedConcept(deleted, null, null, null, "x")));

        assertThat(links()).isEmpty();
    }

    @Test
    void AC5_항목이_없으면_아무것도_넣지_않는다() {
        service.save(analysisId, List.of());

        assertThat(links()).isEmpty();
    }
}
