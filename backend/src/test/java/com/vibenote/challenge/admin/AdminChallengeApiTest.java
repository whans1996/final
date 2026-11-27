package com.vibenote.challenge.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.challenge.ChallengeMapper;
import com.vibenote.challenge.domain.Challenge;
import com.vibenote.common.IntegrationTestBase;
import com.vibenote.common.security.JwtTokenProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * F-24 관리자 챌린지 관리 (docs/specs/F-24-challenge-admin.md). 이전 이미지 파일은 트랜잭션 커밋 뒤에 지우므로 테스트 트랜잭션으로 감싸지
 * 않고, 챌린지·{@code @f24.test} 회원·업로드 파일을 테스트마다 직접 정리한다. 오늘은 2026-11-24(KST)다.
 */
class AdminChallengeApiTest extends IntegrationTestBase {

    static final String LIST = "/api/admin/challenges";
    static final String ONE = "/api/admin/challenges/{id}";
    static final Path DIR = Path.of("build/test-uploads/challenges");
    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    static final byte[] GIF = "GIF89a-not-allowed".getBytes(StandardCharsets.US_ASCII);
    static final int TWO_MB = 2 * 1024 * 1024;

    @TestBean Clock clock;

    static Clock clock() {
        return Clock.fixed(LocalDateTime.of(2026, 11, 24, 10, 0).atZone(KST).toInstant(), KST);
    }

    @MockitoSpyBean ChallengeMapper challengeMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider tokenProvider;

    long adminId;
    long userId;

    @BeforeEach
    void setUp() throws IOException {
        cleanUp();
        adminId = insertUser("f24admin");
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", adminId);
        userId = insertUser("f24user");
    }

    @AfterEach
    void cleanUp() throws IOException {
        jdbc.update("DELETE FROM challenges");
        jdbc.update("DELETE FROM users WHERE email LIKE '%@f24.test'");
        if (Files.isDirectory(DIR)) {
            try (Stream<Path> files = Files.list(DIR)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    // ── 데이터 준비 ──

    long insertUser(String name) {
        jdbc.update(
                "INSERT INTO users (email, password_hash, nickname) VALUES (?, 'hash', ?)",
                name + "@f24.test",
                name);
        return lastId();
    }

    long insertChallenge(String title, String start, String end, String goalType, int goal) {
        jdbc.update(
                "INSERT INTO challenges (title, start_date, end_date, goal_type, goal_value,"
                        + " created_by) VALUES (?, ?, ?, ?, ?, ?)",
                title,
                start,
                end,
                goalType,
                goal,
                adminId);
        return lastId();
    }

    void join(long challengeId, long memberId) {
        jdbc.update(
                "INSERT INTO challenge_participants (challenge_id, user_id) VALUES (?, ?)",
                challengeId,
                memberId);
    }

    long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long challengeCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM challenges", Long.class);
    }

    long fileCount() throws IOException {
        if (!Files.isDirectory(DIR)) return 0;
        try (Stream<Path> files = Files.list(DIR)) {
            return files.count();
        }
    }

    static Path fileOf(String imageUrl) {
        return DIR.resolve(imageUrl.substring("/uploads/challenges/".length()));
    }

    static MockMultipartFile image(String name, String type, byte[] bytes) {
        return new MockMultipartFile("image", name, type, bytes);
    }

    static MockMultipartFile png() {
        return image("cover.png", "image/png", PNG);
    }

    static byte[] withHeader(byte[] header, int size) {
        byte[] bytes = Arrays.copyOf(header, size);
        return bytes;
    }

    static Map<String, String> validFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "12월 퀴즈 챌린지");
        fields.put("description", "분석한 프로젝트의 퀴즈를 12월 안에 10번 풀어 보세요.");
        fields.put("startDate", "2026-12-01");
        fields.put("endDate", "2026-12-31");
        fields.put("goalType", "QUIZ_COUNT");
        fields.put("goalValue", "10");
        return fields;
    }

    static MockMultipartHttpServletRequestBuilder form(
            MockMultipartHttpServletRequestBuilder builder,
            Map<String, String> fields,
            MockMultipartFile file) {
        fields.forEach(builder::param);
        if (file != null) builder.file(file);
        return builder;
    }

    static MockMultipartHttpServletRequestBuilder createRequest(
            Map<String, String> fields, MockMultipartFile file) {
        return form(multipart(LIST), fields, file);
    }

    static MockMultipartHttpServletRequestBuilder updateRequest(
            long id, Map<String, String> fields, MockMultipartFile file) {
        return form(multipart(HttpMethod.PUT, ONE, id), fields, file);
    }

    ResultActions asAdmin(AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        return mockMvc.perform(
                request.header(
                        "Authorization",
                        "Bearer " + tokenProvider.createAccessToken(adminId, "ADMIN")));
    }

    ResultActions asUser(AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        return mockMvc.perform(
                request.header(
                        "Authorization",
                        "Bearer " + tokenProvider.createAccessToken(userId, "USER")));
    }

    long create(Map<String, String> fields, MockMultipartFile file) throws Exception {
        MvcResult result =
                asAdmin(createRequest(fields, file)).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    String imageUrlOf(long id) throws Exception {
        MvcResult result = asAdmin(get(ONE, id)).andExpect(status().isOk()).andReturn();
        var node = objectMapper.readTree(result.getResponse().getContentAsString()).get("imageUrl");
        return node == null || node.isNull() ? null : node.asText();
    }

    // ── AC1 등록 ──

    @Test
    void AC1_관리자가_이미지와_함께_챌린지를_등록하면_201을_받고_이미지를_누구나_받을_수_있다() throws Exception {
        long id = create(validFields(), png());

        assertThat(
                        jdbc.queryForObject(
                                "SELECT created_by FROM challenges WHERE id = ?", Long.class, id))
                .isEqualTo(adminId);
        String imageUrl = imageUrlOf(id);
        assertThat(imageUrl)
                .matches(
                        "/uploads/challenges/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
        assertThat(fileOf(imageUrl)).exists().hasBinaryContent(PNG);

        // 내용 검사는 앞부분 매직 바이트만 보므로, 뒤에 HTML을 붙인 파일도 이미지로만 해석되게 두 헤더를 고정한다
        mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG))
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void AC1_이미지_없이_등록하면_imageUrl은_null이다() throws Exception {
        long id = create(validFields(), null);

        asAdmin(get(ONE, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("12월 퀴즈 챌린지"))
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
        assertThat(fileCount()).isZero();
    }

    // ── AC2 본문 검증 ──

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("title", null, "title"),
                Arguments.of("title", "   ", "title"),
                Arguments.of("title", "가".repeat(101), "title"),
                Arguments.of("description", "가".repeat(2001), "description"),
                Arguments.of("startDate", null, "startDate"),
                Arguments.of("startDate", "2026/12/01", "startDate"),
                Arguments.of("startDate", "2026-13-01", "startDate"),
                Arguments.of("endDate", null, "endDate"),
                Arguments.of("endDate", "abc", "endDate"),
                Arguments.of("endDate", "2026-11-30", "endDate"), // < 시작일 12-01
                Arguments.of("goalType", null, "goalType"),
                Arguments.of("goalType", "DAILY", "goalType"),
                Arguments.of("goalValue", null, "goalValue"),
                Arguments.of("goalValue", "0", "goalValue"),
                Arguments.of("goalValue", "1001", "goalValue"),
                Arguments.of("goalValue", "abc", "goalValue"));
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("invalidFields")
    void AC2_본문이_잘못되면_400과_필드별_사유를_받는다(String field, String value, String detail) throws Exception {
        Map<String, String> fields = validFields();
        if (value == null) fields.remove(field);
        else fields.put(field, value);

        asAdmin(createRequest(fields, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + detail).isString());
        assertThat(challengeCount()).isZero();
    }

    @Test
    void AC2_종료일이_오늘보다_앞이거나_기간이_90일을_넘으면_400이다() throws Exception {
        Map<String, String> past = validFields();
        past.put("startDate", "2026-11-20");
        past.put("endDate", "2026-11-23");
        asAdmin(createRequest(past, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.endDate").isString());

        Map<String, String> tooLong = validFields();
        tooLong.put("endDate", "2027-03-01"); // 12-01부터 91일
        asAdmin(createRequest(tooLong, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.endDate").isString());
        assertThat(challengeCount()).isZero();
    }

    static Stream<Arguments> boundaryFields() {
        return Stream.of(
                Arguments.of(Map.of("title", "가")),
                Arguments.of(Map.of("title", "가".repeat(100))),
                Arguments.of(Map.of("title", "  앞뒤 공백  ")),
                Arguments.of(Map.of("description", "가".repeat(2000))),
                Arguments.of(Map.of("description", "")),
                Arguments.of(Map.of("endDate", "2027-02-28")), // 12-01부터 90일
                Arguments.of(Map.of("goalValue", "1")),
                Arguments.of(Map.of("goalValue", "1000")),
                Arguments.of(Map.of("startDate", "2026-11-20", "endDate", "2026-11-24")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boundaryFields")
    void AC2_경계값은_통과한다(Map<String, String> override) throws Exception {
        Map<String, String> fields = validFields();
        fields.putAll(override);

        create(fields, null);
    }

    @Test
    void AC2_제목은_앞뒤_공백을_지우고_빈_설명은_null로_저장한다() throws Exception {
        Map<String, String> fields = validFields();
        fields.put("title", "  공백 제목  ");
        fields.put("description", "   ");
        long id = create(fields, null);

        asAdmin(get(ONE, id))
                .andExpect(jsonPath("$.title").value("공백 제목"))
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    // ── AC3 이미지 검증 ──

    static Stream<Arguments> invalidImages() {
        return Stream.of(
                Arguments.of("gif", image("a.gif", "image/gif", GIF)),
                Arguments.of("확장자·내용 불일치", image("a.png", "image/png", JPEG)),
                Arguments.of("MIME 불일치", image("a.png", "text/plain", PNG)),
                Arguments.of("빈 파일", image("a.png", "image/png", new byte[0])),
                Arguments.of(
                        "2MB + 1바이트", image("a.png", "image/png", withHeader(PNG, TWO_MB + 1))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidImages")
    void AC3_이미지가_규칙에_맞지_않으면_400이고_행도_파일도_남지_않는다(String label, MockMultipartFile file)
            throws Exception {
        asAdmin(createRequest(validFields(), file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.image").isString());
        assertThat(challengeCount()).isZero();
        assertThat(fileCount()).isZero();
    }

    @Test
    void AC3_정확히_2MB_이미지는_통과한다() throws Exception {
        long id = create(validFields(), image("big.png", "image/png", withHeader(PNG, TWO_MB)));

        assertThat(Files.size(fileOf(imageUrlOf(id)))).isEqualTo(TWO_MB);
    }

    // 10MB를 넘는 요청의 413 COMMON_413은 전역 multipart 한도가 처리한다 (GlobalExceptionHandlerTest). MockMvc는
    // multipart 한도를
    // 거치지 않는다.

    // ── AC4 목록 ──

    @Test
    void AC4_목록은_시작일_역순이고_상태와_참여자_수를_준다() throws Exception {
        long c1 = insertChallenge("C1", "2026-11-01", "2026-11-10", "QUIZ_COUNT", 5);
        long c2 = insertChallenge("C2", "2026-11-20", "2026-11-30", "REVIEW_COUNT", 20);
        long c3 = insertChallenge("C3", "2026-11-24", "2026-11-24", "ANALYSIS_COUNT", 1);
        long c4 = insertChallenge("C4", "2026-12-01", "2026-12-31", "QUIZ_COUNT", 10);
        join(c2, adminId);
        join(c2, userId);

        asAdmin(get(LIST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains(ints(c4, c3, c2, c1))))
                .andExpect(
                        jsonPath(
                                "$.items[*].status",
                                contains("UPCOMING", "ONGOING", "ONGOING", "ENDED")))
                .andExpect(jsonPath("$.items[*].participantCount", contains(0, 0, 2, 0)))
                .andExpect(jsonPath("$.items[0].goalType").value("QUIZ_COUNT"))
                .andExpect(jsonPath("$.items[0].goalValue").value(10))
                .andExpect(jsonPath("$.items[0].startDate").value("2026-12-01"))
                .andExpect(jsonPath("$.items[0].endDate").value("2026-12-31"))
                .andExpect(jsonPath("$.items[0].imageUrl").value(nullValue()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(4));

        asAdmin(get(LIST).param("status", "ONGOING"))
                .andExpect(jsonPath("$.items[*].id", contains(ints(c3, c2))))
                .andExpect(jsonPath("$.total").value(2));
        asAdmin(get(LIST).param("status", "UPCOMING"))
                .andExpect(jsonPath("$.items[*].id", contains(ints(c4))));
        asAdmin(get(LIST).param("status", "ENDED"))
                .andExpect(jsonPath("$.items[*].id", contains(ints(c1))));
        asAdmin(get(LIST).param("size", "2").param("page", "2"))
                .andExpect(jsonPath("$.items[*].id", contains(ints(c2, c1))))
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void AC4_시작일이_같으면_id_역순이다() throws Exception {
        long a = insertChallenge("A", "2026-12-01", "2026-12-31", "QUIZ_COUNT", 5);
        long b = insertChallenge("B", "2026-12-01", "2026-12-10", "QUIZ_COUNT", 5);

        asAdmin(get(LIST)).andExpect(jsonPath("$.items[*].id", contains(ints(b, a))));
    }

    @Test
    void AC4_잘못된_상태나_페이지는_400이다() throws Exception {
        asAdmin(get(LIST).param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.status").isString());
        asAdmin(get(LIST).param("page", "0")).andExpect(status().isBadRequest());
        asAdmin(get(LIST).param("size", "101")).andExpect(status().isBadRequest());
        asAdmin(get(LIST).param("size", "abc")).andExpect(status().isBadRequest());
    }

    static Integer[] ints(long... ids) {
        return Arrays.stream(ids).mapToObj(id -> (int) id).toArray(Integer[]::new);
    }

    // ── AC5 상세 ──

    @Test
    void AC5_상세는_설명_시각_상태_참여자_수를_준다() throws Exception {
        long id = create(validFields(), null);
        join(id, userId);

        asAdmin(get(ONE, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("12월 퀴즈 챌린지"))
                .andExpect(jsonPath("$.description").value("분석한 프로젝트의 퀴즈를 12월 안에 10번 풀어 보세요."))
                .andExpect(jsonPath("$.startDate").value("2026-12-01"))
                .andExpect(jsonPath("$.endDate").value("2026-12-31"))
                .andExpect(jsonPath("$.goalType").value("QUIZ_COUNT"))
                .andExpect(jsonPath("$.goalValue").value(10))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString());
    }

    @Test
    void AC4_AC5_관리자_목록과_상세의_참여자_수도_탈퇴_회원을_뺀다() throws Exception {
        // #202: 회원 화면과 같은 집계(ChallengeMapper withParticipantCount)를 쓴다
        long id = create(validFields(), null);
        join(id, userId);
        long gone = insertUser("f24gone");
        join(id, gone);
        jdbc.update(
                "UPDATE users SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?", gone);

        asAdmin(get(LIST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].participantCount").value(1));
        asAdmin(get(ONE, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(1));
    }

    @Test
    void AC5_없는_챌린지는_404_CHALLENGE_404() throws Exception {
        asAdmin(get(ONE, 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));
    }

    // ── AC6 예정 챌린지 수정 ──

    @Test
    void AC6_예정_챌린지는_모든_필드와_이미지를_바꾸고_이전_파일을_지운다() throws Exception {
        long id = create(validFields(), png());
        String oldUrl = imageUrlOf(id);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "바뀐 제목");
        fields.put("description", "바뀐 설명");
        fields.put("startDate", "2026-12-05");
        fields.put("endDate", "2026-12-20");
        fields.put("goalType", "REVIEW_COUNT");
        fields.put("goalValue", "20");
        asAdmin(updateRequest(id, fields, image("new.jpeg", "image/jpeg", JPEG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.description").value("바뀐 설명"))
                .andExpect(jsonPath("$.startDate").value("2026-12-05"))
                .andExpect(jsonPath("$.endDate").value("2026-12-20"))
                .andExpect(jsonPath("$.goalType").value("REVIEW_COUNT"))
                .andExpect(jsonPath("$.goalValue").value(20))
                .andExpect(jsonPath("$.imageUrl", matchesPattern("/uploads/challenges/.+\\.jpg")))
                .andExpect(jsonPath("$.imageUrl", not(oldUrl)));

        String newUrl = imageUrlOf(id);
        assertThat(fileOf(newUrl)).exists().hasBinaryContent(JPEG);
        assertThat(fileOf(oldUrl)).doesNotExist();
    }

    @Test
    void AC6_저장이_롤백되면_새_이미지_파일을_지우고_이전_파일은_남긴다() throws Exception {
        long id = create(validFields(), png());
        String oldUrl = imageUrlOf(id);
        doThrow(new IllegalStateException("db down"))
                .when(challengeMapper)
                .update(any(Challenge.class));
        doThrow(new IllegalStateException("db down"))
                .when(challengeMapper)
                .insert(any(Challenge.class));

        asAdmin(updateRequest(id, validFields(), image("new.jpeg", "image/jpeg", JPEG)))
                .andExpect(status().isInternalServerError());
        asAdmin(createRequest(validFields(), png())).andExpect(status().isInternalServerError());

        assertThat(fileCount()).isEqualTo(1);
        assertThat(fileOf(oldUrl)).exists().hasBinaryContent(PNG);
        assertThat(challengeCount()).isEqualTo(1);
        assertThat(imageUrlOf(id)).isEqualTo(oldUrl);
    }

    @Test
    void AC6_이미지_없이_보내면_유지하고_removeImage면_지운다() throws Exception {
        long id = create(validFields(), png());
        String oldUrl = imageUrlOf(id);

        asAdmin(updateRequest(id, validFields(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(oldUrl));
        assertThat(fileOf(oldUrl)).exists();

        Map<String, String> remove = validFields();
        remove.put("removeImage", "true");
        asAdmin(updateRequest(id, remove, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
        assertThat(fileOf(oldUrl)).doesNotExist();
    }

    @Test
    void AC6_새_이미지와_removeImage를_함께_보내거나_검증에_실패하면_400이고_바뀌지_않는다() throws Exception {
        long id = create(validFields(), png());
        String oldUrl = imageUrlOf(id);

        Map<String, String> both = validFields();
        both.put("removeImage", "true");
        asAdmin(updateRequest(id, both, image("b.png", "image/png", PNG)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.image").isString());

        Map<String, String> invalid = validFields();
        invalid.put("title", " ");
        asAdmin(updateRequest(id, invalid, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.title").isString());

        asAdmin(updateRequest(id, validFields(), image("a.gif", "image/gif", GIF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.image").isString());

        assertThat(imageUrlOf(id)).isEqualTo(oldUrl);
        assertThat(fileOf(oldUrl)).exists();
        assertThat(fileCount()).isEqualTo(1);
    }

    @Test
    void AC6_없는_챌린지를_수정하면_404다_본문이_잘못되어도_404가_먼저다() throws Exception {
        asAdmin(updateRequest(999_999, validFields(), null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));

        Map<String, String> invalid = validFields();
        invalid.remove("title");
        asAdmin(updateRequest(999_999, invalid, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));
    }

    // ── AC7 시작한 챌린지 잠금 ──

    static Map<String, String> ongoingFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "진행 중");
        fields.put("startDate", "2026-11-20");
        fields.put("endDate", "2026-11-30");
        fields.put("goalType", "QUIZ_COUNT");
        fields.put("goalValue", "10");
        return fields;
    }

    static Stream<Arguments> lockedChanges() {
        return Stream.of(
                Arguments.of("goalType", "REVIEW_COUNT"),
                Arguments.of("goalValue", "11"),
                Arguments.of("startDate", "2026-11-21"));
    }

    @ParameterizedTest(name = "{0}={1}")
    @MethodSource("lockedChanges")
    void AC7_시작한_챌린지의_목표나_시작일을_바꾸면_409_CHALLENGE_001(String field, String value) throws Exception {
        long id = insertChallenge("진행 중", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        Map<String, String> fields = ongoingFields();
        fields.put("title", "바꾸려던 제목");
        fields.put(field, value);

        asAdmin(updateRequest(id, fields, png()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_001"));

        asAdmin(get(ONE, id))
                .andExpect(jsonPath("$.title").value("진행 중"))
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
        assertThat(fileCount()).isZero();
    }

    @Test
    void AC7_목표를_바꾸면서_본문도_잘못되면_400이_먼저다() throws Exception {
        long id = insertChallenge("진행 중", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        Map<String, String> fields = ongoingFields();
        fields.put("goalValue", "11");
        fields.put("title", "");

        asAdmin(updateRequest(id, fields, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.title").isString());
    }

    @Test
    void AC7_시작한_챌린지도_같은_목표와_함께_제목_설명_이미지_종료일은_바꿀_수_있다() throws Exception {
        long id = insertChallenge("진행 중", "2026-11-20", "2026-11-30", "QUIZ_COUNT", 10);
        Map<String, String> fields = ongoingFields();
        fields.put("title", "바뀐 제목");
        fields.put("description", "설명 추가");
        fields.put("endDate", "2026-12-10");

        asAdmin(updateRequest(id, fields, png()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.description").value("설명 추가"))
                .andExpect(jsonPath("$.endDate").value("2026-12-10"))
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.imageUrl", matchesPattern("/uploads/challenges/.+\\.png")));

        Map<String, String> past = ongoingFields();
        past.put("endDate", "2026-11-23");
        asAdmin(updateRequest(id, past, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.endDate").isString());
    }

    @Test
    void AC7_종료_챌린지는_종료일을_그대로_두면_고칠_수_있고_종료일을_늘리면_다시_진행_중이다() throws Exception {
        long id = insertChallenge("종료", "2026-11-01", "2026-11-10", "QUIZ_COUNT", 5);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "종료 제목 수정");
        fields.put("description", "설명");
        fields.put("startDate", "2026-11-01");
        fields.put("endDate", "2026-11-10");
        fields.put("goalType", "QUIZ_COUNT");
        fields.put("goalValue", "5");

        asAdmin(updateRequest(id, fields, png()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("종료 제목 수정"))
                .andExpect(jsonPath("$.status").value("ENDED"));

        fields.put("endDate", "2026-12-05");
        asAdmin(updateRequest(id, fields, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value("2026-12-05"))
                .andExpect(jsonPath("$.status").value("ONGOING"));

        fields.put("endDate", "2027-02-01"); // 11-01부터 93일
        asAdmin(updateRequest(id, fields, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.endDate").isString());
    }

    // ── AC8 삭제 ──

    @Test
    void AC8_삭제하면_참여_기록과_이미지_파일도_사라진다() throws Exception {
        long id = create(validFields(), png());
        String imageUrl = imageUrlOf(id);
        join(id, adminId);
        join(id, userId);

        asAdmin(delete(ONE, id)).andExpect(status().isNoContent());

        assertThat(challengeCount()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM challenge_participants WHERE challenge_id ="
                                        + " ?",
                                Long.class,
                                id))
                .isZero();
        assertThat(fileOf(imageUrl)).doesNotExist();
        asAdmin(get(ONE, id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));
    }

    @Test
    void AC8_없는_챌린지를_삭제하면_404() throws Exception {
        asAdmin(delete(ONE, 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_404"));
    }

    // ── AC9 권한 ──

    @Test
    void AC9_비로그인은_401_회원은_403이고_아무것도_바뀌지_않는다() throws Exception {
        long id = insertChallenge("기존", "2026-12-01", "2026-12-31", "QUIZ_COUNT", 10);

        for (var request :
                new AbstractMockHttpServletRequestBuilder<?>[] {
                    get(LIST),
                    get(ONE, id),
                    createRequest(validFields(), png()),
                    updateRequest(id, validFields(), png()),
                    delete(ONE, id)
                }) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_401"));
        }
        for (var request :
                new AbstractMockHttpServletRequestBuilder<?>[] {
                    get(LIST),
                    get(ONE, id),
                    createRequest(validFields(), png()),
                    updateRequest(id, validFields(), png()),
                    delete(ONE, id)
                }) {
            asUser(request)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_403"));
        }

        assertThat(challengeCount()).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT title FROM challenges WHERE id = ?", String.class, id))
                .isEqualTo("기존");
        assertThat(fileCount()).isZero();
    }
}
