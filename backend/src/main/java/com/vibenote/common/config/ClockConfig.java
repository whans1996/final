package com.vibenote.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 서비스 시각의 기준. "오늘"은 Asia/Seoul 날짜로 센다 (F-05a 일일 한도). 테스트에서 시각을 고정할 때 이 빈을 바꾼다. */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    Clock clock() {
        return Clock.system(ZONE);
    }
}
