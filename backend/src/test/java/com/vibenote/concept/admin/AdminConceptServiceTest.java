package com.vibenote.concept.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vibenote.common.Level;
import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.ConceptMapper;
import com.vibenote.concept.ConceptSource;
import com.vibenote.concept.ConceptStatus;
import com.vibenote.concept.admin.dto.AdminConceptRequest;
import com.vibenote.concept.domain.Concept;
import com.vibenote.concept.review.ConceptReviewMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/** 중복 확인과 저장 사이에 같은 이름이 먼저 들어온 경우 (동시 요청)를 재현한다. 통합 테스트로는 순서를 맞추기 어렵다. */
class AdminConceptServiceTest {

    private final ConceptMapper mapper = mock(ConceptMapper.class);
    private final AdminConceptService service =
            new AdminConceptService(mapper, mock(ConceptReviewMapper.class));

    private static AdminConceptRequest request(String status) {
        return new AdminConceptRequest(
                "낙관적 락", "spring", "SENIOR", "요약", null, null, null, status, null);
    }

    @Test
    void AC6_생성_중_UNIQUE_충돌은_CONCEPT_002() {
        when(mapper.existsByName(anyString(), isNull())).thenReturn(false);
        doThrow(new DuplicateKeyException("uk_concepts_name")).when(mapper).insertByAdmin(any());

        assertThatThrownBy(() -> service.create(request("PUBLISHED")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONCEPT_002);
                            assertThat(e.getDetails()).containsOnlyKeys("name");
                        });
    }

    @Test
    void AC6_수정_중_UNIQUE_충돌은_CONCEPT_002() {
        Concept draft = new Concept();
        draft.setId(7L);
        draft.setName("초안");
        draft.setDifficulty(Level.JUNIOR);
        draft.setStatus(ConceptStatus.DRAFT);
        draft.setSource(ConceptSource.AI);
        when(mapper.findByIdForUpdate(7L)).thenReturn(Optional.of(draft));
        when(mapper.existsByName(anyString(), anyLong())).thenReturn(false);
        doThrow(new DuplicateKeyException("uk_concepts_name")).when(mapper).updateByAdmin(any());

        assertThatThrownBy(() -> service.update(7L, request("PUBLISHED")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONCEPT_002);
                            assertThat(e.getDetails())
                                    .isEqualTo(Map.of("name", AdminConceptService.NAME_TAKEN));
                        });
    }
}
