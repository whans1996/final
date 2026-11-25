package com.vibenote.analysis.pipeline;

import com.vibenote.analysis.AnalysisProperties;
import com.vibenote.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.springframework.stereotype.Component;

/**
 * 업로드 ZIP에서 인덱스에 담을 파일을 메모리로 읽는다 (F-05a 5절·5.2절, F-26a 5.2절).
 *
 * <p>압축을 디스크에 풀지 않으므로 Zip Slip으로 파일이 생길 곳이 없다. 그래도 루트 밖·절대 경로 엔트리가 있으면 조작된 ZIP으로 보고 거부한다. 엔트리 크기
 * 헤더는 믿지 않고 실제로 읽은 바이트로 단일 파일·합계 상한을 센다. 상한에 걸리면 실패시키지 않고 거기서 읽기를 멈추므로, 읽는 양은 합계 상한 + 파일 하나를 넘지 않는다
 * (zip bomb 방어).
 */
@Component
public class ZipArchiveReader {

    /**
     * Windows 기본 압축 도구는 파일명을 UTF-8 표시 없이 MS949로 쓴다. UTF-8로 못 읽으면 이것으로 다시 읽는다. JDK는 UTF-8로 풀 수 없는
     * 이름을 ZIP을 열 때 {@code ZipException("invalid CEN header (bad entry name)")}으로 알린다.
     */
    private static final Charset FALLBACK_CHARSET = Charset.forName("MS949");

    /** {@code analysis_files.path VARCHAR(500)}. 더 긴 경로는 저장할 수 없으므로 건너뛴다. */
    static final int MAX_PATH_LENGTH = 500;

    private static final String ROOT_README = "README.md";

    private static final Pattern DRIVE = Pattern.compile("^[A-Za-z]:");
    private static final String MAC_METADATA = "__MACOSX";
    private static final Set<String> EXCLUDED_DIRS =
            Set.of(
                    "node_modules",
                    "build",
                    "dist",
                    "target",
                    "out",
                    "bin",
                    ".git",
                    ".gradle",
                    ".idea",
                    ".vscode",
                    ".mvn",
                    "coverage",
                    "generated",
                    "__generated__",
                    "vendor");

    /** 생성물 (번들·압축본·타입 선언). 사람이 쓴 코드가 아니라 인덱스에 담지 않는다. */
    private static final Pattern GENERATED_NAME =
            Pattern.compile(".+(?:\\.min\\.m?js|\\.bundle\\.js|\\.d\\.ts)");

    private static final Set<String> SECRET_EXTENSIONS =
            Set.of("pem", "key", "p12", "pfx", "jks", "keystore");
    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of(
                    "java",
                    "xml",
                    "js",
                    "mjs",
                    "vue",
                    "ts",
                    "tsx",
                    "jsx",
                    "yml",
                    "yaml",
                    "properties",
                    "sql",
                    "md");
    private static final Set<String> SUPPORTED_NAMES =
            Set.of("build.gradle", "build.gradle.kts", "package.json");
    private static final Set<String> CODE_EXTENSIONS =
            Set.of("java", "js", "mjs", "vue", "ts", "tsx", "jsx");

    private final AnalysisProperties properties;

    public ZipArchiveReader(AnalysisProperties properties) {
        this.properties = properties;
    }

    /**
     * 필터를 통과한 파일을 (묶음, 경로) 순서로 상한까지 읽는다. 묶음은 루트 README → 코드·설정 → test·migration → doc 순이라, 상한에 걸려도
     * 코드가 먼저 담긴다. 개수가 {@code max-index-files}에 이르거나 읽은 합계가 {@code max-index-bytes}를 처음 넘으면 그 파일과 뒤의
     * 파일은 버린다. 단일 파일 상한을 넘는 파일은 건너뛴다. 버리거나 건너뛴 파일은 {@link IndexResult#dropped()}로 센다.
     *
     * @throws AnalysisFailedException 깨진 ZIP·루트 밖 경로(ANALYSIS_400), 지원 스택 코드 없음(ANALYSIS_415)
     */
    public IndexResult read(Path zip) {
        try {
            return read(zip, StandardCharsets.UTF_8);
        } catch (ZipException | IllegalArgumentException utf8Failed) {
            try {
                return read(zip, FALLBACK_CHARSET);
            } catch (IOException | IllegalArgumentException e) {
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_400);
            }
        } catch (IOException e) {
            throw new AnalysisFailedException(ErrorCode.ANALYSIS_400);
        }
    }

    /** ZipException(손상·형식 오류·읽을 수 없는 이름)은 그대로 던진다. */
    private IndexResult read(Path zip, Charset charset) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile(), charset)) {
            List<Candidate> candidates = supportedEntries(zipFile);
            candidates.sort(
                    Comparator.comparingInt(Candidate::bundle).thenComparing(Candidate::path));

            long maxFileBytes = properties.maxFileBytes().toBytes();
            long maxIndexBytes = properties.maxIndexBytes().toBytes();
            long total = 0;
            int dropped = 0;
            List<SourceFile> files = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                Candidate candidate = candidates.get(i);
                if (files.size() >= properties.maxIndexFiles() || total > maxIndexBytes) {
                    dropped += candidates.size() - i;
                    break;
                }
                // 헤더가 이미 단일 파일 상한을 넘는다고 하면 읽지 않는다. 헤더가 작다고 속인 엔트리는 읽다가 걸리고,
                // 그때 읽은 양은 합계에 넣어 읽는 양이 상한을 넘어 늘지 않게 한다
                byte[] bytes =
                        candidate.entry().getSize() > maxFileBytes
                                ? null
                                : readAtMost(zipFile, candidate.entry(), maxFileBytes);
                if (bytes == null) {
                    dropped++;
                    if (candidate.entry().getSize() <= maxFileBytes) {
                        total += maxFileBytes;
                    }
                    continue;
                }
                if (total + bytes.length > maxIndexBytes) {
                    dropped += candidates.size() - i;
                    break;
                }
                total += bytes.length;
                files.add(
                        new SourceFile(
                                candidate.path(), new String(bytes, StandardCharsets.UTF_8)));
            }

            if (files.stream().noneMatch(f -> CODE_EXTENSIONS.contains(extension(f.path())))) {
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_415);
            }
            return new IndexResult(List.copyOf(files), dropped);
        }
    }

    /**
     * 읽는 순서의 묶음. 경로만으로 정한다 (F-26a 5.2절 3): 0 루트 README.md, 1 코드·설정, 2 test·migration, 3 doc. 루트
     * README는 개요 입력에 쓰므로 doc이어도 상한에 걸리기 전에 담는다.
     */
    private static int bundle(String path) {
        if (path.equalsIgnoreCase(ROOT_README)) {
            return 0;
        }
        return switch (FileSelector.layer(path, null)) {
            case DOC -> 3;
            case TEST, MIGRATION -> 2;
            default -> 1;
        };
    }

    /** 모든 엔트리 경로를 검사하고, 공통 최상위 폴더를 뗀 뒤 필터를 통과한 지원 파일만 고른다. */
    private static List<Candidate> supportedEntries(ZipFile zipFile) {
        List<Candidate> all = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String slashed = entry.getName().replace('\\', '/');
            String path = normalize(slashed);
            if (slashed.endsWith("/")
                    || path.isEmpty()
                    || path.equals(MAC_METADATA)
                    || path.startsWith(MAC_METADATA + "/")) {
                continue;
            }
            // 묶음은 공통 최상위 폴더를 뗀 경로로 정하므로 아래에서 다시 만든다
            all.add(new Candidate(entry, path, -1));
        }

        String prefix = commonTopFolder(all);
        List<Candidate> supported = new ArrayList<>();
        for (Candidate candidate : all) {
            String path = candidate.path().substring(prefix.length());
            if (path.length() <= MAX_PATH_LENGTH && isSupported(path)) {
                supported.add(new Candidate(candidate.entry(), path, bundle(path)));
            }
        }
        return supported;
    }

    /** '/' 구분 경로를 정규화한다. 절대 경로이거나 '..'로 루트를 벗어나면 ANALYSIS_400. */
    static String normalize(String slashed) {
        if (slashed.startsWith("/") || DRIVE.matcher(slashed).lookingAt()) {
            throw new AnalysisFailedException(ErrorCode.ANALYSIS_400);
        }
        Deque<String> parts = new ArrayDeque<>();
        for (String segment : slashed.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (parts.isEmpty()) {
                    throw new AnalysisFailedException(ErrorCode.ANALYSIS_400);
                }
                parts.removeLast();
            } else {
                parts.addLast(segment);
            }
        }
        return String.join("/", parts);
    }

    /** 모든 파일이 최상위 폴더 하나 아래에 있으면 "그폴더/", 아니면 "". */
    private static String commonTopFolder(List<Candidate> files) {
        String top = null;
        for (Candidate file : files) {
            int slash = file.path().indexOf('/');
            if (slash < 0) {
                return "";
            }
            String first = file.path().substring(0, slash + 1);
            if (top == null) {
                top = first;
            } else if (!top.equals(first)) {
                return "";
            }
        }
        return top == null ? "" : top;
    }

    private static boolean isSupported(String path) {
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if (EXCLUDED_DIRS.contains(segments[i]) || segments[i].startsWith(".")) {
                return false;
            }
        }
        String name = segments[segments.length - 1].toLowerCase(Locale.ROOT);
        String extension = extension(name);
        boolean secret =
                name.equals(".env")
                        || name.startsWith(".env.")
                        || SECRET_EXTENSIONS.contains(extension)
                        || name.startsWith("id_rsa")
                        || name.startsWith("id_ed25519");
        if (secret || GENERATED_NAME.matcher(name).matches()) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(extension) || SUPPORTED_NAMES.contains(name);
    }

    private static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 최대 limit 바이트까지 읽는다. 더 있으면 null (건너뛸 파일).
     *
     * <p>{@code ZipFile.getInputStream()}은 엔트리 CRC를 검사하지 않으므로, 헤더는 멀쩡하고 내용만 바뀐 ZIP도 끝까지 읽힌다. 다 읽은
     * 엔트리는 CRC를 직접 비교해 다르면 ANALYSIS_400으로 거부한다.
     */
    private static byte[] readAtMost(ZipFile zipFile, ZipEntry entry, long limit)
            throws IOException {
        try (InputStream in = zipFile.getInputStream(entry)) {
            byte[] bytes = in.readNBytes(Math.toIntExact(limit));
            if (in.read() != -1) {
                return null;
            }
            if (entry.getCrc() != -1 && crc32(bytes) != entry.getCrc()) {
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_400);
            }
            return bytes;
        }
    }

    private static long crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    /** {@code bundle}은 정렬 비교마다 layer를 다시 판정하지 않도록 한 번만 구한다. */
    private record Candidate(ZipEntry entry, String path, int bundle) {}
}
