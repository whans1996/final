package com.vibenote.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.vibenote.common.IntegrationTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * F-24 AC10 업로드 폴더 정적 제공 범위. {@code /uploads/challenges/**}만 열고, 같은 폴더의 분석 ZIP 임시 파일이나 다른 파일은 경로
 * 조작으로도 받을 수 없어야 한다.
 */
class UploadResourceTest extends IntegrationTestBase {

    static final Path UPLOAD_DIR = Path.of("build/test-uploads");
    static final String SECRET = "f24-secret-content";

    @BeforeAll
    static void createFiles() throws Exception {
        Files.createDirectories(UPLOAD_DIR.resolve("analysis-tmp"));
        Files.createDirectories(UPLOAD_DIR.resolve("challenges"));
        Files.writeString(UPLOAD_DIR.resolve("analysis-tmp/1.zip"), SECRET);
        Files.writeString(UPLOAD_DIR.resolve("secret.txt"), SECRET);
    }

    @AfterAll
    static void deleteFiles() throws Exception {
        Files.deleteIfExists(UPLOAD_DIR.resolve("analysis-tmp/1.zip"));
        Files.deleteIfExists(UPLOAD_DIR.resolve("secret.txt"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/uploads/analysis-tmp/1.zip",
                "/uploads/secret.txt",
                "/uploads/challenges/..%2fsecret.txt",
                "/uploads/challenges/%2e%2e/secret.txt",
                "/uploads/challenges/..%5csecret.txt",
                "/uploads/challenges/../secret.txt"
            })
    void AC10_챌린지_폴더_밖의_파일은_받을_수_없다(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andReturn();

        assertThat(result.getResponse().getStatus()).isIn(400, 404);
        assertThat(result.getResponse().getContentAsString()).doesNotContain(SECRET);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/uploads/challenges/없는파일.png", "/uploads/challenges/none.png"})
    void AC10_없는_챌린지_이미지는_404(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }
}
