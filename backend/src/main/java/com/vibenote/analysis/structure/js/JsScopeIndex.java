package com.vibenote.analysis.structure.js;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * 같은 이름 선언들의 범위 색인 (F-21a 5.3절 3). offset에서 보이는 선언 중 범위 시작이 가장 늦은 것(같으면 범위가 좁은 것, 그것도 같으면 먼저 등록한
 * 것)을 찾는다.
 *
 * <p>선언을 (시작 오름차순, 끝 내림차순, 등록 역순)으로 두고 각 선언의 부모를 "앞에서 그 범위를 감싸는 가장 안쪽 선언"으로 잇는다. 조회는 시작이 offset 이하인
 * 마지막 선언에서 부모 쪽으로 올라가며 끝이 offset보다 뒤인 첫 선언을 찾는다. 부모 쪽으로 갈수록 끝이 늦어지므로(단조) 건너뛰기 포인터(Myers)로 O(log
 * 깊이)에 찾는다. 범위가 일부만 겹치면(짝이 맞지 않는 괄호) 앞 선언은 부모 후보에서 빠지지만, 그 선언이 답이 되는 offset은 그 선언보다 앞에서 시작하는 조회뿐이라
 * 결과는 같다.
 */
final class JsScopeIndex {

    private final JsDecl[] decls;
    private final int[] starts;
    private final int[] ends;
    private final int[] parent;
    private final int[] jump;

    /**
     * @param named 등록 순서의 같은 이름 선언들
     */
    JsScopeIndex(List<JsDecl> named) {
        int n = named.size();
        Integer[] order = new Integer[n];
        for (int k = 0; k < n; k++) {
            order[k] = k;
        }
        Arrays.sort(
                order,
                Comparator.<Integer>comparingInt(k -> named.get(k).scopeStart())
                        .thenComparing(k -> named.get(k).scopeEnd(), Comparator.reverseOrder())
                        .thenComparing(Comparator.reverseOrder()));
        decls = new JsDecl[n];
        starts = new int[n];
        ends = new int[n];
        parent = new int[n];
        jump = new int[n];
        int[] depth = new int[n];
        Deque<Integer> open = new ArrayDeque<>();
        for (int k = 0; k < n; k++) {
            JsDecl decl = named.get(order[k]);
            decls[k] = decl;
            starts[k] = decl.scopeStart();
            ends[k] = decl.scopeEnd();
            while (!open.isEmpty() && ends[open.peek()] < ends[k]) {
                open.pop();
            }
            int p = open.isEmpty() ? -1 : open.peek();
            parent[k] = p;
            if (p < 0) {
                depth[k] = 0;
                jump[k] = k;
            } else {
                depth[k] = depth[p] + 1;
                int jp = jump[p];
                jump[k] = depth[p] - depth[jp] == depth[jp] - depth[jump[jp]] ? jump[jp] : p;
            }
            open.push(k);
        }
    }

    /** offset에서 보이는 가장 안쪽 선언. 없으면 null. */
    JsDecl lookup(int offset) {
        int x = lastStartingAtOrBefore(offset);
        while (x >= 0 && ends[x] <= offset) {
            int j = jump[x];
            x = j != x && ends[j] <= offset ? j : parent[x];
        }
        return x < 0 ? null : decls[x];
    }

    private int lastStartingAtOrBefore(int offset) {
        int lo = 0;
        int hi = starts.length - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (starts[mid] <= offset) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found;
    }
}
