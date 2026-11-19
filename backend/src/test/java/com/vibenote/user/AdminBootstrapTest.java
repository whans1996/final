package com.vibenote.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** 설정값으로 로컬 관리자 계정을 승격한다 (F-00b AC6). 기동 이벤트는 컨텍스트마다 한 번이라, 설정만 바꾼 인스턴스를 만들어 같은 메서드를 부른다. */
@ExtendWith(OutputCaptureExtension.class)
class AdminBootstrapTest extends UserTestSupport {

    @Autowired UserMapper userMapper;

    private AdminBootstrap bootstrap(String email) {
        return new AdminBootstrap(new AdminProperties(email), userMapper);
    }

    @Test
    void AC6_설정한_ACTIVE_회원을_ADMIN으로_승격한다() {
        long id = createUser("admin-boot@vibe.dev", "adminboot");

        bootstrap("admin-boot@vibe.dev").onApplicationReady();

        assertThat(column("role", id)).isEqualTo("ADMIN");
    }

    @Test
    void AC6_설정이_비어_있으면_아무것도_하지_않는다(CapturedOutput output) {
        long id = createUser("admin-boot@vibe.dev", "adminboot");

        bootstrap("").onApplicationReady();
        bootstrap(null).onApplicationReady();

        assertThat(column("role", id)).isEqualTo("USER");
        assertThat(output).doesNotContain("bootstrap-email");
    }

    @Test
    void AC6_회원이_없으면_WARN만_남기고_계속한다(CapturedOutput output) {
        bootstrap("nobody@vibe.dev").onApplicationReady();

        assertThat(output).contains("WARN").contains("bootstrap-email");
    }

    @Test
    void AC6_탈퇴한_회원은_승격하지_않고_WARN을_남긴다(CapturedOutput output) {
        long id = createUser("admin-boot@vibe.dev", "adminboot");
        jdbc.update("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?", id);

        bootstrap("admin-boot@vibe.dev").onApplicationReady();

        assertThat(column("role", id)).isEqualTo("USER");
        assertThat(output).contains("WARN").contains("bootstrap-email");
    }

    @Test
    void AC6_이미_ADMIN이면_그대로다() {
        long id = createUser("admin-boot@vibe.dev", "adminboot");
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", id);

        bootstrap("admin-boot@vibe.dev").onApplicationReady();

        assertThat(column("role", id)).isEqualTo("ADMIN");
    }
}
