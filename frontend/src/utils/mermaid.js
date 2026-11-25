// 구조 그래프 → mermaid 원본 (docs/specs/F-21a-structure-graph.md AC14·AC17). 순수 함수만 둔다.
import { groupByLayer } from '@/utils/layers'

const LABEL_ENTITIES = {
  '"': '#quot;',
  ';': '#59;',
  '#': '#35;',
  '<': '#lt;',
  '>': '#gt;',
  '`': '#96;',
  '%': '#37;',
}

/**
 * 따옴표 라벨 안에서 mermaid 문법을 깨는 문자를 엔티티로, 줄바꿈을 공백으로 바꾼다 (한 번에 바꿔 엔티티를 다시 바꾸지 않는다).
 * `%`는 `%%{…}%%`가 라벨 안에서도 설정(directive)으로 읽혀 뒤의 원본을 지우므로 바꾼다.
 */
export function escapeMermaidLabel(text) {
  return String(text ?? '')
    .replace(/\r\n|\r|\n/g, ' ')
    .replace(/[";#<>`%]/g, (ch) => LABEL_ENTITIES[ch])
}

function layerId(layer) {
  return `L_${String(layer).replace(/[^A-Za-z0-9_]/g, '_')}`
}

function edgeLine({ from, to, type }) {
  return type === 'HTTP' ? `  f${from} -.->|HTTP| f${to}` : `  f${from} --> f${to}`
}

/**
 * `flowchart LR` 원본. 레이어별 subgraph(LAYER_ORDER 순)에 간선이 있는 파일만 넣고, 노드 id는 `f{fileId}`다.
 * IMPORT·MYBATIS는 실선, HTTP는 `HTTP` 라벨의 점선이다. 같은 두 파일·같은 종류의 간선은 한 번만 그린다.
 */
export function buildStructureMermaid({ nodes, edges }) {
  const known = new Set(nodes.map((n) => n.fileId))
  const drawn = new Map()
  for (const edge of edges) {
    if (!known.has(edge.from) || !known.has(edge.to)) continue
    const key = `${edge.from}|${edge.to}|${edge.type}`
    if (!drawn.has(key)) drawn.set(key, edge)
  }
  const linked = new Set([...drawn.values()].flatMap((e) => [e.from, e.to]))

  const lines = ['flowchart LR']
  for (const group of groupByLayer(nodes.filter((n) => linked.has(n.fileId)))) {
    lines.push(`  subgraph ${layerId(group.layer)}["${escapeMermaidLabel(group.label)}"]`)
    for (const n of group.items) lines.push(`    f${n.fileId}["${escapeMermaidLabel(n.name)}"]`)
    lines.push('  end')
  }
  for (const edge of drawn.values()) lines.push(edgeLine(edge))
  return lines.join('\n')
}
