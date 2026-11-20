package com.vibenote.concept.wish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vibenote.concept.ConceptQueryService;
import com.vibenote.concept.wish.dto.ConceptWishResponse;
import com.vibenote.feed.ActivityService;
import com.vibenote.reviewplan.ReviewPlanService;
import com.vibenote.user.UserService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/** F-12 AC2: 미리 확인한 뒤 같은 회원의 동시 찜이 PK에 걸려도 500이 아니라 "이미 찜함" 200 응답이다. */
class ConceptWishServiceTest {

    @Test
    void AC2_DuplicateKey도_이미_찜함() {
        ConceptWishMapper mapper = mock(ConceptWishMapper.class);
        ActivityService activityService = mock(ActivityService.class);
        ReviewPlanService reviewPlanService = mock(ReviewPlanService.class);
        when(mapper.exists(anyLong(), anyLong())).thenReturn(false);
        doThrow(new DuplicateKeyException("PRIMARY"))
                .when(mapper)
                .insert(anyLong(), anyLong(), any());
        when(mapper.findWishCount(anyLong())).thenReturn(5);
        ConceptWishService service =
                new ConceptWishService(
                        mapper,
                        mock(ConceptQueryService.class),
                        activityService,
                        reviewPlanService,
                        mock(UserService.class),
                        Clock.systemUTC());

        ConceptWishResponse response = service.wish(1L, 2L);

        assertThat(response).isEqualTo(new ConceptWishResponse(true, 5, null));
        verifyNoInteractions(activityService, reviewPlanService);
    }
}
