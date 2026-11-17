#!/usr/bin/env node
// PostToolUse(Edit|Write|MultiEdit) hook: frontend 파일을 수정하면 그 파일에만 Prettier를 적용한다.
// Java 포맷은 Gradle이 느려서 여기서 하지 않는다 (verify --fix / spotlessApply).
// 실패해도 작업을 막지 않는다 (항상 exit 0).
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { isAbsolute, join, relative, resolve } from 'node:path';
import { ROOT, isWin, readStdinJson } from '../../scripts/lib/common.mjs';

const input = readStdinJson();
const filePath = input?.tool_input?.file_path;
if (!filePath) process.exit(0);

const abs = isAbsolute(filePath) ? filePath : resolve(input.cwd || ROOT, filePath);
const rel = relative(ROOT, abs).replace(/\\/g, '/');
if (!rel.startsWith('frontend/') || !/\.(vue|js|mjs|ts|css|scss|json|html)$/.test(rel)) process.exit(0);

const prettier = join(ROOT, 'frontend', 'node_modules', '.bin', isWin ? 'prettier.cmd' : 'prettier');
if (!existsSync(prettier) || !existsSync(abs)) process.exit(0);

spawnSync(`"${prettier}" --write --log-level warn "${abs}"`, {
  cwd: join(ROOT, 'frontend'),
  shell: true,
  windowsHide: true,
  timeout: 20_000,
});
process.exit(0);
