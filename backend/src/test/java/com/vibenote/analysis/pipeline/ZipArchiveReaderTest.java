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

/** ZIP 읽기·필터 규칙 (F-05a 5.2절 AC5·AC9, F-26a 5.2절 AC1~AC3). 테스트 ZIP은 코드로 만든다 (eval 데이터 금지). */
class ZipArchiveReaderTest {

    @TempDir Path dir;

    private final ZipArchiveReader reader = new ZipArchiveReader(props(200, 10_000, 1_000));

    private static AnalysisProperties props(
            int maxIndexFiles, long maxIndexBytes, long maxFileBytes) {
        return new AnalysisProperties(
                5,
                maxIndexFiles,
                DataSize.ofBytes(maxIndexBytes),
                DataSize.ofBytes(maxFileBytes),
                30);
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

    private static List<String> paths(IndexResult result) {
        return paths(result.files());
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

        List<SourceFile> files = reader.read(zip).files();

        // F-26a AC1부터 README.md·app.ts도 인덱스에 들어간다
        assertThat(paths(files))
                .containsExactlyInAnyOrder(
                        "src/main/java/A.java", "src/App.vue", "README.md", "app.ts");
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
                                "p/c.ts", "x",
                                "p/D.tsx", "x",
                                "p/E.jsx", "x",
                                "p/r.md", "x",
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
                        "package.json",
                        "c.ts",
                        "D.tsx",
                        "E.jsx",
                        "r.md");
    }

    @Test
    void AC7_1MB를_넘는_단일_파일은_건너뛴다() throws IOException {
        Path zip = zip(entries("p/Big.java", "x".repeat(1_001), "p/Ok.java", "x".repeat(1_000)));

        assertThat(paths(reader.read(zip))).containsExactly("Ok.java");
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

        List<SourceFile> files = reader.read(zip).files();

        assertThat(paths(files)).containsExactly("소스/A.java");
        assertThat(files.get(0).content()).isEqualTo("class A {}");
    }

    @Test
    void AC5_DB_경로_컬럼보다_긴_경로는_건너뛴다() throws IOException {
        String longPath = "p/" + "d/".repeat(250) + "Long.java";
        Path zip = zip(entries("p/A.java", "a", longPath, "b"));

        assertThat(paths(reader.read(zip))).containsExactly("A.java");
    }

    /** tampered 엔트리는 CRC가 내용과 달라 읽으면 ANALYSIS_400이다. 읽지 않고 버렸는지 확인하는 데 쓴다. */
    private Path zipWithTampered(Map<String, String> entries, String tampered) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                byte[] content = e.getValue().getBytes(StandardCharsets.UTF_8);
                ZipEntry entry = new ZipEntry(e.getKey());
                if (e.getKey().equals(tampered)) {
                    CRC32 crc = new CRC32();
                    crc.update(content);
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(content.length);
                    entry.setCompressedSize(content.length);
                    entry.setCrc(crc.getValue());
                }
                out.putNextEntry(entry);
                out.write(content);
                out.closeEntry();
            }
        }
        byte[] raw = bytes.toByteArray();
        raw[new String(raw, StandardCharsets.ISO_8859_1).indexOf("TAMPER-ME")] = (byte) 'X';
        Path file = Files.createTempFile(dir, "t", ".zip");
        Files.write(file, raw);
        return file;
    }

    @Test
    void AC1_ts_tsx_jsx는_js_md는_doc_테스트는_test_마이그레이션은_migration_layer로_인덱스에_들어간다()
            throws IOException {
        Path zip =
                zip(
                        entries(
                                "p/src/a.ts", "x",
                                "p/src/B.tsx", "x",
                                "p/src/C.jsx", "x",
                                "p/README.md", "x",
                                "p/docs/x.md", "x",
                                "p/src/test/java/FooTest.java", "x",
                                "p/src/foo.spec.js", "x",
                                "p/db/migration/V1__init.sql", "x",
                                "p/app.min.js", "x",
                                "p/lib.min.mjs", "x",
                                "p/app.bundle.js", "x",
                                "p/types.d.ts", "x",
                                "p/generated/X.java", "x",
                                "p/src/__generated__/Y.js", "x",
                                "p/vendor/lib.js", "x",
                                "p/node_modules/z/index.ts", "x",
                                "p/build/Z.tsx", "x",
                                "p/.cache/w.md", "x",
                                "p/.env", "K=1",
                                "p/server.pem", "x"));

        Map<String, Layer> layers = new LinkedHashMap<>();
        for (SourceFile f : reader.read(zip).files()) {
            layers.put(f.path(), FileSelector.layer(f.path(), f.content()));
        }

        assertThat(layers)
                .containsOnly(
                        Map.entry("src/a.ts", Layer.JS),
                        Map.entry("src/B.tsx", Layer.JS),
                        Map.entry("src/C.jsx", Layer.JS),
                        Map.entry("README.md", Layer.DOC),
                        Map.entry("docs/x.md", Layer.DOC),
                        Map.entry("src/test/java/FooTest.java", Layer.TEST),
                        Map.entry("src/foo.spec.js", Layer.TEST),
                        Map.entry("db/migration/V1__init.sql", Layer.MIGRATION));
    }

    @Test
    void AC2_개수_상한에서_코드_test_migration_doc_순서로_담고_나머지는_버린_개수로_센다() throws IOException {
        ZipArchiveReader small = new ZipArchiveReader(props(5, 1_000_000, 1_000));
        Path zip =
                zip(
                        entries(
                                "p/README.md", "x",
                                "p/a/Z.java", "x",
                                "p/src/test/java/ATest.java", "x",
                                "p/docs/guide.md", "x",
                                "p/db/migration/V1__a.sql", "x",
                                "p/b/B.vue", "x",
                                "p/a/A.java", "x"));

        IndexResult result = small.read(zip);

        assertThat(paths(result))
                .containsExactly(
                        "a/A.java",
                        "a/Z.java",
                        "b/B.vue",
                        "db/migration/V1__a.sql",
                        "src/test/java/ATest.java");
        assertThat(result.dropped()).isEqualTo(2);
    }

    @Test
    void AC2_파일이_2001개여도_413이_아니고_상한까지_담는다() throws IOException {
        ZipArchiveReader reader2000 = new ZipArchiveReader(props(2_000, 1_000_000, 1_000));
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < 2_001; i++) {
            map.put(String.format("p/F%04d.java", i), "x");
        }

        IndexResult result = reader2000.read(zip(map));

        assertThat(result.files()).hasSize(2_000);
        assertThat(result.files().get(1_999).path()).isEqualTo("F1999.java");
        assertThat(result.dropped()).isEqualTo(1);
    }

    @Test
    void AC2_바이트_상한에_처음_걸린_파일과_뒤의_파일은_읽지_않고_버린다() throws IOException {
        Map<String, String> map =
                entries(
                        "p/D.md", "TAMPER-ME",
                        "p/C.java", "c".repeat(1_000),
                        "p/0Big.java", "x".repeat(1_001),
                        "p/A.java", "a".repeat(1_000),
                        "p/B.java", "b".repeat(1_000));
        Path zip = zipWithTampered(map, "p/D.md");
        // 내용이 바뀐 D.md를 읽으면 CRC 불일치로 실패한다
        assertThatThrownBy(() -> new ZipArchiveReader(props(200, 1_000_000, 1_000)).read(zip))
                .isInstanceOf(AnalysisFailedException.class);

        IndexResult result = new ZipArchiveReader(props(200, 2_500, 1_000)).read(zip);

        assertThat(paths(result)).containsExactly("A.java", "B.java");
        // 0Big.java(단일 파일 상한 초과) + C.java(합계 초과) + D.md(읽지 않음)
        assertThat(result.dropped()).isEqualTo(3);
    }

    @Test
    void AC3_코드_파일_없이_md_yml만_있으면_ANALYSIS_415() throws IOException {
        assertFails(
                zip(entries("p/README.md", "# a", "p/application.yml", "a: b", "p/docs/x.md", "x")),
                ErrorCode.ANALYSIS_415,
                "분석할 수 있는 코드가 없습니다");
    }
}
