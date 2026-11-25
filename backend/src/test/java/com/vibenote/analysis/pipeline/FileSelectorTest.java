package com.vibenote.analysis.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** layer 판정 (F-05a 5.2절 AC5, F-07 AC12, F-26a 5.3절 AC4). 선별은 {@link ImportanceRankerTest}. */
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
        // F-26a AC4: *.config.js는 js보다 config가 먼저다
        assertThat(layer("frontend/vite.config.js")).isEqualTo(Layer.CONFIG);
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
    void AC4_Service_어노테이션이면_파일명과_상관없이_service() {
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/AnalysisPipeline.java",
                                "@Service\npublic class AnalysisPipeline {}"))
                .isEqualTo(Layer.SERVICE);
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/Pipe.java",
                                "@org.springframework.stereotype.Service class Pipe {}"))
                .isEqualTo(Layer.SERVICE);
        assertThat(FileSelector.layer("src/main/java/a/Flow.java", "@ServiceActivator class F {}"))
                .isEqualTo(Layer.OTHER);
    }

    @Test
    void AC4_Repository_어노테이션과_Repository_파일명과_Mapper_어노테이션은_mapper() {
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/UserStore.java",
                                "@Repository\npublic class UserStore {}"))
                .isEqualTo(Layer.MAPPER);
        assertThat(layer("src/main/java/a/TodoRepository.java")).isEqualTo(Layer.MAPPER);
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/TodoQueries.java",
                                "@Mapper\npublic interface TodoQueries {}"))
                .isEqualTo(Layer.MAPPER);
    }

    @Test
    void AC4_Configuration_어노테이션이면_config이고_ConfigurationProperties는_아니다() {
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/WebSetup.java",
                                "@Configuration\npublic class WebSetup {}"))
                .isEqualTo(Layer.CONFIG);
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/AppProps.java",
                                "@ConfigurationProperties(\"app\")\npublic record AppProps() {}"))
                .isEqualTo(Layer.OTHER);
    }

    @Test
    void AC4_models_밖의_Entity는_domain() {
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/models/Todo.java",
                                "@Entity\npublic class Todo {}"))
                .isEqualTo(Layer.DOMAIN);
        assertThat(
                        FileSelector.layer(
                                "src/main/java/a/Todo.java",
                                "@Table(name = \"todos\")\npublic class Todo {}"))
                .isEqualTo(Layer.DOMAIN);
    }

    @Test
    void AC4_JS_설정_파일은_js가_아니라_config() {
        assertThat(layer("frontend/vite.config.js")).isEqualTo(Layer.CONFIG);
        assertThat(layer("eslint.config.mjs")).isEqualTo(Layer.CONFIG);
        assertThat(layer("frontend/vitest.config.ts")).isEqualTo(Layer.CONFIG);
        assertThat(layer("frontend/src/config.js")).isEqualTo(Layer.JS);
    }

    @Test
    void AC4_테스트_폴더와_테스트_파일명은_test() {
        assertThat(
                        FileSelector.layer(
                                "src/test/java/FooControllerTest.java",
                                "@RestController class FooControllerTest {}"))
                .isEqualTo(Layer.TEST);
        assertThat(layer("src/main/java/a/FooIT.java")).isEqualTo(Layer.TEST);
        assertThat(layer("frontend/src/views/__tests__/Home.spec.js")).isEqualTo(Layer.TEST);
        assertThat(layer("frontend/src/a.test.ts")).isEqualTo(Layer.TEST);
        assertThat(layer("e2e/login.js")).isEqualTo(Layer.TEST);
        assertThat(layer("src/main/java/a/Contest.java")).isEqualTo(Layer.OTHER);
    }

    @Test
    void AC4_마이그레이션_폴더와_Flyway_파일명은_migration이고_md는_doc() {
        assertThat(layer("src/main/resources/db/migration/V1__init.sql"))
                .isEqualTo(Layer.MIGRATION);
        assertThat(layer("db/changelog/master.yaml")).isEqualTo(Layer.MIGRATION);
        assertThat(layer("migrations/001.sql")).isEqualTo(Layer.MIGRATION);
        assertThat(layer("sql/V2__add.sql")).isEqualTo(Layer.MIGRATION);
        assertThat(layer("src/main/resources/schema.sql")).isEqualTo(Layer.OTHER);
        assertThat(layer("README.md")).isEqualTo(Layer.DOC);
        assertThat(layer("docs/test/guide.md")).isEqualTo(Layer.DOC);
    }

    @Test
    void AC4_주석_속_어노테이션은_보지_않는다() {
        String commented =
                """
                // @Service
                /* @Repository @Configuration @Entity */
                public class Plain {}
                """;

        assertThat(FileSelector.layer("src/main/java/a/Plain.java", commented))
                .isEqualTo(Layer.OTHER);
    }
}
