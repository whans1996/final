package com.vibenote.analysis.structure.java;

import static com.vibenote.analysis.structure.StructureFiles.java;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureEdge;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedLink;
import com.vibenote.analysis.structure.UnresolvedReason;
import java.util.List;
import org.junit.jupiter.api.Test;

/** F-21a AC1~AC3 Java 타입 참조 (docs/specs/F-21a-structure-graph.md). */
class JavaStructureReaderTest {

    private static StructureLinks read(AnalysisFile... files) {
        StructureLinks links = new StructureLinks();
        JavaStructureReader.read(List.of(files), links);
        return links;
    }

    private static StructureEdge imp(long from, long to, int line) {
        return new StructureEdge(from, to, EdgeType.IMPORT, line);
    }

    private static AnalysisFile type(long id, String pkg, String declaration) {
        String name = declaration.replaceAll("^.*?(?:class|interface|record|enum) (\\w+).*$", "$1");
        return java(id, pkg + "." + name, "package " + pkg + ";\n" + declaration + "\n");
    }

    @Test
    void AC1_같은_패키지_타입을_필드로_쓰면_그_줄로_간선을_만든다() {
        StructureLinks links =
                read(
                        java(
                                1,
                                "com.example.todo.TodoController",
                                """
                                package com.example.todo;

                                public class TodoController {
                                    private final TodoService todoService;
                                }
                                """),
                        type(2, "com.example.todo", "public class TodoService {}"));

        assertThat(links.edges()).containsExactly(imp(1, 2, 4));
        assertThat(links.unresolved()).isEmpty();
    }

    @Test
    void AC1_단일_와일드카드_static_import와_FQN을_해석한다() {
        StructureLinks links =
                read(
                        java(
                                1,
                                "com.example.web.Main",
                                """
                                package com.example.web;

                                import com.example.todo.TodoService;
                                import com.example.dto.*;
                                import static com.example.util.Strings.trim;

                                public class Main {
                                    TodoService service;
                                    TodoDto dto;
                                    com.example.domain.Todo todo;
                                }
                                """),
                        type(2, "com.example.todo", "public class TodoService {}"),
                        type(3, "com.example.dto", "public record TodoDto() {}"),
                        type(4, "com.example.util", "public class Strings {}"),
                        type(5, "com.example.domain", "public class Todo {}"));

        // 단일 타입·static import 줄은 타입 이름이 처음 나온 곳이다. 와일드카드 import 줄은 패키지라 근거가 아니다
        assertThat(links.edges())
                .containsExactlyInAnyOrder(imp(1, 2, 3), imp(1, 3, 9), imp(1, 4, 5), imp(1, 5, 10));
    }

    @Test
    void AC1_코드의_여러_위치에_나온_타입_이름을_모두_간선으로_만든다() {
        String main =
                """
                package com.example.app;

                @Marker
                public class Main extends Base implements Api {
                    java.util.List<Item> items;
                    Object make() { return new Created(); }
                    Object cast(Object o) { return (Casted) o; }
                    boolean check(Object o) { return o instanceof Checked; }
                    Class<?> type() { return Literal.class; }
                    Runnable ref() { return Referenced::run; }
                    void call() { Util.help(); }
                    Param take(Param p) { return p; }
                    void local() { Var v = null; }
                }
                """;
        String[] names = {
            "Marker",
            "Base",
            "Api",
            "Item",
            "Created",
            "Casted",
            "Checked",
            "Literal",
            "Referenced",
            "Util",
            "Param",
            "Var"
        };
        AnalysisFile[] files = new AnalysisFile[names.length + 1];
        files[0] = java(1, "com.example.app.Main", main);
        for (int i = 0; i < names.length; i++) {
            files[i + 1] = type(i + 10, "com.example.app", "class " + names[i] + " {}");
        }

        StructureLinks links = read(files);

        assertThat(links.edges())
                .containsExactlyInAnyOrder(
                        imp(1, 10, 3),
                        imp(1, 11, 4),
                        imp(1, 12, 4),
                        imp(1, 13, 5),
                        imp(1, 14, 6),
                        imp(1, 15, 7),
                        imp(1, 16, 8),
                        imp(1, 17, 9),
                        imp(1, 18, 10),
                        imp(1, 19, 11),
                        imp(1, 20, 12),
                        imp(1, 21, 13));
    }

    @Test
    void AC1_파일_안_중첩_타입이_같은_패키지_파일보다_먼저이고_남의_중첩_타입은_그_파일로_간선() {
        StructureLinks links =
                read(
                        java(
                                1,
                                "com.example.todo.TodoService",
                                """
                                package com.example.todo;

                                public class TodoService {
                                    static class Result {}
                                    Result run() { return new Result(); }
                                    TodoDto.Item item;
                                }
                                """),
                        type(2, "com.example.todo", "public class Result {}"),
                        type(
                                3,
                                "com.example.todo",
                                "public class TodoDto { public record Item() {} }"));

        assertThat(links.edges()).containsExactly(imp(1, 3, 6));
    }

    @Test
    void AC1_주석_문자열_지역_변수_이름과_자기_자신은_간선이_아니다() {
        StructureLinks links =
                read(
                        java(
                                1,
                                "com.example.todo.TodoService",
                                """
                                package com.example.todo;

                                /** {@link TodoMapper}를 쓴다. */
                                public class TodoService {
                                    // TodoMapper mapper;
                                    String name = "TodoMapper";
                                    int run() {
                                        String Helper = "x";
                                        TodoService self = this;
                                        return Helper.length();
                                    }
                                }
                                """),
                        type(2, "com.example.todo", "public interface TodoMapper {}"),
                        type(3, "com.example.todo", "public class Helper {}"));

        assertThat(links.edges()).isEmpty();
    }

    @Test
    void AC2_루트_패키지_아래의_선별되지_않은_타입은_파일당_한_번_NOT_SELECTED() {
        StructureLinks links =
                read(
                        java(
                                1,
                                "com.example.todo.TodoService",
                                """
                                package com.example.todo;

                                import java.util.List;
                                import org.springframework.stereotype.Service;
                                import com.example.todo.valid.TodoValidator;

                                @Service
                                public class TodoService {
                                    TodoValidator validator;
                                    com.example.todo.valid.TodoValidator again;
                                    com.example.common.Clock clock;
                                    List<String> names;
                                    Unknown unknown;
                                }
                                """),
                        type(2, "com.example.todo", "public interface TodoMapper {}"),
                        type(3, "com.example.user", "public class User {}"));

        // 루트 패키지는 com.example. import 없는 단순 이름(Unknown)은 FQN을 알 수 없어 남기지 않는다
        assertThat(links.edges()).isEmpty();
        assertThat(links.unresolved())
                .containsExactly(
                        new UnresolvedLink(
                                1,
                                5,
                                UnresolvedKind.IMPORT,
                                UnresolvedReason.NOT_SELECTED,
                                "com.example.todo.valid.TodoValidator"),
                        new UnresolvedLink(
                                1,
                                11,
                                UnresolvedKind.IMPORT,
                                UnresolvedReason.NOT_SELECTED,
                                "com.example.common.Clock"));
    }

    @Test
    void AC2_루트_패키지가_2단_미만이면_NOT_SELECTED를_만들지_않는다() {
        StructureLinks links =
                read(
                        java(
                                1,
                                "com.a.A",
                                "package com.a;\nimport com.a.Missing;\nclass A { Missing m; }"),
                        type(2, "org.b", "class B {}"));

        assertThat(links.unresolved()).isEmpty();
    }

    @Test
    void AC3_파싱되지_않는_파일은_PARSE_ERROR이고_다른_파일은_그_파일을_가리킬_수_있다() {
        StructureLinks links =
                read(
                        java(
                                1,
                                "com.example.todo.Broken",
                                """
                                // 깨진 파일
                                package com.example.todo;

                                public class Broken {
                                    TodoService service;
                                    void run( {
                                }
                                """),
                        type(2, "com.example.todo", "public class TodoService {}"),
                        java(
                                3,
                                "com.example.todo.User",
                                "package com.example.todo;\npublic class User {\n  Broken b;\n}"));

        assertThat(links.edges()).containsExactly(imp(3, 1, 3));
        assertThat(links.unresolved()).hasSize(1);
        UnresolvedLink error = links.unresolved().get(0);
        assertThat(error.fileId()).isEqualTo(1);
        assertThat(error.kind()).isEqualTo(UnresolvedKind.PARSE);
        assertThat(error.reason()).isEqualTo(UnresolvedReason.PARSE_ERROR);
        assertThat(error.detail()).isNotBlank().hasSizeLessThanOrEqualTo(200);
    }
}
