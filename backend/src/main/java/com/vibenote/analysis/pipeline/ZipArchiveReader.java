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
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.springframework.stereotype.Component;

/**
 * 업로드 ZIP에서 지원 파일만 메모리로 읽는다 (F-05a 5절·5.2절).
 *
 * <p>압축을 디스크에 풀지 않으므로 Zip Slip으로 파일이 생길 곳이 없다. 그래도 루트 밖·절대 경로 엔트리가 있으면 조작된 ZIP으로 보고 거부한다. 엔트리 크기
 * 헤더는 믿지 않고 실제로 읽은 바이트로 단일 파일·합계 한도를 센다 (zip bomb 방어).
 */
@Component
public class ZipArchiveReader {

    static final String TOO_MANY_FILES = "지원하는 코드 파일이 %d개입니다. %d개 이하로 줄여서 다시 올려 주세요.";
    static final String TOO_LARGE = "코드 파일 합계가 %dMB를 넘습니다. 크기를 줄여서 다시 올려 주세요.";

    /**
     * Windows 기본 압축 도구는 파일명을 UTF-8 표시 없이 MS949로 쓴다. UTF-8로 못 읽으면 이것으로 다시 읽는다. JDK는 UTF-8로 풀 수 없는
     * 이름을 ZIP을 열 때 {@code ZipException("invalid CEN header (bad entry name)")}으로 알린다.
     */
    private static final Charset FALLBACK_CHARSET = Charset.forName("MS949");

    /** {@code analysis_files.path VARCHAR(500)}. 더 긴 경로는 저장할 수 없으므로 건너뛴다. */
    static final int MAX_PATH_LENGTH = 500;

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
                    "coverage");
    private static final Set<String> SECRET_EXTENSIONS =
            Set.of("pem", "key", "p12", "pfx", "jks", "keystore");
    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("java", "xml", "js", "mjs", "vue", "yml", "yaml", "properties", "sql");
    private static final Set<String> SUPPORTED_NAMES =
            Set.of("build.gradle", "build.gradle.kts", "package.json");
    private static final Set<String> CODE_EXTENSIONS = Set.of("java", "js", "mjs", "vue");

    private final AnalysisProperties properties;

    public ZipArchiveReader(AnalysisProperties properties) {
        this.properties = properties;
    }

    /**
     * 필터를 통과한 지원 파일을 ZIP 안 순서대로 돌려준다. 단일 파일 한도를 넘는 파일은 건너뛴다.
     *
     * @throws AnalysisFailedException 깨진 ZIP·루트 밖 경로(ANALYSIS_400), 파일 수·합계 초과(ANALYSIS_413), 지원 스택
     *     코드 없음(ANALYSIS_415)
     */
    public List<SourceFile> read(Path zip) {
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
    private List<SourceFile> read(Path zip, Charset charset) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile(), charset)) {
            List<Candidate> candidates = supportedEntries(zipFile);
            if (candidates.size() > properties.maxFiles()) {
                throw new AnalysisFailedException(
                        ErrorCode.ANALYSIS_413,
                        TOO_MANY_FILES.formatted(candidates.size(), properties.maxFiles()));
            }

            long maxFileBytes = properties.maxFileBytes().toBytes();
            long maxTotalBytes = properties.maxTotalBytes().toBytes();
            long total = 0;
            List<SourceFile> files = new ArrayList<>();
            for (Candidate candidate : candidates) {
                byte[] bytes = readAtMost(zipFile, candidate.entry(), maxFileBytes);
                if (bytes == null) {
                    continue;
                }
                total += bytes.length;
                if (total > maxTotalBytes) {
                    throw new AnalysisFailedException(
                            ErrorCode.ANALYSIS_413,
                            TOO_LARGE.formatted(properties.maxTotalBytes().toMegabytes()));
                }
                files.add(
                        new SourceFile(
                                candidate.path(), new String(bytes, StandardCharsets.UTF_8)));
            }

            if (files.stream().noneMatch(f -> CODE_EXTENSIONS.contains(extension(f.path())))) {
                throw new AnalysisFailedException(ErrorCode.ANALYSIS_415);
            }
            return files;
        }
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
            all.add(new Candidate(entry, path));
        }

        String prefix = commonTopFolder(all);
        List<Candidate> supported = new ArrayList<>();
        for (Candidate candidate : all) {
            String path = candidate.path().substring(prefix.length());
            if (path.length() <= MAX_PATH_LENGTH && isSupported(path)) {
                supported.add(new Candidate(candidate.entry(), path));
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
        if (secret) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(extension) || SUPPORTED_NAMES.contains(name);
    }

    private static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 최대 limit 바이트까지 읽는다. 더 있으면 null (건너뛸 파일). */
    private static byte[] readAtMost(ZipFile zipFile, ZipEntry entry, long limit)
            throws IOException {
        try (InputStream in = zipFile.getInputStream(entry)) {
            byte[] bytes = in.readNBytes(Math.toIntExact(limit));
            return in.read() == -1 ? bytes : null;
        }
    }

    private record Candidate(ZipEntry entry, String path) {}
}
