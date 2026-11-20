package com.vibenote.concept.review;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vibenote.common.exception.BusinessException;
import com.vibenote.common.exception.ErrorCode;
import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.review.dto.ConceptReviewRequest;
import com.vibenote.feed.ActivityService;
import com.vibenote.user.UserService;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/** F-11 AC6: 미리 확인한 뒤 동시 요청이 UNIQUE 제약에 걸려도 500이 아니라 CONCEPT_409다. */
class ConceptReviewServiceTest {

    @Test
    void AC6_UNIQUE_위반도_409() {
        ConceptReviewMapper mapper = mock(ConceptReviewMapper.class);
        ActivityService activityService = mock(ActivityService.class);
        when(mapper.findByConceptAndUser(anyLong(), anyLong())).thenReturn(Optional.empty());
        doThrow(new DuplicateKeyException("uk_concept_reviews_concept_user"))
                .when(mapper)
                .insert(any());
        ConceptReviewService service =
                new ConceptReviewService(
                        mapper,
                        mock(ConceptQueryService.class),
                        activityService,
                        mock(UserService.class),
                        Clock.systemUTC());

        assertThatThrownBy(() -> service.create(1L, 2L, new ConceptReviewRequest(4, "좋아요")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCEPT_409);
        verifyNoInteractions(activityService);
    }
}
