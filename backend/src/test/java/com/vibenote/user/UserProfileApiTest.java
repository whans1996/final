package com.vibenote.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vibenote.common.Level;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** F-03 프로필 조회·수정 (docs/specs/F-03-profile.md AC1, AC2, AC4, AC5). */
class UserProfileApiTest extends UserTestSupport {

    @Autowired UserMapper userMapper;

    private ResultActions patchMe(long userId, String json) throws Exception {
        return mockMvc.perform(
                patch("/api/users/me")
                        .header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    @Test
    void AC1_로그인하면_내_프로필을_email까지_받는다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        jdbc.update(
                "UPDATE users SET bio = '스프링 공부 중', created_at = '2026-01-01 09:00:00' WHERE id ="
                        + " ?",
                id);

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("코딩초보"))
                .andExpect(jsonPath("$.bio").value("스프링 공부 중"))
                .andExpect(jsonPath("$.level").value("BEGINNER"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T09:00:00+09:00"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void AC1_토큰이_없으면_401_AUTH_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    void AC2_비로그인도_공개_프로필을_받고_email과_role은_없다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        jdbc.update("UPDATE users SET created_at = '2026-01-01 09:00:00' WHERE id = ?", id);

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nickname").value("코딩초보"))
                .andExpect(jsonPath("$.bio").isEmpty())
                .andExpect(jsonPath("$.level").value("BEGINNER"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T09:00:00+09:00"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void AC2_없는_id나_탈퇴_회원이면_404_USER_404() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", id);

        for (long target : new long[] {id, id + 1000}) {
            mockMvc.perform(get("/api/users/{id}", target))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_404"))
                    .andExpect(jsonPath("$.message").value("존재하지 않거나 탈퇴한 사용자입니다."));
        }
    }

    @Test
    void AC4_보낸_필드만_바뀌고_나머지는_그대로다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        jdbc.update("UPDATE users SET bio = '스프링 공부 중' WHERE id = ?", id);

        patchMe(id, "{\"level\":\"JUNIOR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("코딩초보"))
                .andExpect(jsonPath("$.bio").value("스프링 공부 중"))
                .andExpect(jsonPath("$.level").value("JUNIOR"));

        assertThat(column("level", id)).isEqualTo("JUNIOR");
        assertThat(column("bio", id)).isEqualTo("스프링 공부 중");
    }

    @Test
    void AC4_닉네임과_소개는_trim해서_저장한다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");

        patchMe(id, "{\"nickname\":\" 주니어개발자 \",\"bio\":\"  안녕하세요  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("주니어개발자"))
                .andExpect(jsonPath("$.bio").value("안녕하세요"))
                .andExpect(jsonPath("$.level").value("BEGINNER"));

        assertThat(column("nickname", id)).isEqualTo("주니어개발자");
    }

    @Test
    void AC4_빈_소개는_NULL로_지우고_자기_닉네임은_409가_아니다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");
        jdbc.update("UPDATE users SET bio = '스프링 공부 중' WHERE id = ?", id);

        patchMe(id, "{\"nickname\":\"코딩초보\",\"bio\":\"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("코딩초보"))
                .andExpect(jsonPath("$.bio").isEmpty());

        assertThat(column("bio", id)).isNull();
    }

    @Test
    void AC5_소개_300자는_통과한다() throws Exception {
        long id = createUser("user@example.com", "코딩초보");

        patchMe(id, "{\"bio\":\"" + "가".repeat(300) + "\"}").andExpect(status().isOk());
    }

    static Stream<Arguments> invalidUpdates() {
        return Stream.of(
                Arguments.of("{\"nickname\":\"a\"}", "nickname"),
                Arguments.of("{\"nickname\":\"" + "가".repeat(21) + "\"}", "nickname"),
                Arguments.of("{\"nickname\":\"공 백\"}", "nickname"),
                Arguments.of("{\"nickname\":\"특수!\"}", "nickname"),
                Arguments.of("{\"nickname\":\"  \"}", "nickname"),
                Arguments.of("{\"bio\":\"" + "가".repeat(301) + "\"}", "bio"),
                Arguments.of("{\"level\":\"MASTER\"}", "level"));
    }

    @ParameterizedTest
    @MethodSource("invalidUpdates")
    void AC5_규칙을_어기면_400_COMMON_400과_필드_사유(String json, String field) throws Exception {
        long id = createUser("user@example.com", "코딩초보");

        patchMe(id, json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details." + field).isString());

        assertThat(column("nickname", id)).isEqualTo("코딩초보");
        assertThat(column("level", id)).isEqualTo("BEGINNER");
    }

    @Test
    void AC5_다른_회원의_닉네임이면_대소문자_무시하고_409_USER_409() throws Exception {
        createUser("other@example.com", "Coder");
        long id = createUser("user@example.com", "코딩초보");

        patchMe(id, "{\"nickname\":\"coder\",\"level\":\"SENIOR\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409"))
                .andExpect(jsonPath("$.details.nickname").value("이미 사용 중인 닉네임입니다."));

        assertThat(column("level", id)).isEqualTo("BEGINNER");
    }

    @Test
    void AC4_null인_필드는_SQL이_건드리지_않아_동시에_바뀐_값을_지킨다() {
        long id = createUser("user@example.com", "코딩초보");
        // 다른 요청이 먼저 닉네임·소개를 바꿔 커밋한 상태
        jdbc.update("UPDATE users SET nickname = '먼저바꿈', bio = '먼저' WHERE id = ?", id);

        assertThat(userMapper.updateProfile(id, null, null, Level.JUNIOR)).isEqualTo(1);
        assertThat(column("nickname", id)).isEqualTo("먼저바꿈");
        assertThat(column("bio", id)).isEqualTo("먼저");
        assertThat(column("level", id)).isEqualTo("JUNIOR");

        userMapper.updateProfile(id, null, "", null);
        assertThat(column("bio", id)).isNull();
        assertThat(column("nickname", id)).isEqualTo("먼저바꿈");
    }
}
