package com.vibenote.analysis.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 읽기 단계(Java·XML·JS·HTTP)가 찾은 간선과 확인하지 못한 연결을 모은다. 같은 (from, to, type) 간선은 넣을 때 가장 작은 줄 하나로 합치고(파일
 * 쌍마다 하나라 많지 않다), 확인하지 못한 연결은 응답 순서(fileId → line → kind → reason → detail)로 정렬해 응답 상한보다 하나 많게만 둔다
 * (넘쳤는지 알기에 충분하고, 호출이 많은 입력이 메모리를 채우지 않게, 5.3절 3). 간선 정렬·상한은 {@link StructureAnalyzer}가 한다.
 */
public final class StructureLinks {

    /** 확인하지 못한 연결의 응답 순서. */
    public static final Comparator<UnresolvedLink> UNRESOLVED_ORDER =
            Comparator.comparingLong(UnresolvedLink::fileId)
                    .thenComparingInt(UnresolvedLink::line)
                    .thenComparing(UnresolvedLink::kind)
                    .thenComparing(UnresolvedLink::reason)
                    .thenComparing(UnresolvedLink::detail);

    private record EdgeKey(long from, long to, EdgeType type) {}

    private final Map<EdgeKey, StructureEdge> edges = new LinkedHashMap<>();
    private final TreeSet<UnresolvedLink> unresolved = new TreeSet<>(UNRESOLVED_ORDER);
    private final int keepUnresolved;

    /** 상한 없이 모은다 (읽기 단계 단위 테스트용). */
    public StructureLinks() {
        this(Integer.MAX_VALUE - 1);
    }

    /**
     * @param maxUnresolved 응답에 담을 확인하지 못한 연결 수. 그보다 하나 많게 보관한다
     */
    public StructureLinks(int maxUnresolved) {
        this.keepUnresolved = maxUnresolved + 1;
    }

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
        if (unresolved.size() > keepUnresolved) {
            unresolved.pollLast();
        }
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

    /** 응답 순서로 정렬된 확인하지 못한 연결 (상한보다 하나 많게까지). */
    public List<UnresolvedLink> unresolved() {
        return new ArrayList<>(unresolved);
    }
}
