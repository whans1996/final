package com.vibenote.llm;

import org.apache.ibatis.annotations.Mapper;

/** SQL: resources/mapper/llm/LlmUsageLogMapper.xml. INSERT만 한다 (집계 화면은 범위 밖, F-00). */
@Mapper
public interface LlmUsageLogMapper {

    void insert(LlmUsageLog log);
}
