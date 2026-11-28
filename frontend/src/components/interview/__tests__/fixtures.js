// I-1a 면접 질문 응답 예시 (docs/specs/I-1a-interview-prep.md 3절). 테스트용으로 직접 만든 값이다.

export function question(partial = {}) {
  return {
    id: 301,
    no: 1,
    category: 'STRUCTURE',
    question: '글 작성 기능을 controller·service·mapper로 나눈 이유는 무엇인가요?',
    keyPoints: ['컨트롤러는 요청 검증만 한다', '서비스가 트랜잭션을 맡는다'],
    evidence: [
      { fileId: 101, path: 'src/main/java/todo/TodoController.java', startLine: 12, endLine: 30 },
      { fileId: 102, path: 'src/main/java/todo/TodoService.java', startLine: 7, endLine: 7 },
    ],
    concepts: [{ id: 12, name: '계층형 아키텍처' }],
    ...partial,
  }
}

export function interview(partial = {}) {
  return {
    analysisId: 42,
    status: 'NONE',
    errorCode: null,
    errorMessage: null,
    regenerationsLeft: 2,
    generatedAt: null,
    questions: null,
    ...partial,
  }
}

export function doneInterview(partial = {}) {
  return interview({
    status: 'DONE',
    generatedAt: '2026-11-30T14:05:00+09:00',
    questions: [
      question({ id: 302, no: 2, category: 'FLOW', question: '흐름 질문' }),
      question({ id: 301, no: 1, category: 'STRUCTURE', question: '구조 질문' }),
      question({ id: 303, no: 3, category: 'LIMITATION', question: '한계 질문', concepts: [] }),
    ],
    ...partial,
  })
}
