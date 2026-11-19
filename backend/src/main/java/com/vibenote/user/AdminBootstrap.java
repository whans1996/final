package com.vibenote.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 기동할 때 {@code app.admin.bootstrap-email} 회원을 ADMIN으로 승격한다 (F-00b AC6). 관리자 화면을 로컬에서 쓰기 위한 것이다.
 *
 * <p>이미 로그인한 세션은 새로고침하면 반영된다 (앱 시작 refresh가 DB role로 Access 토큰을 다시 발급한다). 승격에 실패해도 기동은 계속한다.
 */
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminProperties properties;
    private final UserMapper userMapper;

    public AdminBootstrap(AdminProperties properties, UserMapper userMapper) {
        this.properties = properties;
        this.userMapper = userMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String email = properties.bootstrapEmail();
        if (!StringUtils.hasText(email)) {
            return;
        }
        if (userMapper.promoteToAdmin(email.trim()) == 0) {
            log.warn("app.admin.bootstrap-email에 해당하는 활성 회원이 없어 관리자로 승격하지 않았습니다. 먼저 그 이메일로 가입하세요.");
            return;
        }
        log.info("app.admin.bootstrap-email 회원을 관리자(ADMIN)로 설정했습니다.");
    }
}
