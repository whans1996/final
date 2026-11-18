package com.vibenote.analysis.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.analysis.AnalysisProperties;
import com.vibenote.common.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

/** ZIP 읽기·필터 규칙 (F-05a 5.2절, AC7·AC9). 테스트 ZIP은 코드로 만든다 (eval 데이터 금지). */
class ZipArchiveReaderTest {

    @TempDir Path dir;

    private final ZipArchiveReader reader = new ZipArchiveReader(props(200, 10_000, 1_000));

    private static AnalysisProperties props(int maxFiles, long maxTotalBytes, long maxFileBytes) {
        return new AnalysisProperties(
                5, maxFiles, DataSize.ofBytes(maxTotalBytes), DataSize.ofBytes(maxFileBytes), 30);
    }

    private Path zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                if (e.getValue() != null) {
                    out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                }
                out.closeEntry();
            }
        }
        Path file = Files.createTempFile(dir, "a", ".zip");
        Files.write(file, bytes.toByteArray());
        return file;
    }

    private static Map<String, String> entries(String... pathAndContent) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pathAndContent.length; i += 2) {
            map.put(pathAndContent[i], pathAndContent[i + 1]);
        }
        return map;
    }

    private static List<String> paths(List<SourceFile> files) {
        return files.stream().map(SourceFile::path).toList();
    }

    private void assertFails(Path zip, ErrorCode code, String messagePart) {
        assertThatThrownBy(() -> reader.read(zip))
                .isInstanceOfSatisfying(
                        AnalysisFailedException.class,
                        e -> {
                            assertThat(e.code()).isEqualTo(code);
                            assertThat(e.getMessage()).contains(messagePart);
                        });
    }

    @Test
    void AC5_공통_최상위_폴더를_떼고_지원_파일만_읽는다() throws IOException {
        Path zip =
                zip(
                        entries(
                                "todo/", null,
                                "todo/src/main/java/A.java", "class A {}",
                                "todo/src/App.vue", "<template/>",
                                "todo/README.md", "# readme",
                                "todo/logo.png", "png",
                                "todo/app.ts", "let a = 1",
                                "__MACOSX/todo/._A.java", "junk"));

        List<SourceFile> files = reader.read(zip);

        assertThat(paths(files)).containsExactlyInAnyOrder("src/main/java/A.java", "src/App.vue");
        assertThat(files)
                .filteredOn(f -> f.path().equals("src/main/java/A.java"))
                .extracting(SourceFile::content)
                .containsExactly("class A {}");
    }

    @Test
    void AC5_최상위_폴더가_여러_개면_경로를_그대로_둔다() throws IOException {
        Path zip = zip(entries("be/A.java", "a", "fe/main.js", "b"));

        assertThat(paths(reader.read(zip))).containsExactlyInAnyOrder("be/A.java", "fe/main.js");
    }

    @Test
    void AC5_역슬래시_경로를_슬래시로_바꾼다() throws IOException {
        Path zip = zip(entries("todo\\src\\A.java", "a", "todo\\src\\B.java", "b"));

        assertThat(paths(reader.read(zip))).containsExactlyInAnyOrder("src/A.java", "src/B.java");
    }

    @Test
    void AC5_제외_폴더와_비밀_파일은_읽지_않는다() throws IOException {
        Path zip =
                zip(
                        entries(
                                "p/src/A.java", "a",
                                "p/node_modules/lib/index.js", "x",
                                "p/build/classes/B.java", "x",
                                "p/dist/app.js", "x",
                                "p/target/C.java", "x",
                                "p/out/D.java", "x",
                                "p/bin/E.java", "x",
                                "p/.git/config.xml", "x",
                                "p/.gradle/F.java", "x",
                                "p/.idea/workspace.xml", "x",
                                "p/.vscode/settings.js", "x",
                                "p/.mvn/wrapper.properties", "x",
                                "p/coverage/lcov.js", "x",
                                "p/.hidden/G.java", "x",
                                "p/.env", "KEY=1",
                                "p/.env.local", "KEY=1",
                                "p/server.pem", "x",
                                "p/server.key", "x",
                                "p/store.p12", "x",
                                "p/store.pfx", "x",
                                "p/store.jks", "x",
                                "p/my.keystore", "x",
                                "p/id_rsa", "x",
                                "p/id_ed25519.pub", "x"));

        assertThat(paths(reader.read(zip))).containsExactly("src/A.java");
    }

    @Test
    void AC5_지원_확장자와_파일명() throws IOException {
        Path zip =
                zip(
                        entries(
                                "p/A.java", "x",
                                "p/m.xml", "x",
                                "p/a.js", "x",
                                "p/b.mjs", "x",
                                "p/C.vue", "x",
                                "p/a.yml", "x",
                                "p/b.yaml", "x",
                                "p/c.properties", "x",
                                "p/d.sql", "x",
                                "p/build.gradle", "x",
                                "p/build.gradle.kts", "x",
                                "p/package.json", "x",
                                "p/other.json", "x",
                                "p/x.py", "x",
                                "p/X.class", "x",
                                "p/lib.jar", "x"));

        assertThat(paths(reader.read(zip)))
                .containsExactlyInAnyOrder(
                        "A.java",
                        "m.xml",
                        "a.js",
                        "b.mjs",
                        "C.vue",
                        "a.yml",
                        "b.yaml",
                        "c.properties",
                        "d.sql",
                        "build.gradle",
                        "build.gradle.kts",
                        "package.json");
    }

    @Test
    void AC7_1MB를_넘는_단일_파일은_건너뛴다() throws IOException {
        Path zip = zip(entries("p/Big.java", "x".repeat(1_001), "p/Ok.java", "x".repeat(1_000)));

        assertThat(paths(reader.read(zip))).containsExactly("Ok.java");
    }

    @Test
    void AC7_지원_파일이_한도를_넘으면_ANALYSIS_413() throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < 201; i++) {
            map.put("p/F" + i + ".java", "x");
        }
        map.put("p/readme.md", "x");

        assertFails(zip(map), ErrorCode.ANALYSIS_413, "지원하는 코드 파일이 201개입니다");
    }

    @Test
    void AC7_지원_파일이_한도와_같으면_통과한다() throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < 200; i++) {
            map.put("p/F" + i + ".java", "x");
        }

        assertThat(reader.read(zip(map))).hasSize(200);
    }

    @Test
    void AC7_읽은_합계가_한도를_넘으면_ANALYSIS_413() throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < 11; i++) {
            map.put("p/F" + i + ".java", "x".repeat(1_000));
        }

        // 문구의 한도 표기(10MB)는 AnalysisPipelineTest가 기본 설정으로 검증한다
        assertFails(zip(map), ErrorCode.ANALYSIS_413, "코드 파일 합계가");
    }

    @Test
    void AC8_지원_스택_코드가_없으면_ANALYSIS_415() throws IOException {
        assertFails(
                zip(entries("p/app.py", "x", "p/application.yml", "a: b", "p/m.xml", "x")),
                ErrorCode.ANALYSIS_415,
                "분석할 수 있는 코드가 없습니다");
    }

    @Test
    void AC9_루트_밖_경로는_ANALYSIS_400() throws IOException {
        assertFails(
                zip(entries("p/A.java", "a", "../../evil.java", "x")),
                ErrorCode.ANALYSIS_400,
                "ZIP 파일을 읽을 수 없습니다");
        assertFails(
                zip(entries("p/A.java", "a", "p/../../evil.java", "x")),
                ErrorCode.ANALYSIS_400,
                "ZIP 파일을 읽을 수 없습니다");
    }

    @Test
    void AC9_루트_안에서_끝나는_상위_경로는_정규화한다() throws IOException {
        Path zip = zip(entries("p/src/../A.java", "a", "p/B.java", "b"));

        assertThat(paths(reader.read(zip))).containsExactlyInAnyOrder("A.java", "B.java");
    }

    @Test
    void AC9_절대_경로는_ANALYSIS_400() throws IOException {
        assertFails(
                zip(entries("p/A.java", "a", "/etc/passwd", "x")),
                ErrorCode.ANALYSIS_400,
                "ZIP 파일을 읽을 수 없습니다");
        assertFails(
                zip(entries("p/A.java", "a", "C:\\x.java", "x")),
                ErrorCode.ANALYSIS_400,
                "ZIP 파일을 읽을 수 없습니다");
    }

    @Test
    void AC9_손상된_ZIP은_ANALYSIS_400() throws IOException {
        Path broken = dir.resolve("broken.zip");
        Files.write(broken, new byte[] {'P', 'K', 3, 4, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13});

        assertFails(broken, ErrorCode.ANALYSIS_400, "ZIP 파일을 읽을 수 없습니다");
    }

    @Test
    void AC9_헤더는_멀쩡하고_내용만_바뀐_ZIP은_CRC_불일치로_ANALYSIS_400() throws IOException {
        byte[] content = "class A { int n = 1; }".getBytes(StandardCharsets.UTF_8);
        CRC32 crc = new CRC32();
        crc.update(content);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            ZipEntry entry = new ZipEntry("p/A.java");
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(content.length);
            entry.setCompressedSize(content.length);
            entry.setCrc(crc.getValue());
            out.putNextEntry(entry);
            out.write(content);
            out.closeEntry();
        }
        byte[] raw = bytes.toByteArray();
        String latin1 = new String(raw, StandardCharsets.ISO_8859_1);
        int at = latin1.indexOf("int n = 1") + "int n = ".length();
        raw[at] = '9';
        Path tampered = dir.resolve("tampered.zip");
        Files.write(tampered, raw);

        assertFails(tampered, ErrorCode.ANALYSIS_400, "ZIP 파일을 읽을 수 없습니다");
    }

    @Test
    void AC5_Windows_압축_도구가_MS949로_쓴_한글_파일명을_읽는다() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes, Charset.forName("MS949"))) {
            out.putNextEntry(new ZipEntry("할일앱/소스/A.java"));
            out.write("class A {}".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        Path zip = dir.resolve("ms949.zip");
        Files.write(zip, bytes.toByteArray());

        List<SourceFile> files = reader.read(zip);

        assertThat(paths(files)).containsExactly("소스/A.java");
        assertThat(files.get(0).content()).isEqualTo("class A {}");
    }

    @Test
    void AC5_DB_경로_컬럼보다_긴_경로는_건너뛴다() throws IOException {
        String longPath = "p/" + "d/".repeat(250) + "Long.java";
        Path zip = zip(entries("p/A.java", "a", longPath, "b"));

        assertThat(paths(reader.read(zip))).containsExactly("A.java");
    }
}
