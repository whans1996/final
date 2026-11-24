import { describe, expect, it } from 'vitest'
import { buildStructureMermaid, escapeMermaidLabel } from '@/utils/mermaid'

function node(fileId, name, layer) {
  return { fileId, path: `src/${name}`, name, layer, role: null }
}

describe('mermaid 유틸 (F-21a)', () => {
  it('AC17 라벨의 특수 문자를 엔티티로, 줄바꿈을 공백으로 바꾼다', () => {
    expect(escapeMermaidLabel('a"b;c#d<e>f`g')).toBe('a#quot;b#59;c#35;d#lt;e#gt;f#96;g')
    expect(escapeMermaidLabel('줄\r\n바꿈\n과\r끝')).toBe('줄 바꿈 과 끝')
    // 엔티티를 넣은 뒤 그 안의 #·;을 다시 바꾸지 않는다
    expect(escapeMermaidLabel('#;')).toBe('#35;#59;')
    expect(escapeMermaidLabel('TodoView.vue')).toBe('TodoView.vue')
  })

  it('AC14·AC17 레이어 순서 subgraph, f{fileId} 노드, 실선·HTTP 점선 간선을 만든다', () => {
    const code = buildStructureMermaid({
      nodes: [
        node(106, 'TodoView.vue', 'vue'),
        node(105, 'todoApi.js', 'js'),
        node(101, 'TodoController.java', 'controller'),
        node(102, 'TodoService.java', 'service'),
        node(103, 'TodoMapper.java', 'mapper'),
        node(104, 'TodoMapper.xml', 'mapper'),
      ],
      edges: [
        { from: 101, to: 102, type: 'IMPORT', line: 18 },
        { from: 103, to: 104, type: 'MYBATIS', line: 1 },
        { from: 105, to: 101, type: 'HTTP', line: 7 },
        { from: 106, to: 105, type: 'IMPORT', line: 3 },
      ],
    })

    expect(code.split('\n')).toEqual([
      'flowchart LR',
      '  subgraph L_controller["컨트롤러"]',
      '    f101["TodoController.java"]',
      '  end',
      '  subgraph L_service["서비스"]',
      '    f102["TodoService.java"]',
      '  end',
      '  subgraph L_mapper["매퍼(DB)"]',
      '    f103["TodoMapper.java"]',
      '    f104["TodoMapper.xml"]',
      '  end',
      '  subgraph L_vue["화면(Vue)"]',
      '    f106["TodoView.vue"]',
      '  end',
      '  subgraph L_js["스크립트"]',
      '    f105["todoApi.js"]',
      '  end',
      '  f101 --> f102',
      '  f103 --> f104',
      '  f105 -.->|HTTP| f101',
      '  f106 --> f105',
    ])
  })

  it('AC14 간선이 없는 파일은 그림에서 빼고, 같은 두 파일의 같은 종류 간선은 한 번만 그린다', () => {
    const code = buildStructureMermaid({
      nodes: [
        node(1, 'A.java', 'service'),
        node(2, 'B.java', 'service'),
        node(3, 'C.java', 'domain'),
      ],
      edges: [
        { from: 1, to: 2, type: 'IMPORT', line: 3 },
        { from: 1, to: 2, type: 'IMPORT', line: 9 },
      ],
    })

    expect(code).not.toContain('f3')
    expect(code).not.toContain('L_domain')
    expect(code.match(/f1 --> f2/g)).toHaveLength(1)
  })

  it('AC17 파일 이름은 이스케이프하고, 레이어 id에는 영문·숫자·_만 남긴다', () => {
    const code = buildStructureMermaid({
      nodes: [node(7, 'a"b<c>.js', 'js'), node(8, '이상한;이름#.vue', 'we ird-layer')],
      edges: [{ from: 8, to: 7, type: 'IMPORT', line: 1 }],
    })

    expect(code).toContain('f7["a#quot;b#lt;c#gt;.js"]')
    expect(code).toContain('f8["이상한#59;이름#35;.vue"]')
    expect(code).toContain('subgraph L_we_ird_layer["we ird-layer"]')
    // 알 수 없는 레이어는 알려진 레이어 뒤에 온다
    expect(code.indexOf('L_js')).toBeLessThan(code.indexOf('L_we_ird_layer'))
  })
})
