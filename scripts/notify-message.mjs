#!/usr/bin/env node
// GitHub Actions(notify.yml)용: 이벤트(GITHUB_EVENT_NAME·GITHUB_EVENT_PATH)를 읽어 Mattermost 웹훅 본문 JSON을 출력한다.
// 알릴 것이 없으면 아무것도 출력하지 않는다. 로컬 확인: GITHUB_EVENT_NAME=push GITHUB_EVENT_PATH=<json> node scripts/notify-message.mjs
import { readFileSync } from 'node:fs';
import { nowFileOf, readText } from './lib/common.mjs';
import { loadPcs } from './lib/env-check.mjs';
import { buildMessage, changedNowKeys } from './lib/notify.mjs';

const eventName = process.env.GITHUB_EVENT_NAME;
const payload = JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'));

const nowTexts = {};
if (eventName === 'push') for (const k of changedNowKeys(payload)) nowTexts[k] = readText(nowFileOf(k));

const text = buildMessage(eventName, payload, { pcs: loadPcs(), nowTexts });
if (text) process.stdout.write(JSON.stringify({ username: 'VibeNote', text }));
