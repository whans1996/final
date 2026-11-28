// 알림 종류 → Lucide 아이콘 (NT-1 AC15). 모르는 종류(A NT-2가 서버에 먼저 더한 것 등)는 Bell.
// 새 종류의 아이콘을 바꾸고 싶으면 여기에 한 줄 더한다.
import { Bell, MessageSquare, Trophy, UserPlus } from 'lucide-vue-next'

const ICONS = {
  COMMENT: MessageSquare,
  FOLLOW: UserPlus,
  CHALLENGE_COMPLETE: Trophy,
}

export function notificationIcon(type) {
  return ICONS[type] ?? Bell
}
