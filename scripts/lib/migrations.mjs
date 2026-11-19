// Flyway 버전 번호 도우미. next-migration.mjs(로컬 확인)와 check-migrations.mjs(CI)가 함께 쓴다.
// 두 사람이 각자 브랜치에서 같은 번호(V6__a.sql, V6__b.sql)를 만들면 파일 이름이 달라 git 충돌은 없지만
// 머지 뒤 Flyway가 "같은 버전 2개"로 실패한다 (11-19 병렬 전환 때 정한 검사).
import { git } from './common.mjs';

export const MIGRATION_DIR = 'backend/src/main/resources/db/migration';

/** "V12__desc.sql" → [12], "V1_2__x.sql" → [1, 2]. 버전 파일이 아니면(R__ 등) null */
export function versionOf(filename) {
  const m = /^V(\d+(?:[._]\d+)*)__.+\.sql$/.exec(String(filename).split('/').at(-1));
  return m ? m[1].split(/[._]/).map(Number) : null;
}

export const versionText = (v) => v.join('.');

/** 버전 비교 (Flyway 규칙: 자리별 숫자 비교, 짧은 쪽은 0으로 채움) */
export function compareVersions(a, b) {
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const d = (a[i] ?? 0) - (b[i] ?? 0);
    if (d) return d;
  }
  return 0;
}

/** 파일 목록에서 가장 큰 버전. 없으면 [0] */
export function maxVersion(files) {
  return files.map(versionOf).filter(Boolean).reduce((m, v) => (compareVersions(v, m) > 0 ? v : m), [0]);
}

/**
 * PR 검사: head(PR 적용 후) 파일 목록과 base 파일 목록으로 문제를 찾는다.
 * - base에 있던 V 파일이 head에서 사라짐 (이미 적용된 마이그레이션의 이름 변경·삭제 → Flyway validate 실패)
 * - 같은 버전을 쓰는 파일이 둘 이상
 * - PR이 새로 추가한 파일의 버전이 base 최댓값 이하 (상대 PR이 먼저 머지되어 번호가 밀린 경우)
 * 내용 변경은 git이 필요해 check-migrations.mjs가 따로 본다.
 * 반환: 문제 문장 배열 (없으면 [])
 */
export function migrationProblems(headFiles, baseFiles) {
  const problems = [];
  const headSet = new Set(headFiles);
  for (const f of baseFiles) {
    if (versionOf(f) && !headSet.has(f)) problems.push(`${f}: base에 머지된 마이그레이션이 없어졌습니다 (이름 변경·삭제 금지)`);
  }
  const byVersion = new Map();
  for (const f of headFiles) {
    const v = versionOf(f);
    if (!v) continue;
    // V1과 V1.0은 Flyway에서 같은 버전이다
    const norm = versionText(v).replace(/(\.0)+$/, '');
    byVersion.set(norm, [...(byVersion.get(norm) || []), f]);
  }
  for (const [v, fs] of byVersion) {
    if (fs.length > 1) problems.push(`버전 ${v} 중복: ${fs.join(', ')}`);
  }
  const base = new Set(baseFiles);
  const baseMax = maxVersion(baseFiles);
  for (const f of headFiles) {
    const v = versionOf(f);
    if (v && !base.has(f) && compareVersions(v, baseMax) <= 0) {
      problems.push(`${f}: base의 최신 버전 V${versionText(baseMax)} 이하입니다`);
    }
  }
  return problems;
}

/** git ref의 마이그레이션 파일 이름 목록. ref가 없으면 null */
export function migrationFilesAt(ref) {
  const r = git(['ls-tree', '--name-only', `${ref}:${MIGRATION_DIR}`]);
  return r.ok ? r.out.split('\n').filter(Boolean) : null;
}
