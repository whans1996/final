package com.vibenote.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vibenote.team.dto.TeamCreatedResponse;
import com.vibenote.team.dto.TeamRequest;
import com.vibenote.user.UserService;
import jakarta.validation.Validator;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

/**
 * F-20a AC1: 생성한 초대 코드가 이미 있으면(UNIQUE 위반) 새로 뽑아 다시 시도한다. 실제 Mapper·DB에 고정 시퀀스 생성기를 주입한 TeamService를
 * 직접 만들어 충돌을 재현한다 (테스트 트랜잭션 안에서 실행되고 롤백된다).
 */
class TeamServiceTest extends TeamTestSupport {

    @Autowired TeamMapper teamMapper;
    @Autowired UserService userService;
    @Autowired Validator validator;
    @Autowired Clock clock;

    /** 생성기가 아직 내주지 않은 코드. 테스트가 정확히 몇 번 뽑았는지 확인한다. */
    private Deque<String> queue;

    private TeamService serviceWith(String... codes) {
        queue = new ArrayDeque<>(List.of(codes));
        InviteCodeGenerator stub =
                new InviteCodeGenerator() {
                    @Override
                    public String generate() {
                        return queue.removeFirst();
                    }
                };
        return new TeamService(teamMapper, userService, stub, validator, clock);
    }

    @Test
    void AC1_코드가_겹치면_다시_뽑는다() {
        long other = insertUser("f20other");
        insertTeam(other, "기존 팀", "DUPL0001", BASE);
        long u = insertUser("f20owner");

        TeamCreatedResponse res =
                serviceWith("DUPL0001", "DUPL0001", "FRESH002")
                        .create(u, new TeamRequest("새 팀", null));

        assertThat(res.inviteCode()).isEqualTo("FRESH002");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT invite_code FROM teams WHERE id = ?",
                                String.class,
                                res.id()))
                .isEqualTo("FRESH002");
        assertThat(memberCount(res.id())).isEqualTo(1);
    }

    @Test
    void AC1_재발급도_코드가_겹치면_다시_뽑는다() {
        long other = insertUser("f20other");
        insertTeam(other, "기존 팀", "DUPL0001", BASE);
        long u = insertUser("f20owner");
        long team = insertTeam(u, "내 팀", "MINE0001", BASE);

        assertThat(serviceWith("DUPL0001", "FRESH002").regenerateInviteCode(team, u).inviteCode())
                .isEqualTo("FRESH002");
    }

    @Test
    void AC1_처음과_다시_시도_3회가_모두_겹치면_실패하고_팀이_생기지_않는다() {
        long other = insertUser("f20other");
        insertTeam(other, "기존 팀", "DUPL0001", BASE);
        long u = insertUser("f20owner");
        // 처음 1회 + 다시 시도 3회 = 정확히 4번 뽑고, 5번째 코드(FRESH005)는 쓰지 않는다
        TeamService service =
                serviceWith("DUPL0001", "DUPL0001", "DUPL0001", "DUPL0001", "FRESH005");

        assertThatThrownBy(() -> service.create(u, new TeamRequest("새 팀", null)))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(queue).containsExactly("FRESH005");
        assertThat(count("SELECT COUNT(*) FROM teams WHERE owner_id = ?", u)).isZero();
    }
}
