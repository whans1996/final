package com.vibenote.common.security;

/**
 * 인증된 사용자. Controller에서 {@code @AuthenticationPrincipal AuthUser user}로 받는다.
 *
 * @param id users.id
 * @param role users.role (USER | ADMIN)
 */
public record AuthUser(long id, String role) {}
