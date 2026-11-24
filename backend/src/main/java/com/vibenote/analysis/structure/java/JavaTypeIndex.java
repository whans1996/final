package com.vibenote.analysis.structure.java;

import java.util.Map;
import java.util.Optional;

/**
 * 선별된 Java 파일이 선언한 타입 FQN(중첩 타입 포함) → 파일 ID와 프로젝트 루트 패키지 (F-21a AC1·AC2). MyBatis XML 읽기(AC4)도 쓴다.
 *
 * @param rootPackage 선별된 모든 Java 파일 패키지의 공통 접두. 2단 미만이면 빈 문자열
 */
public record JavaTypeIndex(Map<String, Long> types, String rootPackage) {

    public Optional<Long> fileOf(String fqn) {
        return Optional.ofNullable(types.get(fqn));
    }

    /** FQN이 루트 패키지 아래인가. 루트 패키지가 없으면 항상 false (AC2). */
    public boolean inProject(String fqn) {
        return !rootPackage.isEmpty() && fqn.startsWith(rootPackage + ".");
    }
}
