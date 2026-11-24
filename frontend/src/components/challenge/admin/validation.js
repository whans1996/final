// 관리자 챌린지 폼 클라이언트 검증 (F-24 AC13). 규칙과 문구는 BE AdminChallengeRequest·AdminChallengeService와 같다.
// 서버도 같은 검증을 하므로, 여기서는 요청 전에 바로 고칠 수 있는 것을 알려 주는 용도다.
// 오늘(options.today)은 호출하는 쪽이 utils/seoulDate의 todayInSeoul()로 넘긴다 (F-25 AC16).
import { daysBetween } from '@/utils/seoulDate'

export const TITLE_MAX = 100
export const DESCRIPTION_MAX = 2000
export const PERIOD_MAX_DAYS = 90
export const GOAL_VALUE_MIN = 1
export const GOAL_VALUE_MAX = 1000
export const IMAGE_MAX_BYTES = 2 * 1024 * 1024

// 확장자 → MIME. BE ChallengeImageStorage와 같다 (매직 바이트는 서버가 본다)
const IMAGE_TYPES = { jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', webp: 'image/webp' }
export const IMAGE_ACCEPT = '.jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp'

export const MESSAGES = {
  titleRequired: '제목을 입력하세요.',
  titleTooLong: `제목은 ${TITLE_MAX}자 이하로 입력하세요.`,
  descriptionTooLong: '설명은 2,000자 이하로 입력하세요.',
  startRequired: '시작일을 입력하세요.',
  endRequired: '종료일을 입력하세요.',
  endBeforeStart: '종료일은 시작일과 같거나 뒤여야 합니다.',
  periodTooLong: '기간은 시작일과 종료일을 포함해 90일 이하로 정하세요.',
  endInPast: '종료일은 오늘이거나 오늘 이후여야 합니다.',
  goalTypeRequired: '목표 유형을 고르세요.',
  goalValueInvalid: '목표값은 1~1,000 사이의 정수로 입력하세요.',
  imageEmpty: '빈 이미지 파일은 올릴 수 없습니다.',
  imageTooLarge: '이미지는 2MB 이하만 올릴 수 있습니다.',
  imageNotAllowed: 'jpg·png·webp 이미지만 올릴 수 있습니다.',
}

const DATE = /^\d{4}-\d{2}-\d{2}$/

/** 이미지 파일 사유. 문제가 없으면 null. */
export function imageProblem(file) {
  if (file.size === 0) return MESSAGES.imageEmpty
  if (file.size > IMAGE_MAX_BYTES) return MESSAGES.imageTooLarge
  const ext = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : ''
  return IMAGE_TYPES[ext] && IMAGE_TYPES[ext] === file.type ? null : MESSAGES.imageNotAllowed
}

/**
 * 폼 값 → { 필드: 사유 }. 비어 있으면 통과.
 * @param {{ title: string, description: string, startDate: string, endDate: string, goalType: string, goalValue: string|number, image: File|null }} form
 * @param {{ today: string, originalEndDate?: string|null }} options 수정이면 원래 종료일. 종료일 ≥ 오늘은 등록과 종료일을 바꿀 때만 본다
 */
export function validateChallengeForm(form, { today, originalEndDate = null }) {
  const errors = {}
  const title = form.title.trim()
  if (!title) errors.title = MESSAGES.titleRequired
  else if (title.length > TITLE_MAX) errors.title = MESSAGES.titleTooLong
  if (form.description.trim().length > DESCRIPTION_MAX) {
    errors.description = MESSAGES.descriptionTooLong
  }

  const start = DATE.test(form.startDate) ? form.startDate : ''
  const end = DATE.test(form.endDate) ? form.endDate : ''
  if (!start) errors.startDate = MESSAGES.startRequired
  if (!end) errors.endDate = MESSAGES.endRequired
  else if (start && end < start) errors.endDate = MESSAGES.endBeforeStart
  else if (start && daysBetween(start, end) + 1 > PERIOD_MAX_DAYS) {
    errors.endDate = MESSAGES.periodTooLong
  } else if (end !== originalEndDate && end < today) errors.endDate = MESSAGES.endInPast

  if (!form.goalType) errors.goalType = MESSAGES.goalTypeRequired
  const goal = String(form.goalValue).trim()
  if (!/^\d+$/.test(goal) || +goal < GOAL_VALUE_MIN || +goal > GOAL_VALUE_MAX) {
    errors.goalValue = MESSAGES.goalValueInvalid
  }

  if (form.image) {
    const problem = imageProblem(form.image)
    if (problem) errors.image = problem
  }
  return errors
}
