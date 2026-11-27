package com.vibenote.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * application.yml이 PC마다 두는 {@code ~/.vibenote/.env}를 읽는지 본다 (#249). 비밀 파일을 AI가 검색하는 저장소 폴더 밖에 둔다.
 *
 * <p>{@code user.home}을 임시 폴더로 바꿔 실제 PC의 파일은 건드리지 않는다. Windows에서는 홈 경로에 백슬래시가 들어가므로 그 경로로도 import가
 * 되는지 함께 확인된다.
 */
class EnvFileImportTest {

    @TempDir Path home;

    @Test
    void 홈의_vibenote_env를_설정으로_읽는다() throws IOException {
        Path dir = Files.createDirectories(home.resolve(".vibenote"));
        Files.writeString(dir.resolve(".env"), "VIBENOTE_ENV_IMPORT_TEST=home\n");

        assertThat(load(home).getProperty("VIBENOTE_ENV_IMPORT_TEST")).isEqualTo("home");
    }

    @Test
    void 홈에_파일이_없어도_기동은_된다() {
        assertThat(load(home).getProperty("VIBENOTE_ENV_IMPORT_TEST")).isNull();
    }

    /** application.yml은 루트 .env 다음에 ~/.vibenote/.env를 적는다. 둘 다 있으면 ~/.vibenote가 쓰이는 근거다 */
    @Test
    void import_목록에서_뒤에_적은_파일이_이긴다() throws IOException {
        Path first =
                Files.writeString(home.resolve("first.env"), "VIBENOTE_ENV_IMPORT_TEST=first\n");
        Path second =
                Files.writeString(home.resolve("second.env"), "VIBENOTE_ENV_IMPORT_TEST=second\n");
        String imports =
                "optional:file:"
                        + first
                        + "[.properties],optional:file:"
                        + second
                        + "[.properties]";

        StandardEnvironment environment =
                load(Map.of("user.home", home.toString(), "spring.config.import", imports));

        assertThat(environment.getProperty("VIBENOTE_ENV_IMPORT_TEST")).isEqualTo("second");
    }

    @Test
    void application_yml은_루트_env_다음에_홈_env를_적는다() throws IOException {
        String yml =
                new ClassPathResource("application.yml").getContentAsString(StandardCharsets.UTF_8);
        int root = yml.indexOf("optional:file:../.env[.properties]");
        int userHome = yml.indexOf("optional:file:${user.home}/.vibenote/.env[.properties]");

        assertThat(root).isNotNegative();
        assertThat(userHome).isGreaterThan(root);
    }

    private static StandardEnvironment load(Path userHome) {
        return load(Map.of("user.home", userHome.toString()));
    }

    private static StandardEnvironment load(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }
}
