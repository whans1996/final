package com.vibenote.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.fetch.FakeRepositoryServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

    /** 선별 파일 (F-26a 뒤로 analysis_files에는 인덱스 전체가 있고, 설명 대상은 selected = TRUE인 행이다). */
    private List<Map<String, Object>> files(long analysisId) {
        return jdbc.queryForList(
                "SELECT path, layer, content FROM analysis_files"
                        + " WHERE analysis_id = ? AND selected = TRUE ORDER BY id",
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
    void AC5_단계를_FETCHING_FILTERING_SELECTING_EXPLAINING_EXTRACTING_QUIZ_순서로_거친다()
            throws IOException {
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
        // F-14가 EXTRACTING 뒤에 QUIZ를 끼웠다 (F-14 AC1)
        order.verify(analysisService).advance(id, AnalysisStatus.QUIZ, 85);
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
    void F26a_AC2_지원_파일이_201개여도_413이_아니고_DONE이다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 201; i++) {
            entries.put("p/F" + i + ".java", "class F" + i + " {}");
        }
        long id = prepare(createUser("ac7many", "JUNIOR"), zip(entries));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
    }

    @Test
    void AC7_지원_파일이_200개면_DONE이고_30개만_저장한다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 198; i++) {
            entries.put(String.format("p/u/Util%03d.java", i), "class U {}");
        }
        entries.put("p/web/HomeController.java", "class HomeController {}");
        // 1MB 초과 파일은 읽지 않고 건너뛴다
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
    void F26a_AC2_읽은_합계가_10MB를_넘어도_413이_아니고_DONE이다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 11; i++) {
            entries.put("p/F" + i + ".java", "class F" + i + " {}\n//" + "x".repeat(1_000_000));
        }
        long id = prepare(createUser("ac7size", "JUNIOR"), zip(entries));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
    }

    /** 코드 480 + controller 1 + test 10 + doc 9 = 인덱스 500개, 1MB 초과 1개는 버린다. */
    private static Map<String, String> indexOf500() {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 480; i++) {
            entries.put(String.format("p/u/Util%03d.java", i), "class Util" + i + " {}");
        }
        // 선별되지 않을 파일에 키를 넣어 인덱스 전체가 가려지는지 본다
        entries.put("p/u/Util479.java", "class Util479 { String k = \"" + GOOGLE_KEY + "\"; }");
        entries.put("p/web/HomeController.java", "class HomeController {}");
        for (int i = 0; i < 10; i++) {
            entries.put("p/src/test/java/T" + i + "Test.java", "class T" + i + "Test {}");
        }
        for (int i = 0; i < 9; i++) {
            entries.put("p/docs/d" + i + ".md", "# doc " + i);
        }
        entries.put("p/Huge.java", "x".repeat(1024 * 1024 + 1));
        return entries;
    }

    @Test
    void F26a_AC8_인덱스_전체를_가려_저장하고_30개만_selected다() throws IOException {
        long id = prepare(createUser("f26ac8", "JUNIOR"), zip(indexOf500()));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("indexed_files")).isEqualTo(500);
        assertThat(row.get("dropped_files")).isEqualTo(1);

        List<Map<String, Object>> all =
                jdbc.queryForList(
                        "SELECT id, path, layer, selected, importance, content, summary"
                                + " FROM analysis_files WHERE analysis_id = ? ORDER BY id",
                        id);
        assertThat(all).hasSize(500);
        assertThat(all).filteredOn(f -> Boolean.TRUE.equals(f.get("selected"))).hasSize(30);
        assertThat(all)
                .extracting(f -> (String) f.get("content"))
                .noneMatch(c -> c.contains(GOOGLE_KEY));
        assertThat(all)
                .filteredOn(f -> f.get("path").equals("u/Util479.java"))
                .singleElement()
                .satisfies(f -> assertThat(f.get("selected")).isEqualTo(false));
        // 선별 파일이 먼저 저장된다 (5.4절 7): 앞 30행이 선별, 뒤 470행은 선별되지 않음
        assertThat(all.subList(0, 30)).allMatch(f -> Boolean.TRUE.equals(f.get("selected")));
        assertThat(all.subList(30, 500)).allMatch(f -> Boolean.FALSE.equals(f.get("selected")));
        // test·doc은 점수가 없고, 후보는 점수가 있다
        assertThat(all)
                .filteredOn(f -> List.of("test", "doc").contains(f.get("layer")))
                .hasSize(19)
                .allMatch(f -> f.get("importance") == null);
        assertThat(all)
                .filteredOn(f -> !List.of("test", "doc").contains(f.get("layer")))
                .allMatch(f -> f.get("importance") != null);
    }

    @Test
    void F26a_AC9_개요용_인덱스_조회는_루트_README만_내용을_읽는다() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("p/Readme.md", "# 소개");
        entries.put("p/web/HomeController.java", "class HomeController {}");
        entries.put("p/docs/README.md", "# 하위 문서");
        long id = prepare(createUser("f26ac9", "JUNIOR"), zip(entries));
        pipeline.run(id);

        List<AnalysisFile> index = analysisService.findIndexFiles(id);

        assertThat(index)
                .extracting(AnalysisFile::getPath, AnalysisFile::getContent)
                .containsExactlyInAnyOrder(
                        tuple("Readme.md", "# 소개"),
                        tuple("web/HomeController.java", null),
                        tuple("docs/README.md", null));
    }

    @Test
    void F26a_AC8_설명과_개념_연결은_선별_파일만_본다() throws IOException {
        long id = prepare(createUser("f26ac8b", "JUNIOR"), zip(indexOf500()));

        pipeline.run(id);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM analysis_files WHERE analysis_id = ? AND"
                                        + " selected = FALSE AND (summary IS NOT NULL OR role IS"
                                        + " NOT NULL)",
                                Integer.class,
                                id))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM analysis_concepts ac JOIN analysis_files f"
                                        + " ON f.id = ac.file_id WHERE ac.analysis_id = ? AND"
                                        + " f.selected = FALSE",
                                Integer.class,
                                id))
                .isZero();
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
                .saveFiles(anyLong(), anyList(), anyInt());

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
        doThrow(new StackOverflowError())
                .when(analysisService)
                .saveFiles(anyLong(), anyList(), anyInt());

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

    // ═════════════ F-05b 저장소 URL 수집 (docs/specs/F-05b-analysis-request-url.md AC4·AC6·AC11)
    // ═════════════

    /** 가짜 GitHub·GitLab 서버. 테스트마다 다른 저장소 경로를 쓴다. */
    static final FakeRepositoryServer REPOSITORY = new FakeRepositoryServer();

    @DynamicPropertySource
    static void repositoryServer(DynamicPropertyRegistry registry) {
        registry.add("analysis.fetch.github-api-base", REPOSITORY::base);
        registry.add("analysis.fetch.gitlab-api-base", () -> REPOSITORY.base() + "/api/v4");
        registry.add("analysis.fetch.allowed-hosts", () -> "localhost");
    }

    @AfterAll
    static void closeRepositoryServer() {
        REPOSITORY.close();
    }

    /** springProject()의 최상위 폴더를 GitHub zipball 형식({owner}-{repo}-{sha})으로 바꾼다. */
    private static byte[] githubArchive(Map<String, String> entries) {
        Map<String, String> renamed = new LinkedHashMap<>();
        entries.forEach(
                (path, content) ->
                        renamed.put(
                                path.replaceFirst("^todo-app/", "octo-todo-app-1a2b3c4/"),
                                content));
        return zip(renamed);
    }

    @Test
    void F05b_AC4_GitHub_저장소를_받아_선별하고_DONE이_되며_임시_파일을_지운다() {
        long userId = createUser("b4github", "JUNIOR");
        long id = insertUrlAnalysis(userId, "https://github.com/octo/b4-app");
        REPOSITORY
                .on(
                        "/repos/octo/b4-app/zipball",
                        FakeRepositoryServer.redirect(
                                302, REPOSITORY.base() + "/codeload/octo/b4-app/zip/main"))
                .on(
                        "/codeload/octo/b4-app/zip/main",
                        FakeRepositoryServer.zip(githubArchive(springProject())));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
        assertThat(files(id))
                .extracting(f -> (String) f.get("path"))
                .contains("src/main/java/com/example/todo/controller/UserController.java")
                .allSatisfy(path -> assertThat(path).doesNotStartWith("octo-todo-app"));
        assertThat(tempZip(id)).doesNotExist();
    }

    @Test
    void F05b_AC5_GitLab_저장소를_받아_DONE이_된다() {
        long userId = createUser("b5gitlab", "JUNIOR");
        long id = insertUrlAnalysis(userId, "https://gitlab.com/group/sub/b5-app");
        REPOSITORY.on(
                "/api/v4/projects/group%2Fsub%2Fb5-app/repository/archive.zip",
                FakeRepositoryServer.zip(zip(springProject())));

        pipeline.run(id);

        assertThat(analysisRow(id).get("status")).isEqualTo("DONE");
        assertThat(tempZip(id)).doesNotExist();
    }

    @Test
    void F05b_AC6_저장소가_없으면_FAILED_ANALYSIS_004이고_한도를_깎지_않는다() {
        long userId = createUser("b6missing", "JUNIOR");
        long id = insertUrlAnalysis(userId, "https://github.com/octo/b6-missing");
        REPOSITORY.on("/repos/octo/b6-missing/zipball", FakeRepositoryServer.status(404));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_004");
        assertThat(row.get("error_message"))
                .isEqualTo("저장소를 찾을 수 없습니다. 공개 저장소인지, 주소가 맞는지, 기본 브랜치에 코드가 있는지 확인해 주세요.");
        assertThat(tempZip(id)).doesNotExist();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM analyses WHERE user_id = ? AND status <>"
                                        + " 'FAILED'",
                                Long.class,
                                userId))
                .isZero();
    }

    @Test
    void F05b_AC11_받은_아카이브에_지원_코드가_없으면_ANALYSIS_415() {
        long userId = createUser("b11none", "JUNIOR");
        long id = insertUrlAnalysis(userId, "https://github.com/octo/b11-docs");
        REPOSITORY.on(
                "/repos/octo/b11-docs/zipball",
                FakeRepositoryServer.zip(
                        zip(
                                Map.of(
                                        "octo-b11-docs-1a2b3c4/README.md",
                                        "# docs",
                                        "octo-b11-docs-1a2b3c4/logo.png",
                                        "png"))));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("FAILED");
        assertThat(row.get("error_code")).isEqualTo("ANALYSIS_415");
        assertThat(tempZip(id)).doesNotExist();
    }

    @Test
    void F26a_AC2_받은_아카이브의_지원_파일이_200개를_넘어도_DONE이다() {
        long userId = createUser("b11many", "JUNIOR");
        long id = insertUrlAnalysis(userId, "https://github.com/octo/b11-many");
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 201; i++) {
            entries.put("octo-b11-many-1a2b3c4/src/A" + i + ".java", "class A" + i + " {}");
        }
        REPOSITORY.on("/repos/octo/b11-many/zipball", FakeRepositoryServer.zip(zip(entries)));

        pipeline.run(id);

        Map<String, Object> row = analysisRow(id);
        assertThat(row.get("status")).isEqualTo("DONE");
        assertThat(row.get("error_code")).isNull();
        assertThat(tempZip(id)).doesNotExist();
    }
}
