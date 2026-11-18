package com.vibenote.analysis.explain;

import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.AnalysisFile;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-06 AC7 규칙 기반 API 탐지 (docs/specs/F-06-overview-explanation.md). */
class ApiDetectorTest {

    static AnalysisFile file(long id, String path, String layer, String content) {
        AnalysisFile f = new AnalysisFile();
        f.setId(id);
        f.setAnalysisId(1L);
        f.setPath(path);
        f.setLayer(layer);
        f.setContent(content);
        return f;
    }

    static AnalysisFile controller(long id, String content) {
        return file(id, "src/main/java/web/C" + id + ".java", "controller", content);
    }

    @Test
    void AC7_클래스_경로와_메서드_경로를_합치고_path_method_순으로_정렬한다() {
        String code =
                """
                @RestController
                @RequestMapping("/api/users")
                public class UserController {
                    @GetMapping
                    public List<User> list() { return null; }

                    @PostMapping("")
                    public User create() { return null; }

                    @GetMapping("/{id}")
                    public User get(@PathVariable long id) { return null; }

                    @PatchMapping(value = "/{id}", produces = "application/json")
                    public User update() { return null; }

                    @DeleteMapping(path = "/{id}")
                    public void delete() {}

                    @PutMapping({"/{id}/name", "/{id}/nick"})
                    public void rename() {}
                }
                """;

        assertThat(ApiDetector.detect(List.of(controller(7, code))))
                .containsExactly(
                        new ApiEndpoint("GET", "/api/users", 7L),
                        new ApiEndpoint("POST", "/api/users", 7L),
                        new ApiEndpoint("DELETE", "/api/users/{id}", 7L),
                        new ApiEndpoint("GET", "/api/users/{id}", 7L),
                        new ApiEndpoint("PATCH", "/api/users/{id}", 7L),
                        new ApiEndpoint("PUT", "/api/users/{id}/name", 7L),
                        new ApiEndpoint("PUT", "/api/users/{id}/nick", 7L));
    }

    @Test
    void AC7_RequestMapping의_method_속성과_method가_없으면_ANY() {
        String code =
                """
                @Controller
                public class LegacyController {
                    @RequestMapping(value = "/legacy", method = RequestMethod.POST)
                    public String post() { return "x"; }

                    @RequestMapping(path = "/multi", method = {RequestMethod.GET, RequestMethod.HEAD})
                    public String multi() { return "x"; }

                    @RequestMapping("/any")
                    public String any() { return "x"; }
                }
                """;

        assertThat(ApiDetector.detect(List.of(controller(3, code))))
                .containsExactly(
                        new ApiEndpoint("ANY", "/any", 3L),
                        new ApiEndpoint("POST", "/legacy", 3L),
                        new ApiEndpoint("GET", "/multi", 3L),
                        new ApiEndpoint("HEAD", "/multi", 3L));
    }

    @Test
    void AC7_슬래시를_하나로_정규화한다() {
        String code =
                """
                @RequestMapping("api/v1/")
                class A {
                    @GetMapping("/items/") void a() {}
                    @GetMapping("//things") void b() {}
                }
                """;

        assertThat(ApiDetector.detect(List.of(controller(1, code))))
                .extracting(ApiEndpoint::path)
                .containsExactly("/api/v1/items", "/api/v1/things");
    }

    @Test
    void AC7_주석_처리된_어노테이션은_무시하고_문자열_안의_주석_기호는_그대로_둔다() {
        String code =
                """
                @RequestMapping("/api/*")
                class A {
                    // @GetMapping("/commented")
                    /* @PostMapping("/block")
                       @PutMapping("/block2") */
                    @GetMapping("/real") // trailing comment
                    void a() {}
                    /** javadoc @DeleteMapping("/doc") */
                    @GetMapping(value = "/str//*x") void b() {}
                }
                """;

        assertThat(ApiDetector.detect(List.of(controller(1, code))))
                .containsExactly(
                        new ApiEndpoint("GET", "/api/*/real", 1L),
                        new ApiEndpoint("GET", "/api/*/str/*x", 1L));
    }

    @Test
    void AC7_controller_layer_java가_아니면_보지_않고_없으면_빈_배열() {
        String code = "class S { @GetMapping(\"/x\") void a() {} }";

        assertThat(
                        ApiDetector.detect(
                                List.of(
                                        file(1, "src/S.java", "service", code),
                                        file(2, "web/x.js", "controller", code))))
                .isEmpty();
        assertThat(ApiDetector.detect(List.of(controller(3, "class Empty {}")))).isEmpty();
        assertThat(ApiDetector.detect(List.of())).isEmpty();
    }

    @Test
    void AC7_중복을_제거하고_50개로_자른다() {
        StringBuilder code = new StringBuilder("@RequestMapping(\"/api\")\nclass A {\n");
        for (int i = 0; i < 60; i++) {
            code.append(String.format("  @GetMapping(\"/r%02d\") void m%d() {}%n", i, i));
        }
        code.append("}\n");
        List<AnalysisFile> files = new ArrayList<>();
        files.add(controller(1, code.toString()));
        files.add(controller(2, code.toString()));

        List<ApiEndpoint> apis = ApiDetector.detect(files);

        assertThat(apis).hasSize(50).doesNotHaveDuplicates();
        assertThat(apis.get(0)).isEqualTo(new ApiEndpoint("GET", "/api/r00", 1L));
        assertThat(apis.get(49).path()).isEqualTo("/api/r49");
    }

    @Test
    void AC7_클래스_경로를_알_수_없으면_그_파일의_API는_건너뛴다() {
        String constant =
                """
                @RestController
                @RequestMapping(Paths.USERS)
                public class UserController {
                    @GetMapping("/{id}")
                    public User get() { return null; }
                }
                """;
        String literal =
                """
                @RestController
                @RequestMapping("/api/todos")
                public class TodoController {
                    @GetMapping
                    public List<Todo> list() { return null; }
                }
                """;

        assertThat(ApiDetector.detect(List.of(controller(1, constant), controller(2, literal))))
                .containsExactly(new ApiEndpoint("GET", "/api/todos", 2L));
    }
}
