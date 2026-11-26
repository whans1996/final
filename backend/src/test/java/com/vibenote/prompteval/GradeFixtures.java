package com.vibenote.prompteval;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 채점기 테스트의 합성 보고서·라벨 (E-1 AC5~AC11). 모두 dev 분할 {@value #FIXTURE}의 경로 이름만 빌려 손으로 만든 값이다. eval
 * 저장소·라벨은 쓰지 않는다 (AC11이 검사한다).
 */
final class GradeFixtures {

    static final String FIXTURE = "DS-02-01";
    static final ObjectMapper JSON = JsonMapper.builder().build();

    static final String API = "src/main/java/io/spring/api/ArticlesApi.java";
    static final String SVC =
            "src/main/java/io/spring/application/article/ArticleCommandService.java";
    static final String MAP =
            "src/main/java/io/spring/infrastructure/mybatis/mapper/ArticleMapper.java";

    /** 보고서의 흐름 단계 심볼이 가리키는 파일 본문 (ArticleMapper에는 insertArticle이 없다). */
    static final Map<String, String> SOURCES =
            Map.of(
                    API,
                    """
                    package io.spring.api;

                    @RestController
                    @RequestMapping("/articles")
                    public class ArticlesApi {
                        private final ArticleCommandService articleCommandService;

                        @PostMapping
                        public Object createArticle(@RequestBody Object param) {
                            return articleCommandService.createArticle(param);
                        }

                        @GetMapping
                        public Object getArticles() {
                            return null;
                        }
                    }
                    """,
                    SVC,
                    """
                    package io.spring.application.article;

                    @Service
                    public class ArticleCommandService {
                        @Transactional
                        public Object createArticle(Object param) {
                            return articleMapper.insert(param);
                        }
                    }
                    """,
                    MAP,
                    """
                    package io.spring.infrastructure.mybatis.mapper;

                    @Mapper
                    public interface ArticleMapper {
                        Object insert(Object article);

                        Object findById(String id);
                    }
                    """);

    private GradeFixtures() {}

    /** {@link #SOURCES}를 담은 ZIP (최상위 폴더 하나 아래, 앱 ZIP 업로드와 같다). */
    static Path zip(Path dir) {
        Path zip = dir.resolve(FIXTURE + ".zip");
        try (OutputStream os = Files.newOutputStream(zip);
                ZipOutputStream out = new ZipOutputStream(os)) {
            for (Map.Entry<String, String> f : SOURCES.entrySet()) {
                out.putNextEntry(new ZipEntry("spring-boot-realworld-example-app/" + f.getKey()));
                out.write(f.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return zip;
    }

    /** 세 수준이 다 있는 합성 보고서. {@code fixtureZip}을 {@code zip}으로 채운다. */
    static ObjectNode report(Path zip) {
        ObjectNode report = (ObjectNode) resource("reports/" + FIXTURE + "-all.json");
        report.put("fixtureZip", zip.toAbsolutePath().toString());
        return report;
    }

    static ObjectNode label() {
        return (ObjectNode) resource("labels/" + FIXTURE + ".json");
    }

    static JsonNode resource(String name) {
        try (InputStream in =
                GradeFixtures.class.getResourceAsStream("/prompt-eval/synthetic/" + name)) {
            if (in == null) {
                throw new IllegalStateException("테스트 리소스 없음: " + name);
            }
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
