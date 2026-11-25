package com.vibenote.analysis.structure.js;

import java.util.List;

/**
 * 내보낸 이름 하나 (F-21a AC7·AC8·AC10). 지역 이름이나 식 중 하나다.
 *
 * @param local {@code export const X}·{@code export function f}·{@code export default X}·{@code
 *     export { a as b }}의 지역 이름
 * @param expr {@code export default 식}의 식 토큰
 */
public record JsExport(String local, List<JsToken> expr) {}
