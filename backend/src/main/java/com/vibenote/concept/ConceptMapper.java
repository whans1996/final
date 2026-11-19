package com.vibenote.concept;

import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.domain.ConceptAppearance;
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

    /** 이름 비교·카드 목록용 전체 카드 (id·name·category·status만, id 오름차순). 카드 수백 개 규모라 한 번에 읽는다. */
    List<Concept> findAllForMatching();

    /**
     * AI 초안 INSERT. 같은 이름의 카드가 이미 있으면(UNIQUE 충돌) 넣지 않고 그 카드의 id를 {@code concept.id}에 채운다 (F-08
     * AC5).
     */
    void insertDraft(Concept concept);

    // ── F-09~13 카드 (B) ──

    /** PUBLISHED 카드 검색 (F-09). keyword는 {@code !}로 이스케이프된 값이고, null인 조건은 걸지 않는다. 요약 컬럼만 채운다. */
    List<Concept> findPublished(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("difficulty") Level difficulty,
            @Param("sort") ConceptSort sort,
            @Param("size") int size,
            @Param("offset") long offset);

    /** {@link #findPublished}와 같은 조건의 개수. */
    long countPublished(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("difficulty") Level difficulty);

    /**
     * 회원의 삭제되지 않은 DONE 분석에서 카드가 등장한 위치, 분석 최신순 최대 10개 (F-10 AC3·AC4). 제목은 {@code COALESCE(title,
     * project_name)}이라 null일 수 있다.
     */
    List<ConceptAppearance> findAppearances(
            @Param("conceptId") long conceptId, @Param("userId") long userId);

    /**
     * 모든 회원의 삭제되지 않은 DONE 분석에서 카드와 함께 연결된 PUBLISHED 카드. 함께 등장한 분석 수 → 찜 수 → 평점 → id 순 (F-10 AC6).
     * 요약 컬럼만 채운다.
     */
    List<Concept> findCoOccurring(@Param("conceptId") long conceptId, @Param("limit") int limit);

    /** 같은 카테고리의 PUBLISHED 카드 인기순. excludeIds는 비지 않아야 한다 (F-10 AC7). 요약 컬럼만 채운다. */
    List<Concept> findSameCategoryPopular(
            @Param("category") String category,
            @Param("excludeIds") List<Long> excludeIds,
            @Param("limit") int limit);
}
