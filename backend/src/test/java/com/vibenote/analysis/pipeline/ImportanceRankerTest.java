package com.vibenote.analysis.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** 중요도 선별 (F-26a 5.4절 AC5·AC6·AC7·AC11). 입력은 테스트 코드로 만든 합성 파일 목록이다 (eval 데이터 금지). */
@ExtendWith(OutputCaptureExtension.class)
class ImportanceRankerTest {

    private final ImportanceRanker ranker =
            new ImportanceRanker(new SelectionProperties(3, 50_000_000, Duration.ofSeconds(10)));

    private static SourceFile file(String path, String content) {
        return new SourceFile(path, content);
    }

    private static List<String> selectedPaths(List<RankedFile> ranked) {
        return ranked.stream().filter(RankedFile::selected).map(RankedFile::path).toList();
    }

    private static Map<String, RankedFile> byPath(List<RankedFile> ranked) {
        return ranked.stream().collect(Collectors.toMap(RankedFile::path, Function.identity()));
    }

    private static String controller(int n) {
        return """
        package a.web;

        import a.service.UsedService;

        public class C%dController {
            private final UsedService service;

            C%dController(UsedService service) {
                this.service = service;
            }

            String get() {
                return service.find();
            }
        }
        """
                .formatted(n, n);
    }

    /** controller 3개가 UsedService를 import·호출하고 IdleService는 아무도 쓰지 않는다. */
    private static List<SourceFile> usedAndIdleServices() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            files.add(file("src/main/java/a/web/C" + i + "Controller.java", controller(i)));
        }
        files.add(
                file(
                        "src/main/java/a/service/UsedService.java",
                        "package a.service;\n\npublic class UsedService {\n"
                                + "    public String find() {\n        return \"x\";\n    }\n}\n"));
        files.add(
                file(
                        "src/main/java/a/service/IdleService.java",
                        "package a.service;\n\npublic class IdleService {}\n"));
        return files;
    }

    @Test
    void AC5_쓰이는_service의_importance가_높고_먼저_선별된다() {
        List<RankedFile> ranked = ranker.rank(usedAndIdleServices(), 4);

        Map<String, RankedFile> rows = byPath(ranked);
        RankedFile used = rows.get("src/main/java/a/service/UsedService.java");
        RankedFile idle = rows.get("src/main/java/a/service/IdleService.java");
        assertThat(used.importance()).isGreaterThan(idle.importance());
        // 서로 다른 파일 3개에서 들어오는 연결: service 8 + 2×3
        assertThat(used.importance()).isEqualTo(14);
        assertThat(idle.importance()).isEqualTo(8);
        assertThat(used.selected()).isTrue();
        assertThat(idle.selected()).isFalse();
    }

    @Test
    void AC5_점수가_같으면_경로_오름차순이고_결과가_항상_같다() {
        List<SourceFile> files =
                List.of(
                        file("src/main/java/a/BService.java", "class BService {}"),
                        file("src/main/java/a/AService.java", "class AService {}"));

        List<RankedFile> first = ranker.rank(files, 1);
        List<RankedFile> second = ranker.rank(List.of(files.get(1), files.get(0)), 1);

        assertThat(selectedPaths(first)).containsExactly("src/main/java/a/AService.java");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void AC5_진입점은_보너스_5점을_받는다() {
        List<RankedFile> ranked =
                ranker.rank(
                        List.of(
                                file(
                                        "src/main/java/a/App.java",
                                        "@SpringBootApplication\npublic class App {}"),
                                file("src/main/java/a/Plain.java", "public class Plain {}"),
                                file("frontend/src/App.vue", "<template><div/></template>"),
                                file("frontend/src/Other.vue", "<template><div/></template>"),
                                file("frontend/src/main.js", "const a = 1;"),
                                file("frontend/src/util/main.js", "const a = 1;"),
                                file("frontend/src/router/index.js", "const a = 1;")),
                        30);

        Map<String, RankedFile> rows = byPath(ranked);
        assertThat(rows.get("src/main/java/a/App.java").importance()).isEqualTo(1 + 5);
        assertThat(rows.get("src/main/java/a/Plain.java").importance()).isEqualTo(1);
        assertThat(rows.get("frontend/src/App.vue").importance()).isEqualTo(6 + 5);
        assertThat(rows.get("frontend/src/Other.vue").importance()).isEqualTo(6);
        assertThat(rows.get("frontend/src/main.js").importance()).isEqualTo(4 + 5);
        assertThat(rows.get("frontend/src/util/main.js").importance()).isEqualTo(4);
        assertThat(rows.get("frontend/src/router/index.js").importance()).isEqualTo(4 + 5);
    }

    @Test
    void AC6_BE_200개_FE_40개면_FE가_최소_10개를_받는다() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            files.add(file("be/src/main/java/p/U%03dController.java".formatted(i), "class U {}"));
        }
        for (int i = 0; i < 40; i++) {
            files.add(file("fe/src/u%02d.js".formatted(i), "const a = 1;"));
        }

        List<String> selected = selectedPaths(ranker.rank(files, 30));

        assertThat(selected).hasSize(30);
        assertThat(selected).filteredOn(p -> p.startsWith("fe/")).hasSize(10);
    }

    @Test
    void AC6_FE_후보가_5개뿐이면_모두_고르고_남은_자리는_BE에_넘긴다() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            files.add(file("be/src/main/java/p/U%03dController.java".formatted(i), "class U {}"));
        }
        for (int i = 0; i < 5; i++) {
            files.add(file("fe/src/u%02d.js".formatted(i), "const a = 1;"));
        }

        List<String> selected = selectedPaths(ranker.rank(files, 30));

        assertThat(selected).hasSize(30);
        assertThat(selected).filteredOn(p -> p.startsWith("fe/")).hasSize(5);
        assertThat(selected).filteredOn(p -> p.startsWith("be/")).hasSize(25);
    }

    @Test
    void AC6_한쪽만_있는_저장소는_그쪽이_30자리를_다_쓴다() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            files.add(file("fe/src/u%02d.js".formatted(i), "const a = 1;"));
        }

        assertThat(selectedPaths(ranker.rank(files, 30))).hasSize(30);
    }

    @Test
    void AC7_test_migration_doc은_자리가_남아도_선별되지_않고_importance가_없다() {
        List<RankedFile> ranked =
                ranker.rank(
                        List.of(
                                file("src/main/java/a/A.java", "class A {}"),
                                file("src/test/java/a/ATest.java", "class ATest {}"),
                                file(
                                        "src/main/resources/db/migration/V1__a.sql",
                                        "create table a;"),
                                file("README.md", "# a")),
                        30);

        assertThat(selectedPaths(ranked)).containsExactly("src/main/java/a/A.java");
        assertThat(ranked).hasSize(4);
        assertThat(ranked)
                .filteredOn(f -> f.layer().indexOnly())
                .allSatisfy(f -> assertThat(f.importance()).isNull());
    }

    @Test
    void AC7_config_후보가_6개면_3개만_선별된다() {
        List<RankedFile> ranked =
                ranker.rank(
                        List.of(
                                file("build.gradle", "plugins {}"),
                                file("pom.xml", "<project/>"),
                                file("src/main/resources/application.yml", "a: b"),
                                file("src/main/java/a/WebConfig.java", "class WebConfig {}"),
                                file("frontend/package.json", "{}"),
                                file("frontend/vite.config.js", "export default {};"),
                                file("src/main/java/a/A.java", "class A {}"),
                                file("frontend/src/a.js", "const a = 1;")),
                        30);

        assertThat(ranked).filteredOn(f -> f.selected() && f.layer() == Layer.CONFIG).hasSize(3);
        assertThat(selectedPaths(ranked)).contains("src/main/java/a/A.java", "frontend/src/a.js");
    }

    /** #159: 점수 상위 config가 몫이 찬 BE에 있어도 FE 몫의 config가 선별된다. */
    @Test
    void AC7_config_상한은_FE_BE_몫_안에서_실제로_고를_때_센다() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            files.add(
                    file(
                            String.format("src/main/java/a/web/C%02dController.java", i),
                            "class C" + i + "Controller {}"));
        }
        for (int i = 0; i < 3; i++) {
            files.add(file("a/application" + i + ".yml", "a: b"));
        }
        for (int i = 0; i < 10; i++) {
            files.add(file("frontend/src/util" + i + ".js", "const a = 1;"));
        }
        for (int i = 0; i < 3; i++) {
            files.add(file("z/tool" + i + ".config.js", "export default {};"));
        }

        List<RankedFile> ranked = ranker.rank(files, 30);

        assertThat(selectedPaths(ranked)).hasSize(30);
        assertThat(ranked)
                .filteredOn(f -> f.selected() && f.layer() == Layer.CONFIG)
                .extracting(RankedFile::path)
                .containsExactlyInAnyOrder(
                        "z/tool0.config.js", "z/tool1.config.js", "z/tool2.config.js");
    }

    @Test
    void AC7_config_상한은_FE_BE를_합쳐_센다() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            files.add(file("a/application" + i + ".yml", "a: b"));
            files.add(file("z/tool" + i + ".config.js", "export default {};"));
        }
        files.add(file("src/main/java/a/A.java", "class A {}"));
        files.add(file("frontend/src/a.js", "const a = 1;"));

        List<RankedFile> ranked = ranker.rank(files, 30);

        assertThat(ranked).filteredOn(f -> f.selected() && f.layer() == Layer.CONFIG).hasSize(3);
        assertThat(selectedPaths(ranked)).hasSize(5);
    }

    @Test
    void AC5_저장_순서는_선별_파일을_layer_경로순으로_먼저_그다음_나머지를_경로순() {
        List<RankedFile> ranked =
                ranker.rank(
                        List.of(
                                file("z/README.md", "# z"),
                                file("src/main/java/a/B.java", "class B {}"),
                                file("src/main/java/a/AService.java", "class AService {}"),
                                file("src/main/java/a/HomeController.java", "class H {}"),
                                file("src/test/java/a/HTest.java", "class HTest {}")),
                        2);

        assertThat(ranked)
                .extracting(RankedFile::path)
                .containsExactly(
                        "src/main/java/a/HomeController.java",
                        "src/main/java/a/AService.java",
                        "src/main/java/a/B.java",
                        "src/test/java/a/HTest.java",
                        "z/README.md");
        assertThat(selectedPaths(ranked))
                .containsExactly(
                        "src/main/java/a/HomeController.java", "src/main/java/a/AService.java");
    }

    @Test
    void F05a_AC5_후보가_다_들어가면_저장_순서는_layer와_경로순이다() {
        List<RankedFile> ranked =
                ranker.rank(
                        List.of(
                                file("z/Util.java", "u"),
                                file("a/domain/User.java", "d"),
                                file("b/BController.java", "c2"),
                                file("a/AController.java", "c1"),
                                file("App.vue", "v"),
                                file("a/UserService.java", "s"),
                                file("application.yml", "y"),
                                file("main.js", "j"),
                                file("a/UserMapper.xml", "m")),
                        30);

        assertThat(selectedPaths(ranked))
                .containsExactly(
                        "a/AController.java",
                        "b/BController.java",
                        "a/UserService.java",
                        "a/UserMapper.xml",
                        "App.vue",
                        "main.js",
                        "application.yml",
                        "a/domain/User.java",
                        "z/Util.java");
        assertThat(ranked.get(0).layer()).isEqualTo(Layer.CONTROLLER);
        assertThat(ranked.get(0).content()).isEqualTo("c1");
    }

    @Test
    void F05a_AC7_상한을_넘으면_점수_상위만_고른다() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            files.add(file(String.format("u/Util%02d.java", i), "u"));
        }
        files.add(file("z/ZController.java", "c"));

        List<String> selected = selectedPaths(ranker.rank(files, 30));

        assertThat(selected).hasSize(30);
        assertThat(selected.get(0)).isEqualTo("z/ZController.java");
        assertThat(selected.get(29)).isEqualTo("u/Util28.java");
    }

    @Test
    void F07_AC12_선별할_때_내용으로_controller를_판정한다() {
        List<RankedFile> ranked =
                ranker.rank(
                        List.of(
                                file("a/Util.java", "class Util {}"),
                                file(
                                        "web/ArticlesApi.java",
                                        "@RestController class ArticlesApi {}")),
                        30);

        assertThat(ranked.get(0).path()).isEqualTo("web/ArticlesApi.java");
        assertThat(ranked.get(0).layer()).isEqualTo(Layer.CONTROLLER);
    }

    @Test
    void AC11_그래프_예산을_다_쓰면_실패하지_않고_연결_수_0으로_선별하고_경고를_남긴다(CapturedOutput output) {
        ImportanceRanker exhausted =
                new ImportanceRanker(new SelectionProperties(3, 0, Duration.ofSeconds(10)));

        List<RankedFile> ranked = exhausted.rank(usedAndIdleServices(), 4);

        Map<String, RankedFile> rows = byPath(ranked);
        assertThat(rows.get("src/main/java/a/service/UsedService.java").importance()).isEqualTo(8);
        assertThat(rows.get("src/main/java/a/web/C1Controller.java").importance()).isEqualTo(10);
        // 점수가 같은 두 service는 경로순: IdleService가 먼저
        assertThat(selectedPaths(ranked))
                .contains("src/main/java/a/service/IdleService.java")
                .doesNotContain("src/main/java/a/service/UsedService.java");
        assertThat(output).contains("선별 그래프 계산 예산");
    }
}
