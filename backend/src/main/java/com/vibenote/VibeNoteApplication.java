package com.vibenote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// JWT만 쓰므로 기본 인메모리 사용자(로그에 임시 비밀번호 출력)를 만들지 않는다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class VibeNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeNoteApplication.class, args);
    }
}
