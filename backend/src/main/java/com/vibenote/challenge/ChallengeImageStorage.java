package com.vibenote.challenge;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 챌린지 대표 이미지 {@code {app.upload-dir}/challenges/{UUID}.{ext}} (F-24 AC3). 확장자·MIME·매직 바이트가 모두 맞는
 * jpg·png·webp만, 2MB 이하만 받는다. 파일 이름은 UUID로 정하므로 업로드 파일명으로 경로를 조작할 수 없고, 공개 주소 {@code
 * /uploads/challenges/{이름}}은 {@code WebConfig}가 이 폴더만 제공한다.
 */
@Component
public class ChallengeImageStorage {

    public static final String URL_PREFIX = "/uploads/challenges/";
    public static final long MAX_BYTES = 2L * 1024 * 1024;

    static final String EMPTY = "빈 이미지 파일은 올릴 수 없습니다.";
    static final String TOO_LARGE = "이미지는 2MB 이하만 올릴 수 있습니다.";
    static final String NOT_ALLOWED = "jpg·png·webp 이미지만 올릴 수 있습니다.";

    private static final Logger log = LoggerFactory.getLogger(ChallengeImageStorage.class);

    /** 허용 형식. 확장자는 저장할 때 첫 번째 값으로 정규화한다 (.jpeg → .jpg). */
    private enum ImageType {
        JPG("image/jpeg", new String[] {"jpg", "jpeg"}) {
            @Override
            boolean matches(byte[] head) {
                return startsWith(head, 0, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            }
        },
        PNG("image/png", new String[] {"png"}) {
            @Override
            boolean matches(byte[] head) {
                return startsWith(
                        head,
                        0,
                        new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            }
        },
        WEBP("image/webp", new String[] {"webp"}) {
            @Override
            boolean matches(byte[] head) {
                return startsWith(head, 0, new byte[] {'R', 'I', 'F', 'F'})
                        && startsWith(head, 8, new byte[] {'W', 'E', 'B', 'P'});
            }
        };

        static final int HEAD_BYTES = 12;

        final String mime;
        final String[] extensions;

        ImageType(String mime, String[] extensions) {
            this.mime = mime;
            this.extensions = extensions;
        }

        abstract boolean matches(byte[] head);

        String extension() {
            return extensions[0];
        }

        static ImageType byExtension(String ext) {
            return Arrays.stream(values())
                    .filter(t -> Arrays.asList(t.extensions).contains(ext))
                    .findFirst()
                    .orElse(null);
        }

        private static boolean startsWith(byte[] head, int offset, byte[] prefix) {
            if (head.length < offset + prefix.length) return false;
            return Arrays.equals(head, offset, offset + prefix.length, prefix, 0, prefix.length);
        }
    }

    private final Path dir;

    public ChallengeImageStorage(@Value("${app.upload-dir}") String uploadDir) {
        this.dir = Path.of(uploadDir, "challenges").toAbsolutePath().normalize();
    }

    /** 규칙에 맞지 않는 이유. 맞으면 null. */
    public String problem(MultipartFile file) {
        if (file.isEmpty()) return EMPTY;
        if (file.getSize() > MAX_BYTES) return TOO_LARGE;
        return typeOf(file) == null ? NOT_ALLOWED : null;
    }

    /**
     * {@link #problem}을 통과한 파일을 새 UUID 이름으로 쓰고 공개 주소를 돌려준다. 실패하면 쓰다 만 파일을 지우고 예외를 던진다 (COMMON_500).
     */
    public String save(MultipartFile file) {
        ImageType type = typeOf(file);
        if (type == null) {
            throw new IllegalArgumentException("검사하지 않은 이미지: " + file.getOriginalFilename());
        }
        String name = UUID.randomUUID() + "." + type.extension();
        Path target = dir.resolve(name);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(dir);
            Files.copy(in, target);
        } catch (IOException e) {
            deleteQuietly(target);
            throw new UncheckedIOException("챌린지 이미지 저장 실패: " + target, e);
        }
        return URL_PREFIX + name;
    }

    /**
     * 공개 주소의 파일을 지운다. 이름만 꺼내 이 폴더 아래로 다시 정규화해, 폴더 밖을 가리키는 값은 무시한다. 지우지 못해도 응답에는 영향이 없으므로 경고만 남긴다.
     */
    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) return;
        String name = imageUrl.substring(URL_PREFIX.length());
        if (name.isEmpty() || name.contains("/") || name.contains("\\")) return;
        Path target = dir.resolve(name).normalize();
        if (!dir.equals(target.getParent())) return;
        deleteQuietly(target);
    }

    private static void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("챌린지 이미지 삭제 실패: {}", target, e);
        }
    }

    private static ImageType typeOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        int dot = name == null ? -1 : name.lastIndexOf('.');
        if (dot < 0) return null;
        ImageType type = ImageType.byExtension(name.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (type == null || !type.mime.equals(mimeOf(file))) return null;
        try (InputStream in = file.getInputStream()) {
            return type.matches(in.readNBytes(ImageType.HEAD_BYTES)) ? type : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** {@code image/png; charset=...}처럼 매개변수가 붙어도 형식만 비교한다. */
    private static String mimeOf(MultipartFile file) {
        String type = file.getContentType();
        if (type == null) return null;
        int semicolon = type.indexOf(';');
        return (semicolon < 0 ? type : type.substring(0, semicolon))
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}
