package com.vibenote.analysis.structure;

import java.util.ArrayList;
import java.util.List;

/** 읽기 단계(Java·XML·JS·HTTP)가 찾은 간선과 확인하지 못한 연결을 모은다. 중복 제거·정렬·상한은 {@link StructureAnalyzer}가 한다. */
public final class StructureLinks {

    private final List<StructureEdge> edges = new ArrayList<>();
    private final List<UnresolvedLink> unresolved = new ArrayList<>();

    /** 자기 자신을 가리키는 간선은 남기지 않는다. */
    public void edge(long from, long to, EdgeType type, int line) {
        if (from != to) {
            edges.add(new StructureEdge(from, to, type, line));
        }
    }

    public void unresolved(
            long fileId, int line, UnresolvedKind kind, UnresolvedReason reason, String detail) {
        unresolved.add(new UnresolvedLink(fileId, line, kind, reason, detail));
    }

    public List<StructureEdge> edges() {
        return edges;
    }

    public List<UnresolvedLink> unresolved() {
        return unresolved;
    }
}
