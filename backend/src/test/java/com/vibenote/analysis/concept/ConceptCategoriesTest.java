package com.vibenote.analysis.concept;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** K-1 AC5(카테고리 12종)·AC8(별칭 정규화). */
class ConceptCategoriesTest {

    @Test
    void AC5_카테고리는_12종이고_순서가_정해져_있다() {
        assertThat(ConceptCategories.ORDERED)
                .containsExactly(
                        "java",
                        "spring",
                        "web",
                        "security",
                        "mybatis",
                        "vue",
                        "database",
                        "javascript",
                        "tooling",
                        "typescript",
                        "react",
                        "cs");
        assertThat(ConceptCategories.ALL)
                .containsExactlyInAnyOrderElementsOf(ConceptCategories.ORDERED);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "java, java",
        "Java, java",
        "  React  , react",
        "JS, javascript",
        "Node.js, javascript",
        "ECMAScript, javascript",
        "TS, typescript",
        "Type-Script, typescript",
        "Spring Boot, spring",
        "spring_framework, spring",
        "Vue.js, vue",
        "Pinia, vue",
        "React.js, react",
        "MySQL, database",
        "SQL, database",
        "JPA, database",
        "JDBC, database",
        "RDBMS, database",
        "Docker, tooling",
        "Git, tooling",
        "Gradle, tooling",
        "npm, tooling",
        "Vite, tooling",
        "DevOps, tooling",
        "JVM, java",
        "CS, cs",
    })
    void AC8_소문자로_바꾸고_공백_점_하이픈_밑줄을_뗀_뒤_별칭을_바꾼다(String raw, String expected) {
        assertThat(ConceptCategories.normalize(raw)).contains(expected);
    }

    @Test
    void AC8_12종과_별칭_밖이면_빈_값이다() {
        for (String raw : List.of("kotlin", "python", "", "   ", "java script x")) {
            assertThat(ConceptCategories.normalize(raw)).as(raw).isEmpty();
        }
        assertThat(ConceptCategories.normalize(null)).isEmpty();
    }
}
