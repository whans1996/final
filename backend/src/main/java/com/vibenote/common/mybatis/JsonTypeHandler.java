package com.vibenote.common.mybatis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * MySQL JSON 컬럼 ↔ Java 객체 변환 (analyses.overview, analysis_flows.steps, quiz_questions.choices).
 *
 * <p>MyBatis가 {@code javaType}을 생성자 인자로 넘겨 주므로 매핑에 javaType을 함께 적는다. 목록은 제네릭 정보가 사라지므로 배열 타입이나
 * {@code JsonNode}를 쓴다.
 *
 * <pre>{@code
 * <result column="steps" property="steps" javaType="com.vibenote.analysis.domain.FlowStep[]"
 *         typeHandler="com.vibenote.common.mybatis.JsonTypeHandler"/>
 * #{steps, javaType=com.vibenote.analysis.domain.FlowStep[],
 *          typeHandler=com.vibenote.common.mybatis.JsonTypeHandler}
 * }</pre>
 */
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Class<T> type;

    public JsonTypeHandler(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("JsonTypeHandler에는 javaType이 필요합니다.");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, write(parameter));
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return read(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return read(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return read(cs.getString(columnIndex));
    }

    private String write(T value) throws SQLException {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON 직렬화 실패: " + type.getName(), e);
        }
    }

    private T read(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON 역직렬화 실패: " + type.getName(), e);
        }
    }
}
