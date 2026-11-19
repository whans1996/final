package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * F-05a 파이프라인 (docs/specs/F-05a-analysis-request-zip.md AC5~AC10). 행과 임시 ZIP을 직접 만들고 {@link
 * AnalysisPipeline#run}을 동기로 불러 DB를 확인한다.
 */
class AnalysisPipelineTest extends AnalysisTestSupport {

    private static final String GOOGLE_KEY = "AIza" + "k".repeat(35);

    @Autowired AnalysisPipeline pipeline;

    /** 단계 순서 확인과 예기치 못한 예외 주입용. */
    @MockitoSpyBean AnalysisService analysisService;

    /** DB 세션 시간대와 상관없이 상태 시각이 서비스 시계로 기록되는지 보려고 고정한다. */
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2030, 1, 2, 3, 4, 5);

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);
    }

    private long prepare(long userId, byte[] zipBytes) throws IOException {
        long id = insertAnalysis(userId, "FETCHING", null, false, "todo-app");
        Path tmp = tempZip(id);
        Files.createDirectories(tmp.getParent());
        Files.write(tmp, zipBytes);
        return id;
    }

    private List<Map<String, Object>> files(long analysisId) {
        return jdbc.queryForList(
                "SELECT path, layer, content FROM analysis_files WHERE analysis_id = ? ORDER BY id",
                analysisId);
    }

    private static Map<String, String> springProject() {
        Map<String, String> entries = new LinkedHashMap<>();
        String java = "todo-app/src/main/java/com/example/todo/";
        entries.put(java + "controller/UserController.java", "class UserController {}");
        entries.put(java + "service/UserService.java", "class UserService {}");
        entries.put(java + "mapper/UserMapper.java", "interface UserMapper {}");
        entries.put(java + "domain/User.java", "class User {}");
        entries.put("todo-app/src/main/resources/mapper/UserMapper.xml", "<mapper/>");
        entries.put(
                "todo-app/src/main/resources/application.yml",
                "spring:\n  datasource:\n    password: ${DB_PASSWORD}\n");
        entries.put("todo-app/frontend/src/views/TodoView.vue", "<template><div/></template>");
        entries.put("todo-app/frontend/node_modules/vue/index.js", "module.exports = {}");
        entries.put("todo-app/build/classes/Stale.java", "class Stale {}");
        entries.put("todo-app/.env", "DB_PASSWORD=hunter2");
        entries.put("todo-app/id_rsa", "secret");
        entries.put("todo-app/logo.png", "png");
        return entries;
    }

    @Test
    void AC5_Spring_프로젝트를_선별해_저장하고_DONE이_된다() throws IOException {
        long userId = createUser("ac5", "JUNIOR");
        long id = prepare(userId, zip(springProject()));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("progress")).isEqualTo(100);
        assertThat(row.get("error_code")).isNull();
        assertThat(row.get("error_message")).isNull();

        assertThat(files(id))
                .extracting(f -> f.get("path") + " " + f.get("layer"))
                .containsExactly(
                        "src/main/java/com/example/todo/controller/UserController.java controller",
                        "src/main/java/com/example/todo/service/UserService.java service",
                        "src/main/java/com/example/todo/mapper/UserMapper.java mapper",
                        "src/main/resources/mapper/UserMapper.xml mapper",
                        "frontend/src/views/TodoView.vue vue",
                        "src/main/resources/application.yml config",
                        "src/main/java/com/example/todo/domain/User.java domain");

        assertThat(
                        jdbc.queryForList(
                                "SELECT type FROM activities WHERE user_id = ? AND target_id = ?",
                                String.class,
                                userId,
                                id))
                .containsExactly("ANALYSIS");
    }

    @Test
    void AC5_단계를_FETCHING_FILTERING_SELECTING_EXPLAINING_EXTRACTING_순서로_거친다() throws IOException {
        long id = prepare(createUser("ac5order", "JUNIOR"), zip(springProject()));

        pipeline.run(id);

        InOrder order = inOrder(analysisService);
        order.verify(analysisService).advance(id, AnalysisStatus.FETCHING, 5);
        order.verify(analysisService).advance(id, AnalysisStatus.FILTERING, 15);
        order.verify(analysisService).advance(id, AnalysisStatus.SELECTING, 25);
        // F-06이 SELECTING 뒤에 EXPLAINING을 끼웠다 (F-06 AC1)
        order.verify(analysisService).advance(id, AnalysisStatus.EXPLAINING, 40);
        // F-08이 EXPLAINING 뒤에 EXTRACTING을 끼웠다 (F-08 AC1)
        order.verify(analysisService).advance(id, AnalysisStatus.EXTRACTING, 70);
        order.verify(analysisService).complete(eq(id));
    }

    @Test
    void AC6_저장된_내용에서_키와_비밀_설정값이_가려진다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(
                "p/src/main/java/GeminiController.java",
                "class GeminiController {\n  String key = \"" + GOOGLE_KEY + "\";\n}");
        entries.put(
                "p/src/main/resources/application.yml",
                "spring:\n  datasource:\n    password: hunter2\n    username: ${DB_USERNAME}\n"
                        + "db:\n  password: ${DB_PASSWORD}\napp:\n  api-key: xyz\n");
        entries.put(
                "p/src/main/resources/application.properties", "jwt.secret=abc\nserver.port=8080");
        long id = prepare(createUser("ac6", "JUNIOR"), zip(entries));

        pipeline.run(id);

        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
        Map<String, String> saved = new LinkedHashMap<>();
        files(id).forEach(f -> saved.put((String) f.get("path"), (String) f.get("content")));
        assertThat(saved.get("src/main/java/GeminiController.java"))
                .isEqualTo("class GeminiController {\n  String key = \"[REDACTED]\";\n}");
        assertThat(saved.get("src/main/resources/application.yml"))
                .isEqualTo(
                        "spring:\n  datasource:\n    password: [REDACTED]\n"
                                + "    username: ${DB_USERNAME}\n"
                                + "db:\n  password: ${DB_PASSWORD}\napp:\n  api-key: [REDACTED]\n");
        assertThat(saved.get("src/main/resources/application.properties"))
                .isEqualTo("jwt.secret=[REDACTED]\nserver.port=8080");
        assertThat(String.join("\n", saved.values()))
                .doesNotContain(GOOGLE_KEY, "hunter2", "xyz", "=abc");
    }

    @Test
    void AC7_지원_파일이_201개면_FAILED_ANALYSIS_413() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 201; i++) {
            entries.put("p/F" + i + ".java", "class F" + i + " {}");
        }
        long id = prepare(createUser("ac7many", "JUNIOR"), zip(entries));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_413");
        assertThat(row.get("error_message"))
                .isEqualTo("지원하는 코드 파일이 201개입니다. 200개 이하로 줄여서 다시 올려 주세요.");
        assertThat(row.get("progress")).isEqualTo(15);
        assertThat(files(id)).isEmpty();
    }

    @Test
    void AC7_지원_파일이_200개면_DONE이고_30개만_저장한다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 198; i++) {
            entries.put(String.format("p/u/Util%03d.java", i), "class U {}");
        }
        entries.put("p/web/HomeController.java", "class HomeController {}");
        // 1MB 초과 파일도 지원 파일 수(200)에는 들어가고 읽지 않고 건너뛴다
        entries.put("p/Huge.java", "x".repeat(1024 * 1024 + 1));
        long id = prepare(createUser("ac7select", "JUNIOR"), zip(entries));

        pipeline.run(id);

        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
        List<Map<String, Object>> saved = files(id);
        assertThat(saved).hasSize(30);
        assertThat(saved.get(0).get("path")).isEqualTo("web/HomeController.java");
        assertThat(saved).extracting(f -> f.get("path")).doesNotContain("Huge.java");
    }

    @Test
    void AC7_읽은_합계가_10MB를_넘으면_FAILED_ANALYSIS_413() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 11; i++) {
            entries.put("p/F" + i + ".java", "x".repeat(1_000_000));
        }
        long id = prepare(createUser("ac7size", "JUNIOR"), zip(entries));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_413");
        assertThat(row.get("error_message")).isEqualTo("코드 파일 합계가 10MB를 넘습니다. 크기를 줄여서 다시 올려 주세요.");
    }

    @Test
    void AC8_지원_스택_코드가_없으면_FAILED_ANALYSIS_415() throws IOException {
        long id =
                prepare(
                        createUser("ac8", "JUNIOR"),
                        zip(Map.of("p/app.py", "print(1)", "p/application.yml", "a: b")));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_415");
        assertThat(row.get("error_message"))
                .isEqualTo("분석할 수 있는 코드가 없습니다. Java·Spring·MyBatis·JavaScript·Vue 프로젝트만 지원합니다.");
        assertThat(files(id)).isEmpty();
    }

    @Test
    void AC9_루트_밖_경로가_있으면_FAILED_ANALYSIS_400이고_밖에_파일이_생기지_않는다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("p/A.java", "class A {}");
        entries.put("../../evil.java", "class Evil {}");
        long id = prepare(createUser("ac9slip", "JUNIOR"), zip(entries));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_400");
        assertThat(row.get("error_message"))
                .isEqualTo("ZIP 파일을 읽을 수 없습니다. 손상되었거나 허용되지 않는 경로가 들어 있습니다.");
        Path tmpDir = tempZip(id).getParent();
        assertThat(tmpDir.resolve("../../evil.java").normalize()).doesNotExist();
        assertThat(tmpDir.resolve("../evil.java").normalize()).doesNotExist();
        assertThat(Path.of("evil.java")).doesNotExist();
        assertThat(files(id)).isEmpty();
    }

    @Test
    void AC9_손상된_ZIP이면_FAILED_ANALYSIS_400() throws IOException {
        long id =
                prepare(
                        createUser("ac9broken", "JUNIOR"),
                        new byte[] {'P', 'K', 3, 4, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9});

        pipeline.run(id);

        assertThat(analysisRow(id).get("error_code")).isEqualTo("ANALYSIS_400");
    }

    @Test
    void AC10_DONE이든_FAILED든_임시_ZIP을_지운다() throws IOException {
        long done = prepare(createUser("ac10done", "JUNIOR"), zip(springProject()));
        long failed =
                prepare(createUser("ac10failed", "JUNIOR"), zip(Map.of("p/app.py", "print(1)")));

        pipeline.run(done);
        pipeline.run(failed);

        assertThat(analysisRow(done).get("status")).isEqualTo("DONE");
        assertThat(analysisRow(failed).get("status")).isEqualTo("FAILED");
        assertThat(tempZip(done)).doesNotExist();
        assertThat(tempZip(failed)).doesNotExist();
    }

    @Test
    void AC10_예기치_못한_예외는_FAILED_ANALYSIS_500이고_progress를_유지한다() throws IOException {
        long id = prepare(createUser("ac10error", "JUNIOR"), zip(springProject()));
        doThrow(new IllegalStateException("db down"))
                .when(analysisService)
                .saveFiles(anyLong(), anyList());

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_500");
        assertThat(row.get("error_message")).isEqualTo("분석 중 오류가 발생했습니다. 다시 요청해 주세요.");
        assertThat(row.get("progress")).isEqualTo(25);
        assertThat(tempZip(id)).doesNotExist();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM activities WHERE target_id = ?",
                                Long.class,
                                id))
                .isZero();
    }

    @Test
    void AC10_Error가_나도_FAILED_ANALYSIS_500으로_바꾸고_다시_던진다() throws IOException {
        long id = prepare(createUser("ac10fatal", "JUNIOR"), zip(springProject()));
        doThrow(new StackOverflowError()).when(analysisService).saveFiles(anyLong(), anyList());

        assertThatThrownBy(() -> pipeline.run(id)).isInstanceOf(StackOverflowError.class);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_500");
        assertThat(tempZip(id)).doesNotExist();
    }

    @Test
    void AC13_상태_변경_시각은_서비스_시계로_기록한다() throws IOException {
        long userId = createUser("ac13clock", "JUNIOR");
        long done = prepare(userId, zip(springProject()));
        long failed = prepare(userId, zip(Map.of("p/app.py", "print(1)")));
        long interrupted = insertAnalysis(userId, "FILTERING", null, false, "interrupted");

        pipeline.run(done);
        pipeline.run(failed);
        analysisService.failInterrupted("restart");

        for (long id : List.of(done, failed, interrupted)) {
            assertThat(analysisRow(id).get("updated_at"))
                    .as("analysis %d", id)
                    .isEqualTo(FIXED_NOW);
        }
    }
}
