package com.vibenote.analysis.dto;

import com.vibenote.analysis.fetch.InvalidRepositoryUrlException;
import com.vibenote.analysis.fetch.RepositoryUrl;
import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;

/**
 * 분석 요청 multipart 파트를 검증한 결과 (F-05a AC2, F-05b AC2). 문제가 있는 필드를 모두 모아 COMMON_400 {@code details}로
 * 알린다. {@code file}과 {@code repository} 중 정확히 하나만 있다.
 *
 * @param file 첫 4바이트가 ZIP 시그니처인 .zip 파일. 저장소 주소로 요청했으면 null
 * @param repository 검증·정규화한 저장소 주소. ZIP으로 요청했으면 null
 * @param level 분석 수준. null이면 내 프로필 수준
 * @param projectId 붙일 내 프로젝트 (P-1a AC4·AC5). null이면 저장소는 주소로 찾거나 만들고, ZIP은 새로 만든다
 */
public record CreateAnalysisRequest(
        MultipartFile file, RepositoryUrl repository, Level level, Long projectId) {

    public static final String SOURCE_REQUIRED = "ZIP 파일을 올리거나 저장소 주소를 입력하세요.";
    public static final String SOURCE_BOTH = "ZIP 파일과 저장소 주소 중 하나만 보내세요.";
    public static final String FILE_REQUIRED = "ZIP 파일을 선택하세요.";
    public static final String FILE_NOT_ZIP = "ZIP 파일(.zip)만 올릴 수 있습니다.";
    public static final String CONSENT_REQUIRED =
            "주요 파일이 SSAFY GMS를 거쳐 외부 AI로 전송되는 데 동의해야 분석할 수 있습니다.";
    public static final String LEVEL_INVALID = "학습 수준은 BEGINNER·JUNIOR·SENIOR 중 하나여야 합니다.";
    public static final String PROJECT_ID_INVALID = "프로젝트 ID는 숫자여야 합니다.";

    private static final Pattern PROJECT_ID = Pattern.compile("[1-9][0-9]{0,17}");

    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};

    public static CreateAnalysisRequest of(
            MultipartFile file, String sourceUrl, String level, String consent) {
        return of(file, sourceUrl, level, consent, null);
    }

    public static CreateAnalysisRequest of(
            MultipartFile file, String sourceUrl, String level, String consent, String projectId) {
        Map<String, String> details = new LinkedHashMap<>();
        RepositoryUrl repository = null;
        if (file != null && sourceUrl != null) {
            details.put("sourceUrl", SOURCE_BOTH);
        } else if (sourceUrl != null) {
            try {
                repository = RepositoryUrl.parse(sourceUrl);
            } catch (InvalidRepositoryUrlException e) {
                details.put("sourceUrl", e.getMessage());
            }
        } else if (file == null) {
            details.put("file", SOURCE_REQUIRED);
        } else if (file.isEmpty()) {
            details.put("file", FILE_REQUIRED);
        } else if (!hasZipName(file) || !hasZipSignature(file)) {
            details.put("file", FILE_NOT_ZIP);
        }
        if (!"true".equals(consent)) {
            details.put("consent", CONSENT_REQUIRED);
        }
        Level parsedLevel = null;
        if (level != null) {
            parsedLevel =
                    Arrays.stream(Level.values())
                            .filter(l -> l.name().equals(level))
                            .findFirst()
                            .orElse(null);
            if (parsedLevel == null) {
                details.put("level", LEVEL_INVALID);
            }
        }
        Long parsedProjectId = null;
        if (projectId != null) {
            if (PROJECT_ID.matcher(projectId.strip()).matches()) {
                parsedProjectId = Long.valueOf(projectId.strip());
            } else {
                details.put("projectId", PROJECT_ID_INVALID);
            }
        }
        if (!details.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_400, details);
        }
        return new CreateAnalysisRequest(file, repository, parsedLevel, parsedProjectId);
    }

    private static boolean hasZipName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private static boolean hasZipSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return Arrays.equals(in.readNBytes(ZIP_SIGNATURE.length), ZIP_SIGNATURE);
        } catch (IOException e) {
            return false;
        }
    }
}
