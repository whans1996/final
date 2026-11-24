package com.vibenote.analysis.structure.xml;

import static com.vibenote.analysis.structure.StructureFiles.file;
import static com.vibenote.analysis.structure.StructureFiles.java;
import static org.assertj.core.api.Assertions.assertThat;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureEdge;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedLink;
import com.vibenote.analysis.structure.UnresolvedReason;
import com.vibenote.analysis.structure.java.JavaStructureReader;
import com.vibenote.analysis.structure.java.JavaTypeIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** F-21a AC4 MyBatis XML ↔ Java (docs/specs/F-21a-structure-graph.md). */
class MyBatisXmlReaderTest {

    private static final String DOCTYPE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\""
                    + " \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n";

    private static final List<AnalysisFile> JAVA =
            List.of(
                    java(
                            1,
                            "com.example.todo.TodoMapper",
                            "package com.example.todo;\npublic interface TodoMapper {}"),
                    java(
                            2,
                            "com.example.todo.Todo",
                            "package com.example.todo;\npublic class Todo {}"),
                    java(
                            3,
                            "com.example.todo.TodoQuery",
                            "package com.example.todo;\npublic record TodoQuery() {}"));

    private static AnalysisFile mapperXml(long id, String content) {
        return file(id, "src/main/resources/mapper/TodoMapper.xml", "mapper", content);
    }

    private static StructureLinks read(AnalysisFile... xml) {
        StructureLinks links = new StructureLinks();
        JavaTypeIndex index = JavaStructureReader.read(JAVA, links);
        StructureLinks out = new StructureLinks();
        MyBatisXmlReader.read(List.of(xml), index, out);
        return out;
    }

    @Test
    void AC4_namespace는_Java에서_XML로_타입_속성은_XML에서_Java로_간선() {
        StructureLinks links =
                read(
                        mapperXml(
                                10,
                                DOCTYPE
                                        + """
                                        <mapper namespace="com.example.todo.TodoMapper">
                                          <resultMap id="todoMap" type="com.example.todo.Todo">
                                            <result property="id" column="id" javaType="long"/>
                                          </resultMap>
                                          <select id="find" parameterType=" com.example.todo.TodoQuery "
                                                  resultType="com.example.todo.Todo">
                                            SELECT * FROM todos
                                          </select>
                                          <select id="count" resultType="int">SELECT COUNT(*) FROM todos</select>
                                        </mapper>
                                        """));

        assertThat(links.edges())
                .containsExactlyInAnyOrder(
                        new StructureEdge(1, 10, EdgeType.MYBATIS, 1),
                        new StructureEdge(10, 2, EdgeType.MYBATIS, 4),
                        new StructureEdge(10, 3, EdgeType.MYBATIS, 7));
        assertThat(links.unresolved()).isEmpty();
    }

    @Test
    void AC4_루트_패키지_아래_선별되지_않은_namespace는_NOT_SELECTED이고_외부_namespace는_무시() {
        StructureLinks links =
                read(
                        mapperXml(
                                10,
                                DOCTYPE
                                        + "<mapper"
                                        + " namespace=\"com.example.todo.user.UserMapper\">\n"
                                        + "</mapper>"),
                        mapperXml(11, "<mapper namespace=\"org.other.Mapper\"></mapper>"));

        assertThat(links.edges()).isEmpty();
        assertThat(links.unresolved())
                .containsExactly(
                        new UnresolvedLink(
                                10,
                                3,
                                UnresolvedKind.IMPORT,
                                UnresolvedReason.NOT_SELECTED,
                                "com.example.todo.user.UserMapper"));
    }

    @Test
    void AC4_읽지_못하는_XML은_PARSE_ERROR() {
        StructureLinks links =
                read(mapperXml(10, "<mapper namespace=\"com.example.todo.TodoMapper\">\n<select>"));

        assertThat(links.edges()).isEmpty();
        assertThat(links.unresolved()).hasSize(1);
        UnresolvedLink error = links.unresolved().get(0);
        assertThat(error.fileId()).isEqualTo(10);
        assertThat(error.kind()).isEqualTo(UnresolvedKind.PARSE);
        assertThat(error.reason()).isEqualTo(UnresolvedReason.PARSE_ERROR);
        assertThat(error.detail()).isNotBlank().hasSizeLessThanOrEqualTo(200);
    }

    @Test
    void AC4_외부_엔티티와_외부_DTD를_읽지_않는다(@TempDir Path dir) throws Exception {
        Path external = dir.resolve("ns.ent");
        Files.writeString(external, "<!ENTITY ns \"com.example.todo.TodoMapper\">");
        String xxe =
                "<?xml version=\"1.0\"?>\n"
                        + "<!DOCTYPE mapper [<!ENTITY % ext SYSTEM \""
                        + external.toUri()
                        + "\"> %ext;]>\n"
                        + "<mapper namespace=\"&ns;\"></mapper>";

        StructureLinks links = read(mapperXml(10, xxe));

        // 외부 파라미터 엔티티를 읽었다면 namespace가 TodoMapper로 풀려 간선이 생긴다
        assertThat(links.edges()).isEmpty();
    }
}
