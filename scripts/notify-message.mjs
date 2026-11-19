#!/usr/bin/env node
// GitHub Actions(notify.yml)용: 이벤트(GITHUB_EVENT_NAME·GITHUB_EVENT_PATH)를 읽어 Mattermost 웹훅 본문 JSON을 출력한다.
// 알릴 것이 없으면 아무것도 출력하지 않는다. 로컬 확인: GITHUB_EVENT_NAME=push GITHUB_EVENT_PATH=<json> node scripts/notify-message.mjs
import { readFileSync } from 'node:fs';
import { git, nowFileOf, readText } from './lib/common.mjs';
import { loadPcs } from './lib/env-check.mjs';
import { buildMessage, changedNowKeys } from './lib/notify.mjs';

const eventName = process.env.GITHUB_EVENT_NAME;
const payload = JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'));
const pcs = loadPcs();

// Actions의 push payload에는 커밋별 added·modified가 없다 → 커밋 id로 조회 (checkout fetch-depth: 0 필요).
// webhook 형식으로 파일 목록이 이미 있으면 그대로 쓴다 (로컬 fixture)
const filesOf = (c) =>
  c.added || c.modified ? [...(c.added || []), ...(c.modified || [])] : git(['show', '--name-only', '--format=', c.id]).out.split('\n').filter(Boolean);

const nowTexts = {};
if (eventName === 'push') for (const k of changedNowKeys(payload, pcs, filesOf)) nowTexts[k] = readText(nowFileOf(k));

const text = buildMessage(eventName, payload, { pcs, nowTexts, filesOf });
if (text) process.stdout.write(JSON.stringify({ username: 'VibeNote', text }));
