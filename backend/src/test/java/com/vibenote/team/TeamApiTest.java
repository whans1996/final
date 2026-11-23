package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

/** F-20a 팀 API (docs/specs/F-20a-team.md AC1~AC9, AC11). */
class TeamApiTest extends TeamTestSupport {

    private static final String TEAMS = "/api/teams";
    private static final String TEAM = "/api/teams/{id}";
    private static final String MY_TEAMS = "/api/users/me/teams";
    private static final String JOIN = "/api/teams/join";
    private static final String INVITE_CODE = "/api/teams/{id}/invite-code";
    private static final String LEAVE = "/api/teams/{id}/members/me";
    private static final String MEMBER = "/api/teams/{id}/members/{userId}";
    private static final String CODE_PATTERN = "[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}";

    private String json(Object body) {
        return objectMapper.writeValueAsString(body);
    }

    private String teamJson(String name, String description) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name);
        body.put("description", description);
        return json(body);
    }

    private String joinJson(String code) {
        return json(Map.of("inviteCode", code));
    }

    private String code(long teamId) {
        return jdbc.queryForObject(
                "SELECT invite_code FROM teams WHERE id = ?", String.class, teamId);
    }

    private int teamCount(long ownerId) {
        return count("SELECT COUNT(*) FROM teams WHERE owner_id = ?", ownerId);
    }

    // ── AC1 ──

    @Test
    void AC1_팀을_만들면_201과_초대_코드를_받고_팀장이_된다() throws Exception {
        long u = insertUser("f20owner");

        String body =
                perform(post(TEAMS), u, teamJson("  스프링 스터디  ", "  F-05 분석 결과를 같이 공부합니다 "))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.inviteCode").value(matchesPattern(CODE_PATTERN)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        JsonNode res = objectMapper.readTree(body);
        long teamId = res.get("id").asLong();

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM teams WHERE id = ?", teamId);
        assertThat(row.get("name")).isEqualTo("스프링 스터디");
        assertThat(row.get("description")).isEqualTo("F-05 분석 결과를 같이 공부합니다");
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(u);
        assertThat(row.get("invite_code")).isEqualTo(res.get("inviteCode").asString());
        assertThat(
                        jdbc.queryForList(
                                "SELECT user_id, role FROM team_members WHERE team_id = ?", teamId))
                .containsExactly(Map.of("user_id", u, "role", "OWNER"));
    }

    // ── AC2 ──

    @Test
    void AC2_이름이_없거나_공백이거나_31자면_400이고_팀이_생기지_않는다() throws Exception {
        long u = insertUser("f20owner");

        for (String name : new String[] {null, "   ", "가".repeat(31)}) {
            perform(post(TEAMS), u, teamJson(name, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_400"))
                    .andExpect(jsonPath("$.details.name").exists());
        }
        perform(post(TEAMS), u, teamJson("팀", " " + "가".repeat(201) + " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.description").exists());

        assertThat(teamCount(u)).isZero();
        assertThat(count("SELECT COUNT(*) FROM team_members WHERE user_id = ?", u)).isZero();
    }

    @Test
    void AC2_경계값_30자_200자는_성공하고_소개가_공백뿐이면_null_이름_중복은_허용한다() throws Exception {
        long u = insertUser("f20owner");

        perform(post(TEAMS), u, teamJson(" " + "가".repeat(30) + " ", "나".repeat(200)))
                .andExpect(status().isCreated());
        perform(post(TEAMS), u, teamJson("같은 이름", "   ")).andExpect(status().isCreated());
        perform(post(TEAMS), u, teamJson("같은 이름", null)).andExpect(status().isCreated());

        assertThat(teamCount(u)).isEqualTo(3);
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM teams WHERE owner_id = ? AND name = '같은 이름'"
                                        + " AND description IS NULL",
                                u))
                .isEqualTo(2);
    }

    // ── AC3 ──

    @Test
    void AC3_내_팀_목록은_내_가입_시각_최신순이고_남의_팀은_없다() throws Exception {
        long u = insertUser("f20me", "JUNIOR");
        long v = insertUser("f20other");
        long t1 = insertTeam(u, "팀원1", "TEAM0001", BASE);
        long t2 = insertTeam(v, "팀원2", "TEAM0002", BASE.minusDays(3));
        addMember(t2, u, BASE.plusDays(1));
        long t3 = insertTeam(v, "팀원3", "TEAM0003", BASE);
        jdbc.update("UPDATE teams SET description = '소개' WHERE id = ?", t1);

        perform(get(MY_TEAMS), u)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(t2))
                .andExpect(jsonPath("$[0].myRole").value("MEMBER"))
                .andExpect(jsonPath("$[0].memberCount").value(2))
                .andExpect(jsonPath("$[0].ownerNickname").value("f20other"))
                .andExpect(jsonPath("$[0].description").value(nullValue()))
                .andExpect(jsonPath("$[0].joinedAt").value("2026-11-25T10:00:00+09:00"))
                .andExpect(jsonPath("$[1].id").value(t1))
                .andExpect(jsonPath("$[1].name").value("팀원1"))
                .andExpect(jsonPath("$[1].description").value("소개"))
                .andExpect(jsonPath("$[1].myRole").value("OWNER"))
                .andExpect(jsonPath("$[1].memberCount").value(1))
                .andExpect(jsonPath("$[1].ownerNickname").value("f20me"));
        assertThat(t3).isPositive();
    }

    @Test
    void AC3_속한_팀이_없으면_빈_배열이다() throws Exception {
        long u = insertUser("f20alone");

        perform(get(MY_TEAMS), u).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    // ── AC4 ──

    @Test
    void AC4_멤버는_상세를_보고_팀장이_맨_앞_나머지는_가입순이며_코드는_팀장에게만_보인다() throws Exception {
        long owner = insertUser("f20owner", "JUNIOR");
        long a = insertUser("f20a");
        long b = insertUser("f20b", "SENIOR");
        long c = insertUser("f20c");
        long team = insertTeam(owner, "스프링 스터디", "TEAM0001", BASE.plusHours(2));
        addMember(team, b, BASE.plusHours(1));
        addMember(team, a, BASE.plusHours(1));
        addMember(team, c, BASE);
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        perform(get(TEAM, team), owner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(team))
                .andExpect(jsonPath("$.name").value("스프링 스터디"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.inviteCode").value("TEAM0001"))
                .andExpect(jsonPath("$.memberCount").value(4))
                .andExpect(jsonPath("$.maxMembers").value(10))
                .andExpect(jsonPath("$.createdAt").value("2026-11-24T12:00:00+09:00"))
                .andExpect(jsonPath("$.members", hasSize(4)))
                .andExpect(jsonPath("$.members[0].userId").value(owner))
                .andExpect(jsonPath("$.members[0].nickname").value("f20owner"))
                .andExpect(jsonPath("$.members[0].level").value("JUNIOR"))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"))
                .andExpect(jsonPath("$.members[1].userId").value(c))
                .andExpect(jsonPath("$.members[1].joinedAt").value("2026-11-24T10:00:00+09:00"))
                .andExpect(jsonPath("$.members[2].userId").value(first))
                .andExpect(jsonPath("$.members[3].userId").value(second))
                .andExpect(jsonPath("$.members[3].role").value("MEMBER"));

        perform(get(TEAM, team), c)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.inviteCode").value(nullValue()));
    }

    @Test
    void AC4_멤버가_아니거나_없는_팀이면_404_TEAM_404() throws Exception {
        long owner = insertUser("f20owner");
        long stranger = insertUser("f20stranger");
        long team = insertTeam(owner, "팀", "TEAM0001", BASE);

        perform(get(TEAM, team), stranger)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"))
                .andExpect(jsonPath("$.message").value("팀을 찾을 수 없습니다."));
        perform(get(TEAM, 999999), owner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));
    }

    // ── AC5 ──

    @Test
    void AC5_코드는_공백을_빼고_대문자로_비교해_가입한다() throws Exception {
        long owner = insertUser("f20owner");
        long w = insertUser("f20w");
        long team = insertTeam(owner, "팀", "K7M2QX9P", BASE);

        perform(post(JOIN), w, joinJson(" k7m2qx9p "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(team));

        assertThat(
                        jdbc.queryForObject(
                                "SELECT role FROM team_members WHERE team_id = ? AND user_id = ?",
                                String.class,
                                team,
                                w))
                .isEqualTo("MEMBER");
    }

    @Test
    void AC5_없는_코드는_404_TEAM_001_이미_멤버면_409_TEAM_409_빈_코드는_400() throws Exception {
        long owner = insertUser("f20owner");
        long w = insertUser("f20w");
        long team = insertTeam(owner, "팀", "K7M2QX9P", BASE);
        addMember(team, w, BASE);

        perform(post(JOIN), w, joinJson("ZZZZ0000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_001"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 초대 코드입니다."));
        perform(post(JOIN), w, joinJson("K7M2QX9P"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_409"))
                .andExpect(jsonPath("$.message").value("이미 가입한 팀입니다."));
        perform(post(JOIN), owner, joinJson("K7M2QX9P"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_409"));
        perform(post(JOIN), w, joinJson("   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.inviteCode").exists());
        perform(post(JOIN), w, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.inviteCode").exists());

        assertThat(memberCount(team)).isEqualTo(2);
    }

    // ── AC6 ──

    @Test
    void AC6_10명인_팀에_가입하면_409_TEAM_002() throws Exception {
        long owner = insertUser("f20owner");
        long team = insertTeam(owner, "팀", "FULL0001", BASE);
        for (int i = 1; i <= 9; i++) {
            addMember(team, insertUser("f20m" + i), BASE);
        }
        long w = insertUser("f20w");

        perform(post(JOIN), w, joinJson("FULL0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_002"))
                .andExpect(jsonPath("$.message").value("팀 인원이 가득 찼습니다. (최대 10명)"));

        assertThat(memberCount(team)).isEqualTo(10);
        assertThat(isMember(team, w)).isFalse();
    }

    @Test
    void AC6_5개_팀에_속한_회원은_만들기와_가입이_409_TEAM_003이고_가득_찬_팀보다_먼저다() throws Exception {
        long w = insertUser("f20w");
        long other = insertUser("f20other");
        for (int i = 1; i <= 5; i++) {
            addMember(insertTeam(other, "팀" + i, "FIVE000" + i, BASE), w, BASE);
        }
        long open = insertTeam(other, "열린 팀", "OPEN0001", BASE);
        long full = insertTeam(insertUser("f20owner"), "가득 찬 팀", "FULL0001", BASE);
        for (int i = 1; i <= 9; i++) {
            addMember(full, insertUser("f20m" + i), BASE);
        }

        perform(post(TEAMS), w, teamJson("새 팀", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_003"))
                .andExpect(jsonPath("$.message").value("더 이상 팀에 속할 수 없습니다. (최대 5개)"));
        perform(post(JOIN), w, joinJson("OPEN0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_003"));
        perform(post(JOIN), w, joinJson("FULL0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_003"));

        assertThat(teamCount(w)).isZero();
        assertThat(isMember(open, w)).isFalse();
        assertThat(memberCount(full)).isEqualTo(10);
    }

    // ── AC7 ──

    @Test
    void AC7_팀장은_정보를_통째로_바꾸고_코드를_재발급하며_이전_코드는_무효다() throws Exception {
        long owner = insertUser("f20owner");
        long w = insertUser("f20w");
        long team = insertTeam(owner, "옛 이름", "OLD00001", BASE);
        jdbc.update("UPDATE teams SET description = '옛 소개' WHERE id = ?", team);

        perform(put(TEAM, team), owner, teamJson(" 바뀐 이름 ", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바뀐 이름"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.members", hasSize(1)));

        String body =
                perform(post(INVITE_CODE, team), owner)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.inviteCode").value(matchesPattern(CODE_PATTERN)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String newCode = objectMapper.readTree(body).get("inviteCode").asString();
        assertThat(code(team)).isEqualTo(newCode).isNotEqualTo("OLD00001");

        perform(post(JOIN), w, joinJson("OLD00001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_001"));
        perform(post(JOIN), w, joinJson(newCode)).andExpect(status().isOk());
    }

    @Test
    void AC7_팀원은_403_멤버가_아니면_404이고_검증보다_먼저다() throws Exception {
        long owner = insertUser("f20owner");
        long member = insertUser("f20member");
        long stranger = insertUser("f20stranger");
        long team = insertTeam(owner, "그대로", "KEEP0001", BASE);
        addMember(team, member, BASE);

        perform(put(TEAM, team), member, teamJson("", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TEAM_403"))
                .andExpect(jsonPath("$.message").value("팀장만 할 수 있습니다."));
        perform(post(INVITE_CODE, team), member)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TEAM_403"));
        perform(put(TEAM, team), stranger, teamJson("", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));
        perform(post(INVITE_CODE, team), stranger)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));
        perform(put(TEAM, 999999), owner, teamJson("이름", null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));
        perform(put(TEAM, team), owner, teamJson("가".repeat(31), "나".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.description").exists());

        assertThat(jdbc.queryForObject("SELECT name FROM teams WHERE id = ?", String.class, team))
                .isEqualTo("그대로");
        assertThat(code(team)).isEqualTo("KEEP0001");
    }

    // ── AC8 ──

    @Test
    void AC8_팀원은_탈퇴하고_팀장은_내보내며_다시_가입할_수_있다() throws Exception {
        long owner = insertUser("f20owner");
        long v = insertUser("f20v");
        long w = insertUser("f20w");
        long team = insertTeam(owner, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE);
        addMember(team, w, BASE);

        perform(delete(LEAVE, team), v).andExpect(status().isNoContent());
        perform(delete(MEMBER, team, w), owner).andExpect(status().isNoContent());

        assertThat(isMember(team, v)).isFalse();
        assertThat(isMember(team, w)).isFalse();
        assertThat(memberCount(team)).isEqualTo(1);
        perform(get(TEAM, team), v)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));
        perform(post(JOIN), w, joinJson("TEAM0001")).andExpect(status().isOk());
    }

    @Test
    void AC8_팀장은_떠날_수_없고_대상이_아니면_TEAM_005_팀원은_내보낼_수_없다() throws Exception {
        long owner = insertUser("f20owner");
        long v = insertUser("f20v");
        long w = insertUser("f20w");
        long stranger = insertUser("f20stranger");
        long team = insertTeam(owner, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE);
        addMember(team, w, BASE);

        perform(delete(LEAVE, team), owner)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEAM_004"))
                .andExpect(jsonPath("$.message").value("팀장은 팀을 떠날 수 없습니다. 팀을 삭제해 주세요."));
        perform(delete(MEMBER, team, owner), owner)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TEAM_004"));
        perform(delete(MEMBER, team, stranger), owner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_005"))
                .andExpect(jsonPath("$.message").value("팀원을 찾을 수 없습니다."));
        perform(delete(MEMBER, team, 999999), owner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_005"));
        perform(delete(MEMBER, team, w), v)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TEAM_403"));
        perform(delete(LEAVE, team), stranger)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));
        perform(delete(MEMBER, team, w), stranger)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));

        assertThat(memberCount(team)).isEqualTo(3);
    }

    // ── AC9 ──

    @Test
    void AC9_팀장이_삭제하면_팀과_멤버가_지워지고_코드가_무효다() throws Exception {
        long owner = insertUser("f20owner");
        long v = insertUser("f20v");
        long w = insertUser("f20w");
        long team = insertTeam(owner, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE);

        perform(delete(TEAM, team), owner).andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM teams WHERE id = ?", team)).isZero();
        assertThat(memberCount(team)).isZero();
        perform(get(MY_TEAMS), v).andExpect(jsonPath("$", hasSize(0)));
        perform(post(JOIN), w, joinJson("TEAM0001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_001"));
    }

    @Test
    void AC9_팀원이_삭제하면_403_멤버가_아니면_404() throws Exception {
        long owner = insertUser("f20owner");
        long v = insertUser("f20v");
        long stranger = insertUser("f20stranger");
        long team = insertTeam(owner, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE);

        perform(delete(TEAM, team), v)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TEAM_403"));
        perform(delete(TEAM, team), stranger)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_404"));

        assertThat(memberCount(team)).isEqualTo(2);
    }

    // ── AC11 ──

    @Test
    void AC11_토큰이_없거나_만료_깨졌거나_탈퇴한_회원이면_모든_팀_API가_401이다() throws Exception {
        long owner = insertUser("f20owner");
        long v = insertUser("f20v");
        long gone = insertUser("f20gone");
        long team = insertTeam(owner, "팀", "TEAM0001", BASE);
        addMember(team, v, BASE);
        addMember(team, gone, BASE);
        withdraw(gone);
        String teamBody = teamJson("이름", null);

        List<Supplier<MockHttpServletRequestBuilder>> requests =
                List.of(
                        () -> post(TEAMS).contentType("application/json").content(teamBody),
                        () -> get(MY_TEAMS),
                        () -> get(TEAM, team),
                        () -> put(TEAM, team).contentType("application/json").content(teamBody),
                        () -> delete(TEAM, team),
                        () -> post(INVITE_CODE, team),
                        () ->
                                post(JOIN)
                                        .contentType("application/json")
                                        .content(joinJson("TEAM0001")),
                        () -> delete(LEAVE, team),
                        () -> delete(MEMBER, team, v));
        String[] authorizations = {
            null, expiredBearer(owner), "Bearer not-a-jwt", bearer(gone),
        };

        for (Supplier<MockHttpServletRequestBuilder> request : requests) {
            for (String authorization : authorizations) {
                perform(request.get(), authorization, null)
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("AUTH_401"));
            }
        }

        assertThat(memberCount(team)).isEqualTo(3);
        assertThat(code(team)).isEqualTo("TEAM0001");
        assertThat(count("SELECT COUNT(*) FROM teams WHERE owner_id IN (?, ?)", owner, gone))
                .isEqualTo(1);
    }
}
