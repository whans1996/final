package com.vibenote.concept;

import com.vibenote.concept.domain.Concept;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SQL: resources/mapper/concept/ConceptMapper.xml. A(F-08)와 B(F-09~13)가 함께 쓰므로 <b>자기 블록 안에만</b>
 * 메서드를 추가한다 (XML도 같은 블록). 다른 도메인은 이 Mapper 대신 {@link ConceptQueryService}로 읽는다.
 */
@Mapper
public interface ConceptMapper {

    // ── 공통 조회 ──

    /** 상태와 상관없이 읽는다. 공개 여부 판단은 호출자(ConceptQueryService 등)가 한다. */
    Optional<Concept> findById(long id);

    /**
     * ids 중 PUBLISHED 카드만 (순서는 보장하지 않는다). ids가 비면 IN ()이 SQL 오류이므로 호출하지 않는다. 요약 컬럼만 채우므로
     * status·body·exampleCode 등은 null이다 ({@code isPublished()}로 판단하지 않는다).
     */
    List<Concept> findPublishedByIds(@Param("ids") List<Long> ids);

    // ── F-08 개념 추출 (A) ──

    // ── F-09~13 카드 (B) ──

}
