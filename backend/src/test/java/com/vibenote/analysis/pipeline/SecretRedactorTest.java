package com.vibenote.analysis.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * 키 패턴 마스킹 (F-05a 5.2절, AC6). 테스트용 키는 형식만 맞춘 가짜 값이고, 하네스 키 가드(guard-files)에 걸리지 않게 문자열을 이어 붙여 만든다.
 */
class SecretRedactorTest {

    private static final String GOOGLE = "AIza" + "a".repeat(35);
    private static final String OPENAI = "sk-" + "b".repeat(24);
    private static final String GHP = "ghp_" + "c".repeat(36);
    private static final String GHO = "gho_" + "C".repeat(36);
    private static final String GITHUB_PAT = "github_pat_" + "d".repeat(30);
    private static final String AWS = "AKIA" + "E".repeat(16);
    private static final String PEM_BEGIN = "-----BEGIN " + "RSA PRIVATE KEY-----";
    private static final String PEM_END = "-----END " + "RSA PRIVATE KEY-----";

    @Test
    void AC6_코드_안의_키_패턴을_가린다() {
        String code =
                String.join(
                        "\n",
                        "class Keys {",
                        "  String g = \"" + GOOGLE + "\";",
                        "  String o = \"" + OPENAI + "\";",
                        "  String h = \"" + GHP + "\";",
                        "  String h2 = \"" + GHO + "\";",
                        "  String p = \"" + GITHUB_PAT + "\";",
                        "  String a = \"" + AWS + "\";",
                        "}");

        String redacted = SecretRedactor.redact("src/Keys.java", code);

        assertThat(redacted)
                .doesNotContain(GOOGLE, OPENAI, GHP, GHO, GITHUB_PAT, AWS)
                .isEqualTo(
                        String.join(
                                "\n",
                                "class Keys {",
                                "  String g = \"[REDACTED]\";",
                                "  String o = \"[REDACTED]\";",
                                "  String h = \"[REDACTED]\";",
                                "  String h2 = \"[REDACTED]\";",
                                "  String p = \"[REDACTED]\";",
                                "  String a = \"[REDACTED]\";",
                                "}"));
    }

    @Test
    void AC6_개인키_블록을_통째로_가린다() {
        String pem =
                String.join(
                        "\n",
                        "const key = `",
                        PEM_BEGIN,
                        "MIIEowIBAAKCAQEAsecretsecret",
                        "abcdefg==",
                        PEM_END,
                        "`;",
                        "export default key;");

        String redacted = SecretRedactor.redact("src/key.js", pem);

        assertThat(redacted)
                .isEqualTo(
                        String.join(
                                "\n", "const key = `", "[REDACTED]", "`;", "export default key;"));
    }

    @Test
    void AC6_닫는_줄이_없는_개인키는_끝까지_가린다() {
        String redacted = SecretRedactor.redact("a.js", "x\n" + PEM_BEGIN + "\nMIIEsecret\nmore");

        assertThat(redacted).isEqualTo("x\n[REDACTED]");
    }

    @Test
    void AC6_짧거나_다른_단어의_일부인_문자열은_그대로_둔다() {
        String code = "const sk = 'sk-short'; const css = 'task-list-item-container-wrapper';";

        assertThat(SecretRedactor.redact("a.js", code)).isEqualTo(code);
    }

    @Test
    void AC6_yml_설정의_비밀_항목_값을_가린다() {
        String yml =
                String.join(
                        "\n",
                        "spring:",
                        "  datasource:",
                        "    url: jdbc:mysql://localhost/db",
                        "    password: hunter2",
                        "app:",
                        "  jwt:",
                        "    secret: abc",
                        "  api-key: xyz",
                        "  apiKey: \"quoted\"",
                        "  github_token: t0k",
                        "  client-credential: cred",
                        "  db-password: ${DB_PASSWORD}",
                        "  other-secret: ${OTHER:default}",
                        "  empty-secret:",
                        "  name: vibenote");

        String redacted = SecretRedactor.redact("src/main/resources/application.yml", yml);

        assertThat(redacted)
                .doesNotContain("hunter2", "abc", "xyz", "quoted", "t0k", "cred\n")
                .isEqualTo(
                        String.join(
                                "\n",
                                "spring:",
                                "  datasource:",
                                "    url: jdbc:mysql://localhost/db",
                                "    password: [REDACTED]",
                                "app:",
                                "  jwt:",
                                "    secret: [REDACTED]",
                                "  api-key: [REDACTED]",
                                "  apiKey: [REDACTED]",
                                "  github_token: [REDACTED]",
                                "  client-credential: [REDACTED]",
                                "  db-password: ${DB_PASSWORD}",
                                "  other-secret: ${OTHER:[REDACTED]}",
                                "  empty-secret:",
                                "  name: vibenote"));
    }

    @Test
    void AC6_properties_설정의_비밀_항목_값을_가린다() {
        String props =
                String.join(
                        "\n",
                        "jwt.secret=abc",
                        "spring.datasource.password = hunter2",
                        "api_key: xyz",
                        "db.password=${DB_PASSWORD}",
                        "server.port=8080");

        String redacted = SecretRedactor.redact("application.properties", props);

        assertThat(redacted)
                .isEqualTo(
                        String.join(
                                "\n",
                                "jwt.secret=[REDACTED]",
                                "spring.datasource.password = [REDACTED]",
                                "api_key: [REDACTED]",
                                "db.password=${DB_PASSWORD}",
                                "server.port=8080"));
    }

    @Test
    void AC6_환경변수_참조의_기본값은_가린다() {
        String yml =
                String.join(
                        "\n",
                        "password: ${DB_PASSWORD:hunter2}",
                        "jwt.secret: \"${JWT_SECRET:my-real-secret}\"",
                        "api-key: ${API_KEY:}",
                        "token: ${A}${B:leak}",
                        "credential: ${OUTER:${INNER:leak}}");

        String redacted = SecretRedactor.redact("application.yml", yml);

        assertThat(redacted)
                .doesNotContain("hunter2", "my-real-secret", "leak")
                .isEqualTo(
                        String.join(
                                "\n",
                                "password: ${DB_PASSWORD:[REDACTED]}",
                                "jwt.secret: \"${JWT_SECRET:[REDACTED]}\"",
                                "api-key: ${API_KEY:}",
                                "token: [REDACTED]",
                                "credential: [REDACTED]"));
    }

    @Test
    void AC6_yml_블록_스칼라_값은_이어지는_줄까지_가린다() {
        String yml =
                String.join(
                        "\n",
                        "app:",
                        "  jwt-secret: |",
                        "    line-one",
                        "",
                        "    line-two",
                        "  private-token: >-",
                        "    folded",
                        "  name: vibenote",
                        "password: |",
                        "  top",
                        "url: x");

        String redacted = SecretRedactor.redact("application.yaml", yml);

        assertThat(redacted)
                .doesNotContain("line-one", "line-two", "folded", "top")
                .isEqualTo(
                        String.join(
                                "\n",
                                "app:",
                                "  jwt-secret: |",
                                "    [REDACTED]",
                                "",
                                "    [REDACTED]",
                                "  private-token: >-",
                                "    [REDACTED]",
                                "  name: vibenote",
                                "password: |",
                                "  [REDACTED]",
                                "url: x"));
    }

    @Test
    void AC6_CRLF_줄바꿈을_유지한다() {
        String yml = "password: hunter2\r\nsecret: |\r\n  abc\r\nname: x\r\n";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo("password: [REDACTED]\r\nsecret: |\r\n  [REDACTED]\r\nname: x\r\n");
    }

    @Test
    void AC6_구분자_없는_긴_줄도_선형_시간에_처리한다() {
        String line = "token".repeat(200_000);

        String redacted =
                assertTimeoutPreemptively(
                        Duration.ofSeconds(2),
                        () -> SecretRedactor.redact("application.yml", line + "\npassword: x"));

        assertThat(redacted).isEqualTo(line + "\npassword: [REDACTED]");
    }

    @Test
    void AC6_설정_파일이_아니면_키_이름_규칙은_적용하지_않는다() {
        String code = "String password = \"hunter2\";";

        assertThat(SecretRedactor.redact("src/A.java", code)).isEqualTo(code);
    }
}
