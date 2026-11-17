package com.vibenote.common.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class JsonTypeHandlerTest {

    record Step(int order, String layer, String description) {}

    @Test
    void 객체를_JSON_문자열로_저장한다() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        new JsonTypeHandler<>(Step[].class)
                .setNonNullParameter(
                        ps, 1, new Step[] {new Step(1, "controller", "요청을 받는다")}, null);

        verify(ps)
                .setString(
                        1, "[{\"order\":1,\"layer\":\"controller\",\"description\":\"요청을 받는다\"}]");
    }

    @Test
    void JSON_컬럼을_배열_타입으로_읽는다() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("steps"))
                .thenReturn(
                        "[{\"order\":1,\"layer\":\"service\",\"description\":\"검증\",\"extra\":true}]");

        Step[] steps = new JsonTypeHandler<>(Step[].class).getNullableResult(rs, "steps");

        assertThat(steps).containsExactly(new Step(1, "service", "검증"));
    }

    @Test
    void NULL_컬럼은_null() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        assertThat(new JsonTypeHandler<>(String[].class).getNullableResult(rs, 1)).isNull();
    }

    @Test
    void 깨진_JSON이면_SQLException() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("choices")).thenReturn("[\"a\",");

        assertThatThrownBy(
                        () ->
                                new JsonTypeHandler<>(String[].class)
                                        .getNullableResult(rs, "choices"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("역직렬화");
    }
}
