// 파일 레이어의 화면 라벨과 순서 (docs/specs/F-06-overview-explanation.md 5.5절).
// 순서는 F-05a 선별 우선순위(controller → service → mapper → vue → js → config → domain → other)와 같다.

export const LAYER_ORDER = [
  'controller',
  'service',
  'mapper',
  'vue',
  'js',
  'config',
  'domain',
  'other',
]

export const LAYER_LABELS = {
  controller: '컨트롤러',
  service: '서비스',
  mapper: '매퍼(DB)',
  vue: '화면(Vue)',
  js: '스크립트',
  config: '설정',
  domain: '도메인·DTO',
  other: '기타',
}

export function layerLabel(layer) {
  return LAYER_LABELS[layer] ?? layer
}

function rank(layer) {
  const index = LAYER_ORDER.indexOf(layer)
  return index === -1 ? LAYER_ORDER.length : index
}

/** `layer` 필드를 가진 항목을 레이어 순서로 정렬한다 (같은 레이어는 원래 순서 유지). */
export function sortByLayer(items) {
  return [...items].sort((a, b) => rank(a.layer) - rank(b.layer))
}

/** 항목을 레이어별로 묶어 `[{ layer, label, items }]`를 레이어 순서로 돌려준다. 묶음 안 순서는 입력 순서. */
export function groupByLayer(items) {
  const groups = new Map()
  for (const item of items) {
    if (!groups.has(item.layer)) groups.set(item.layer, [])
    groups.get(item.layer).push(item)
  }
  return sortByLayer([...groups.keys()].map((layer) => ({ layer }))).map(({ layer }) => ({
    layer,
    label: layerLabel(layer),
    items: groups.get(layer),
  }))
}
