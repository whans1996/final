package com.vibenote.analysis.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    void AC6_yml_흐름_매핑_안의_비밀_항목을_가린다() {
        String yml =
                String.join(
                        "\n",
                        "datasource: {username: app, password: hunter2}",
                        "jwt: {\"secret\": \"a,b\", ttl: 3600, nested: {api-key: xyz}}",
                        "- {token: ${TOKEN:t0k}, name: n}",
                        "env: {db-password: ${DB_PASSWORD}}");

        String redacted = SecretRedactor.redact("application.yml", yml);

        assertThat(redacted)
                .doesNotContain("hunter2", "a,b", "xyz", "t0k")
                .isEqualTo(
                        String.join(
                                "\n",
                                "datasource: {username: app, password: [REDACTED]}",
                                "jwt: {\"secret\": [REDACTED], ttl: 3600, nested: {api-key:"
                                        + " [REDACTED]}}",
                                "- {token: ${TOKEN:[REDACTED]}, name: n}",
                                "env: {db-password: ${DB_PASSWORD}}"));
    }

    @Test
    void AC6_properties_줄_이음으로_이어진_비밀_값을_끝까지_가린다() {
        String props =
                String.join(
                        "\n",
                        "spring.datasource.password=\\",
                        "    hunter2",
                        "jwt.secret=ab\\\\",
                        "server.port=8080");

        String redacted = SecretRedactor.redact("application.properties", props);

        assertThat(redacted)
                .doesNotContain("hunter2", "ab")
                .isEqualTo(
                        String.join(
                                "\n",
                                "spring.datasource.password=[REDACTED]",
                                "    [REDACTED]",
                                "jwt.secret=[REDACTED]",
                                "server.port=8080"));
    }

    @Test
    void AC6_원문에_그대로_없는_비밀_값이_있으면_파일_전체를_가린다() {
        // 이스케이프·줄 이음으로 해석한 값이 원문 글자와 다르면 위치를 찾을 수 없다
        assertThat(
                        SecretRedactor.redact(
                                "application.properties",
                                "spring.datasource.password hun\\u0074er2\nserver.port 8080"))
                .isEqualTo("[REDACTED]");
        assertThat(
                        SecretRedactor.redact(
                                "application.yml", "secret:\n  key: \"hun\\x74er2\"\nname: x"))
                .isEqualTo("[REDACTED]");
        assertThat(
                        SecretRedactor.redact(
                                "application.properties", "password=hun\\\n    ter2\nport=1"))
                .isEqualTo("[REDACTED]");
    }

    @Test
    void AC6_properties_공백_구분_항목도_줄_단위로_가린다() {
        // 파서가 읽지 못하는 파일(잘못된 유니코드 이스케이프)이라도 1차에서 가린다
        String props = "spring.datasource.password hunter2\nbad=\\uZZZZ";

        assertThat(SecretRedactor.redact("application.properties", props))
                .isEqualTo("spring.datasource.password [REDACTED]\nbad=\\uZZZZ");
    }

    @Test
    void AC6_yml_다음_줄에_오는_값과_비밀_키_아래_하위_값을_가린다() {
        String yml =
                String.join(
                        "\n",
                        "password:",
                        "  hunter2",
                        "jwt-secret:",
                        "  key: s3cr3t-value",
                        "  ttl: 3600",
                        "token-enabled: true",
                        "flag: true",
                        "ttl: 3600",
                        "url: jdbc:mysql://localhost/app");

        String redacted = SecretRedactor.redact("application.yml", yml);

        assertThat(redacted)
                .doesNotContain("hunter2", "s3cr3t-value")
                .isEqualTo(
                        String.join(
                                "\n",
                                "password:",
                                "  [REDACTED]",
                                "jwt-secret:",
                                "  key: [REDACTED]",
                                "  ttl: 3600",
                                "token-enabled: [REDACTED]",
                                "flag: true",
                                "ttl: 3600",
                                "url: jdbc:mysql://localhost/app"));
    }

    @Test
    void AC6_properties_공백으로_구분한_비밀_항목을_가린다() {
        String props = "spring.datasource.password hunter2\nserver.port 8080\napp.name hunter";

        assertThat(SecretRedactor.redact("application.properties", props))
                .isEqualTo(
                        "spring.datasource.password [REDACTED]\nserver.port 8080\napp.name hunter");
    }

    @Test
    void AC6_순환하는_yml_별칭도_끝난다() {
        String yml = "secret: &a\n  - leaked\n  - *a\nname: x";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .doesNotContain("leaked")
                .isEqualTo("secret: [REDACTED]\n  - [REDACTED]\n  - *a\nname: x");
    }

    @Test
    void AC6_비밀_키_밖에서_정의한_yml_별칭을_비밀_키_아래에서_쓰면_가린다() {
        String yml = "defaults: &creds\n  value: hunter2\nsecret: *creds\n";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .doesNotContain("hunter2")
                .isEqualTo("defaults: &creds\n  value: [REDACTED]\nsecret: [REDACTED]\n");
    }

    @Test
    void AC6_비밀_토큰이_상한을_넘으면_파일_전체를_가린다() {
        String yml =
                "password: |\n  "
                        + String.join(
                                " ",
                                IntStream.rangeClosed(0, SecretRedactor.MAX_SECRET_TOKENS)
                                        .mapToObj(i -> "w" + i)
                                        .toList());

        assertThat(SecretRedactor.redact("application.yml", yml)).isEqualTo("[REDACTED]");
    }

    @Test
    void AC6_해석할_수_없는_yml도_줄_단위로_가린다() {
        String yml = "password: hunter2\n  bad: [\n";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo("password: [REDACTED]\n  bad: [\n");
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

        String flow = "a: {password: " + "${\"".repeat(200_000);
        assertTimeoutPreemptively(
                Duration.ofSeconds(2), () -> SecretRedactor.redact("application.yml", flow));
    }

    @Test
    void 이슈16_key로_끝나는_설정_키의_값을_가린다() {
        String yml =
                String.join(
                        "\n",
                        "jwt:",
                        "  key: k1-value",
                        "  private-key: k2-value",
                        "aws:",
                        "  access_key: k3-value",
                        "  signingKey: k4-value",
                        "  privatekey: k5-value",
                        "  accesskey: k6-value",
                        "azure:",
                        "  primary-key: k7-value");
        String properties =
                String.join(
                        "\n",
                        "cloud.aws.access-key=p1-value",
                        "encryption.key=p2-value",
                        "MASTER_KEY=p3-value");

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo(
                        String.join(
                                "\n",
                                "jwt:",
                                "  key: [REDACTED]",
                                "  private-key: [REDACTED]",
                                "aws:",
                                "  access_key: [REDACTED]",
                                "  signingKey: [REDACTED]",
                                "  privatekey: [REDACTED]",
                                "  accesskey: [REDACTED]",
                                "azure:",
                                "  primary-key: [REDACTED]"));
        assertThat(SecretRedactor.redact("application.properties", properties))
                .isEqualTo(
                        String.join(
                                "\n",
                                "cloud.aws.access-key=[REDACTED]",
                                "encryption.key=[REDACTED]",
                                "MASTER_KEY=[REDACTED]"));
    }

    @Test
    void 이슈16_대문자_약어_뒤의_Key도_가린다() {
        String yml = "JWTKey: a1-value\nhmac:\n  HMACKey: a2-value";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo("JWTKey: [REDACTED]\nhmac:\n  HMACKey: [REDACTED]");
    }

    @Test
    void 이슈16_key_규칙으로만_비밀인_짧은_값은_그_줄만_가린다() {
        String yml =
                String.join(
                        "\n",
                        "sort:",
                        "  key: id",
                        "id: 5",
                        "user-id: 3",
                        "# id comment",
                        "selector:",
                        "  - key: app",
                        "labels:",
                        "  app: web");

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo(
                        String.join(
                                "\n",
                                "sort:",
                                "  key: [REDACTED]",
                                "id: 5",
                                "user-id: 3",
                                "# id comment",
                                "selector:",
                                "  - key: [REDACTED]",
                                "labels:",
                                "  app: web"));
    }

    @Test
    void 이슈16_다음_줄_값과_하위_매핑의_짧은_값도_원래_자리에서_가린다() {
        String yml =
                String.join(
                        "\n",
                        "private-key:",
                        "  abc1234",
                        "jwt:",
                        "  key:",
                        "    value: abc1234",
                        "    list:",
                        "      - \"s1 s2\"",
                        "signingKey:",
                        "  ${SIGNING:dflt}",
                        "other: abc1234");

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo(
                        String.join(
                                "\n",
                                "private-key:",
                                "  [REDACTED]",
                                "jwt:",
                                "  key:",
                                "    value: [REDACTED]",
                                "    list:",
                                "      - \"[REDACTED] [REDACTED]\"",
                                "signingKey:",
                                "  ${SIGNING:[REDACTED]}",
                                "other: abc1234"));
    }

    @Test
    void 이슈16_properties_이스케이프나_줄_이음_키의_짧은_값이_남으면_파일_전체를_가린다() {
        assertThat(SecretRedactor.redact("application.properties", "private\\u002dkey=abc1234"))
                .isEqualTo("[REDACTED]");
        // 줄 이음: 1차가 다음 줄을 가리든 파일 전체를 가리든 값은 남지 않는다
        assertThat(SecretRedactor.redact("application.properties", "private\\\n  -key=abc1234"))
                .doesNotContain("abc1234");
        assertThat(
                        SecretRedactor.redact(
                                "application.properties", "priv\\\n  ate-k\\\n  ey=abc1234"))
                .doesNotContain("abc1234");
    }

    @Test
    void 이슈16_BMP_밖_문자가_앞에_있어도_짧은_값의_자리를_맞춘다() {
        String yml = "emoji: 😀😀\nprivate-key:\n  abc1234\nother: abc1234";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo("emoji: 😀😀\nprivate-key:\n  [REDACTED]\nother: abc1234");
    }

    @Test
    void 이슈16_짧은_값이_아주_많아도_선형_시간에_끝나고_상한을_넘으면_파일_전체를_가린다() {
        String yml =
                "key:\n"
                        + IntStream.range(0, 40_000)
                                .mapToObj(i -> "  - abc")
                                .collect(Collectors.joining("\n"));

        String redacted =
                assertTimeoutPreemptively(
                        Duration.ofSeconds(3), () -> SecretRedactor.redact("application.yml", yml));

        assertThat(redacted).isEqualTo("[REDACTED]");
    }

    @Test
    void 이슈16_key_규칙으로_비밀인_8자_이상_값은_본문_전체에서_가린다() {
        String yml =
                String.join(
                        "\n",
                        "\"private-key\": \"long-key-1\"",
                        "jwt:",
                        "  key:",
                        "    value: long-key-2",
                        "copy: long-key-1 long-key-2");

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo(
                        String.join(
                                "\n",
                                "\"private-key\": [REDACTED]",
                                "jwt:",
                                "  key:",
                                "    value: [REDACTED]",
                                "copy: [REDACTED] [REDACTED]"));
    }

    @Test
    void 이슈16_비밀_키워드의_짧은_값은_지금처럼_본문_전체에서_가린다() {
        String yml = "password: pw1\nurl: db?user=pw1";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo("password: [REDACTED]\nurl: db?user=[REDACTED]");
    }

    @Test
    void 이슈16_예외_이름_아래_하위_매핑은_가리지_않는다() {
        String yml = "cache-key:\n  value: plain-value-1\nother: plain-value-1";

        assertThat(SecretRedactor.redact("application.yml", yml)).isEqualTo(yml);
    }

    @Test
    void 이슈16_properties_공백_구분_key_항목을_가린다() {
        String properties = "private-key long-key-3\ncopy=long-key-3";

        assertThat(SecretRedactor.redact("application.properties", properties))
                .isEqualTo("private-key [REDACTED]\ncopy=[REDACTED]");
    }

    @Test
    void 이슈16_해석할_수_없는_yml에서도_따옴표_키의_key_규칙을_적용한다() {
        String yml = "\"private-key\": q1-value\n'key': q2-value\nbroken: [unclosed";

        assertThat(SecretRedactor.redact("application.yml", yml))
                .isEqualTo("\"private-key\": [REDACTED]\n'key': [REDACTED]\nbroken: [unclosed");
    }

    @Test
    void 이슈16_비밀이_아닌_key_설정과_key로_끝나는_일반_단어는_그대로_둔다() {
        String yml =
                String.join(
                        "\n",
                        "cache:",
                        "  cache-key: users",
                        "  key-prefix: app",
                        "  keys: a",
                        "rabbit:",
                        "  routing-key: orders",
                        "  partitionKey: id",
                        "  sort_key: created",
                        "db:",
                        "  foreign-key: user_id",
                        "ssh:",
                        "  public-key: ssh-ed25519 AAAA",
                        "zoo:",
                        "  monkey: george",
                        "  hockey: puck");
        String properties = "spring.cache.redis.key-prefix=app\nkafka.routing.key.cache-key=x";

        assertThat(SecretRedactor.redact("application.yml", yml)).isEqualTo(yml);
        assertThat(SecretRedactor.redact("application.properties", properties))
                .isEqualTo(properties);
    }

    @Test
    void AC6_설정_파일이_아니면_키_이름_규칙은_적용하지_않는다() {
        String code = "String password = \"hunter2\";";

        assertThat(SecretRedactor.redact("src/A.java", code)).isEqualTo(code);
    }
}
