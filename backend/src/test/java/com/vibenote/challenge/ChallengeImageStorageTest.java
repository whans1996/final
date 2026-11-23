package com.vibenote.challenge;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

/** F-24 AC3 챌린지 이미지 검사·저장·삭제. */
class ChallengeImageStorageTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 7};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB, 7};
    static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    static final byte[] GIF = "GIF89a".getBytes(StandardCharsets.ISO_8859_1);
    static final int TWO_MB = 2 * 1024 * 1024;

    @TempDir Path uploadDir;

    ChallengeImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new ChallengeImageStorage(uploadDir.toString());
    }

    static MockMultipartFile file(String name, String type, byte[] bytes) {
        return new MockMultipartFile("image", name, type, bytes);
    }

    static Stream<Arguments> validImages() {
        return Stream.of(
                Arguments.of(file("a.png", "image/png", PNG), ".png"),
                Arguments.of(file("A.PNG", "image/png", PNG), ".png"),
                Arguments.of(file("a.jpg", "image/jpeg", JPEG), ".jpg"),
                Arguments.of(file("a.jpeg", "image/jpeg", JPEG), ".jpg"),
                Arguments.of(file("a.webp", "image/webp", WEBP), ".webp"),
                Arguments.of(file("a.png", "image/png", Arrays.copyOf(PNG, TWO_MB)), ".png"));
    }

    @ParameterizedTest
    @MethodSource("validImages")
    void AC3_허용된_이미지는_UUID_이름과_정규화한_확장자로_저장한다(MockMultipartFile image, String ext)
            throws Exception {
        assertThat(storage.problem(image)).isNull();

        String url = storage.save(image);

        assertThat(url)
                .matches(
                        "/uploads/challenges/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                                + ext.replace(".", "\\."));
        Path saved =
                uploadDir.resolve("challenges").resolve(url.substring(url.lastIndexOf('/') + 1));
        assertThat(saved).exists().hasBinaryContent(image.getBytes());
    }

    static Stream<Arguments> invalidImages() {
        return Stream.of(
                Arguments.of(file("a.gif", "image/gif", GIF)),
                Arguments.of(file("a.png", "image/png", JPEG)),
                Arguments.of(file("a.jpg", "image/jpeg", PNG)),
                Arguments.of(
                        file(
                                "a.webp",
                                "image/webp",
                                "RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.ISO_8859_1))),
                Arguments.of(file("a.png", "text/plain", PNG)),
                Arguments.of(file("a.png", null, PNG)),
                Arguments.of(file("png", "image/png", PNG)),
                Arguments.of(file(null, "image/png", PNG)),
                Arguments.of(file("a.png", "image/png", new byte[0])),
                Arguments.of(file("a.png", "image/png", Arrays.copyOf(PNG, TWO_MB + 1))),
                Arguments.of(file("a.png", "image/png", new byte[] {(byte) 0x89, 0x50})));
    }

    @ParameterizedTest
    @MethodSource("invalidImages")
    void AC3_규칙에_맞지_않는_이미지는_사유를_준다(MockMultipartFile image) {
        assertThat(storage.problem(image)).isNotBlank();
    }

    @Test
    void AC6_저장한_이미지를_공개_주소로_지운다() throws Exception {
        String url = storage.save(file("a.png", "image/png", PNG));
        Path saved =
                uploadDir.resolve("challenges").resolve(url.substring(url.lastIndexOf('/') + 1));

        storage.delete(url);

        assertThat(saved).doesNotExist();
    }

    @Test
    void AC10_챌린지_폴더_밖의_파일은_지우지_않는다() throws Exception {
        Path secret = Files.writeString(uploadDir.resolve("secret.txt"), "secret");
        Files.createDirectories(uploadDir.resolve("challenges"));

        storage.delete("/uploads/challenges/../secret.txt");
        storage.delete("/uploads/challenges/..\\secret.txt");
        storage.delete("/uploads/secret.txt");
        storage.delete(null);

        assertThat(secret).exists();
    }
}
