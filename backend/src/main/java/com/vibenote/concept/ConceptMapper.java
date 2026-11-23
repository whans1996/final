package com.vibenote.concept;

import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.domain.ConceptAppearance;
import com.vibenote.concept.domain.ConceptStatusCount;
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

    // ── F-16 추천 (A) ──

    /** PUBLISHED 카드 전체의 요약 컬럼, id 오름차순. 카드 수백 개 규모라 한 번에 읽는다. */
    List<Concept> findAllPublishedSummaries();

    /** 회원이 찜한 PUBLISHED 카드 (id·category만 채운다). concept_wishes는 B(F-12) 테이블이다. */
    List<Concept> findWishedByUser(long userId);

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

    // F-13 관리자 카드 관리

    /**
     * 관리자 목록 (상태 무관). keyword는 {@code !}로 이스케이프된 값이고, null인 조건은 걸지 않는다. updated_at DESC, id DESC.
     * 요약 컬럼 + status·source·updated_at만 채운다.
     */
    List<Concept> findForAdmin(
            @Param("status") ConceptStatus status,
            @Param("source") ConceptSource source,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") long offset);

    /** {@link #findForAdmin}과 같은 조건의 개수. */
    long countForAdmin(
            @Param("status") ConceptStatus status,
            @Param("source") ConceptSource source,
            @Param("keyword") String keyword);

    /** 카드가 있는 상태만 행이 나온다 (없는 상태는 호출자가 0으로 채운다). */
    List<ConceptStatusCount> countByStatus();

    /** 카드를 잠그고 읽는다. PUT·DELETE에서 상태 확인과 변경 사이에 다른 요청이 끼지 않게 한다. */
    Optional<Concept> findByIdForUpdate(long id);

    /** 같은 이름(콜레이션 기준)의 다른 카드가 있는지. excludeId가 null이면 모든 카드와 비교한다. */
    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 관리자 작성 카드 INSERT (source=ADMIN). 생성된 id를 {@code concept.id}에 채운다. 이름이 겹치면
     * DuplicateKeyException.
     */
    void insertByAdmin(Concept concept);

    /** 내용·상태를 바꾼다. source·wish_count·avg_rating은 건드리지 않는다. */
    void updateByAdmin(Concept concept);

    /** DRAFT 카드만 지운다 (analysis_concepts는 FK CASCADE). 지운 행 수. */
    int deleteDraft(long id);

    /** 카드에 연결된 분석 수 (analysis_concepts PK가 (analysis_id, concept_id)라 행 수 = 분석 수). */
    long countLinkedAnalyses(long conceptId);
}
