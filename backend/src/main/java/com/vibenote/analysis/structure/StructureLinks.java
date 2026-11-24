package com.vibenote.analysis.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 읽기 단계(Java·XML·JS·HTTP)가 찾은 간선과 확인하지 못한 연결을 모은다. 같은 (from, to, type) 간선은 넣을 때 가장 작은 줄 하나로 합치고 같은
 * 확인하지 못한 연결은 한 번만 둔다 (래퍼 인라인처럼 곱으로 늘어나는 입력이 메모리를 채우지 않게). 정렬·상한은 {@link StructureAnalyzer}가 한다.
 */
public final class StructureLinks {

    private record EdgeKey(long from, long to, EdgeType type) {}

    private final Map<EdgeKey, StructureEdge> edges = new LinkedHashMap<>();
    private final Set<UnresolvedLink> unresolved = new LinkedHashSet<>();

    /** 자기 자신을 가리키는 간선은 남기지 않는다. */
    public void edge(long from, long to, EdgeType type, int line) {
        if (from != to) {
            edges.merge(
                    new EdgeKey(from, to, type),
                    new StructureEdge(from, to, type, line),
                    (a, b) -> a.line() <= b.line() ? a : b);
        }
    }

    public void unresolved(
            long fileId, int line, UnresolvedKind kind, UnresolvedReason reason, String detail) {
        unresolved.add(new UnresolvedLink(fileId, line, kind, reason, detail));
    }

    public void parseError(long fileId, int line, String detail) {
        unresolved(fileId, line, UnresolvedKind.PARSE, UnresolvedReason.PARSE_ERROR, detail);
    }

    /**
     * 파일을 읽지 못한 이유가 예산 소진이나 예상하지 못한 예외일 때 그 파일의 PARSE_ERROR (줄 1). 예외 메시지는 내부 정보라 내보내지 않는다 (5.3절 3,
     * 500 대신).
     */
    public void parseFailure(long fileId, WorkBudget budget) {
        parseError(fileId, 1, budget.exhausted() ? WorkBudget.EXHAUSTED_DETAIL : FAILED_DETAIL);
    }

    /** 읽다가 예상하지 못한 예외가 난 파일의 PARSE_ERROR detail. */
    public static final String FAILED_DETAIL = "코드를 해석하는 중 오류가 났습니다.";

    public List<StructureEdge> edges() {
        return new ArrayList<>(edges.values());
    }

    public List<UnresolvedLink> unresolved() {
        return new ArrayList<>(unresolved);
    }
}
