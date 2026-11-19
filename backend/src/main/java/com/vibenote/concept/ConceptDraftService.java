package com.vibenote.concept;

import com.vibenote.common.Level;
import com.vibenote.concept.domain.Concept;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석의 개념 추출(F-08, A)이 카드를 읽고 AI 초안을 만드는 입구. 분석 도메인이 {@link ConceptMapper}를 직접 부르지 않게 한다. 관리자
 * 검수(F-13)는 B의 서비스가 한다.
 */
@Service
public class ConceptDraftService {

    private final ConceptMapper conceptMapper;

    public ConceptDraftService(ConceptMapper conceptMapper) {
        this.conceptMapper = conceptMapper;
    }

    /** 상태와 상관없는 전체 카드 (id·name·category·status만, id 오름차순). */
    @Transactional(readOnly = true)
    public List<Concept> findAllForMatching() {
        return conceptMapper.findAllForMatching();
    }

    /**
     * {@code DRAFT·AI} 카드를 만든다. 같은 이름의 카드가 이미 있으면(동시 INSERT 등) 만들지 않고 그 카드의 id를 돌려준다 (F-08 AC5).
     * 호출자의 트랜잭션에 참여한다.
     */
    @Transactional
    public long createDraft(
            String name, String category, Level difficulty, String summary, String body) {
        Concept concept = new Concept();
        concept.setName(name);
        concept.setCategory(category);
        concept.setDifficulty(difficulty);
        concept.setSummary(summary);
        concept.setBody(body);
        conceptMapper.insertDraft(concept);
        return concept.getId();
    }
}
