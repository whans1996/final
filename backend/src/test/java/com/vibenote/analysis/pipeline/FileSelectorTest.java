package com.vibenote.analysis.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** layer 판정과 선별 우선순위 (F-05a 5.2절, AC5·AC7). */
class FileSelectorTest {

    private static Layer layer(String path) {
        return FileSelector.layer(path, "");
    }

    @Test
    void AC5_layer_판정() {
        assertThat(layer("src/main/java/a/UserController.java")).isEqualTo(Layer.CONTROLLER);
        assertThat(layer("src/main/java/a/UserService.java")).isEqualTo(Layer.SERVICE);
        assertThat(layer("src/main/java/a/UserServiceImpl.java")).isEqualTo(Layer.SERVICE);
        assertThat(layer("src/main/java/a/UserMapper.java")).isEqualTo(Layer.MAPPER);
        assertThat(layer("src/main/java/a/UserDao.java")).isEqualTo(Layer.MAPPER);
        assertThat(layer("src/main/resources/UserMapper.xml")).isEqualTo(Layer.MAPPER);
        assertThat(layer("src/main/resources/mapper/user/user.xml")).isEqualTo(Layer.MAPPER);
        assertThat(layer("frontend/src/views/Home.vue")).isEqualTo(Layer.VUE);
        assertThat(layer("frontend/src/router/index.js")).isEqualTo(Layer.JS);
        assertThat(layer("frontend/src/stores/auth.js")).isEqualTo(Layer.JS);
        assertThat(layer("frontend/src/api/http.mjs")).isEqualTo(Layer.JS);
        assertThat(layer("frontend/vite.config.js")).isEqualTo(Layer.JS);
        assertThat(layer("src/main/java/a/SecurityConfig.java")).isEqualTo(Layer.CONFIG);
        assertThat(layer("src/main/resources/application.yml")).isEqualTo(Layer.CONFIG);
        assertThat(layer("src/main/resources/application-local.properties"))
                .isEqualTo(Layer.CONFIG);
        assertThat(layer("build.gradle")).isEqualTo(Layer.CONFIG);
        assertThat(layer("build.gradle.kts")).isEqualTo(Layer.CONFIG);
        assertThat(layer("frontend/package.json")).isEqualTo(Layer.CONFIG);
        assertThat(layer("pom.xml")).isEqualTo(Layer.CONFIG);
        assertThat(layer("src/main/java/a/domain/User.java")).isEqualTo(Layer.DOMAIN);
        assertThat(layer("src/main/java/a/entity/User.java")).isEqualTo(Layer.DOMAIN);
        assertThat(layer("src/main/java/a/dto/UserRequest.java")).isEqualTo(Layer.DOMAIN);
        assertThat(layer("src/main/java/a/model/User.java")).isEqualTo(Layer.DOMAIN);
        assertThat(layer("src/main/java/a/util/Strings.java")).isEqualTo(Layer.OTHER);
        assertThat(layer("src/main/resources/logback.xml")).isEqualTo(Layer.OTHER);
        assertThat(layer("schema.sql")).isEqualTo(Layer.OTHER);
    }

    @Test
    void AC5_layer_값은_소문자로_저장한다() {
        assertThat(Layer.CONTROLLER.value()).isEqualTo("controller");
        assertThat(Layer.OTHER.value()).isEqualTo("other");
    }

    @Test
    void AC5_우선순위와_경로순으로_정렬한다() {
        List<SourceFile> files =
                List.of(
                        new SourceFile("z/Util.java", "u"),
                        new SourceFile("a/domain/User.java", "d"),
                        new SourceFile("b/BController.java", "c2"),
                        new SourceFile("a/AController.java", "c1"),
                        new SourceFile("App.vue", "v"),
                        new SourceFile("a/UserService.java", "s"),
                        new SourceFile("application.yml", "y"),
                        new SourceFile("main.js", "j"),
                        new SourceFile("a/UserMapper.xml", "m"));

        List<SelectedFile> selected = FileSelector.select(files, 30);

        assertThat(selected)
                .extracting(SelectedFile::path)
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
        assertThat(selected.get(0).layer()).isEqualTo(Layer.CONTROLLER);
        assertThat(selected.get(0).content()).isEqualTo("c1");
    }

    @Test
    void AC7_상한을_넘으면_우선순위_상위만_고른다() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            files.add(new SourceFile(String.format("u/Util%02d.java", i), "u"));
        }
        files.add(new SourceFile("z/ZController.java", "c"));

        List<SelectedFile> selected = FileSelector.select(files, 30);

        assertThat(selected).hasSize(30);
        assertThat(selected.get(0).path()).isEqualTo("z/ZController.java");
        assertThat(selected.get(29).path()).isEqualTo("u/Util28.java");
    }

    @Test
    void AC12_파일명이_Controller가_아니어도_주석_밖_RestController면_controller() {
        String api =
                """
                package web;

                @RestController
                @RequestMapping("/api/articles")
                public class ArticlesApi {}
                """;
        String plainController =
                """
                @org.springframework.stereotype.Controller
                public class HomePage {}
                """;

        assertThat(FileSelector.layer("src/main/java/web/ArticlesApi.java", api))
                .isEqualTo(Layer.CONTROLLER);
        assertThat(FileSelector.layer("src/main/java/web/HomePage.java", plainController))
                .isEqualTo(Layer.CONTROLLER);
        assertThat(FileSelector.layer("src/main/java/a/UserController.java", ""))
                .isEqualTo(Layer.CONTROLLER);
    }

    @Test
    void AC12_주석_속_어노테이션과_ControllerAdvice는_controller가_아니다() {
        String commented =
                """
                // @RestController
                /* @Controller
                 */
                public class ArticlesApi {}
                """;
        String advice =
                """
                @RestControllerAdvice
                public class GlobalHandler {}
                """;
        String controllerAdvice = "@ControllerAdvice\npublic class Advice {}";

        assertThat(FileSelector.layer("src/main/java/web/ArticlesApi.java", commented))
                .isEqualTo(Layer.OTHER);
        assertThat(FileSelector.layer("src/main/java/web/GlobalHandler.java", advice))
                .isEqualTo(Layer.OTHER);
        assertThat(FileSelector.layer("src/main/java/web/Advice.java", controllerAdvice))
                .isEqualTo(Layer.OTHER);
        assertThat(FileSelector.layer("src/main/resources/x.xml", "@RestController"))
                .isEqualTo(Layer.OTHER);
    }

    @Test
    void AC12_선별할_때_내용으로_controller를_판정한다() {
        List<SelectedFile> selected =
                FileSelector.select(
                        List.of(
                                new SourceFile("a/Util.java", "class Util {}"),
                                new SourceFile(
                                        "web/ArticlesApi.java",
                                        "@RestController class ArticlesApi {}")),
                        30);

        assertThat(selected.get(0).path()).isEqualTo("web/ArticlesApi.java");
        assertThat(selected.get(0).layer()).isEqualTo(Layer.CONTROLLER);
    }
}
