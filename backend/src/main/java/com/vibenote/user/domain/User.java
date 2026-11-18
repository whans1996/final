package com.vibenote.user.domain;

import com.vibenote.common.Level;

/** users 행. Controller 응답으로 직접 내보내지 않는다 (DTO로 변환). */
public class User {

    public static final String ROLE_USER = "USER";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private String bio;
    private Level level;
    private String role;
    private String status;

    /** 가입 직후의 새 회원 (role USER, status ACTIVE). */
    public static User newMember(String email, String passwordHash, String nickname, Level level) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.level = level;
        user.role = ROLE_USER;
        user.status = STATUS_ACTIVE;
        return user;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
