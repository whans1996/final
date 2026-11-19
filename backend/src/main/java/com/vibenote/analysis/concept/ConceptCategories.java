package com.vibenote.analysis.concept;

import java.util.Set;

/** AI 초안 카드가 가질 수 있는 카테고리 (F-08 AC5). FE의 utils/conceptCategories.js(B)와 같은 6종이다. */
public final class ConceptCategories {

    public static final Set<String> ALL =
            Set.of("java", "spring", "web", "security", "mybatis", "vue");

    private ConceptCategories() {}
}
