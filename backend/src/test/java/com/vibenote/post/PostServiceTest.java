package com.vibenote.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** F-17 PostService 단위 테스트. */
class PostServiceTest {

    @Test
    void keyword_이스케이프() {
        assertThat(PostService.escapeLike("100%")).isEqualTo("100!%");
        assertThat(PostService.escapeLike("snake_case")).isEqualTo("snake!_case");
        assertThat(PostService.escapeLike("표!")).isEqualTo("표!!");
        assertThat(PostService.escapeLike("트랜잭션")).isEqualTo("트랜잭션");
    }
}
