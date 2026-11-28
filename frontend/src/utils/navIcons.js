// meta.nav.icon 문자열 → Lucide 컴포넌트 (F-29b 5.1). route 파일은 이름만 적고 셸이 여기서 그린다.
// 새 아이콘은 이 맵에 한 줄 더한다. 맵에 없는 이름은 Circle로 그린다.
import {
  BookOpen,
  Circle,
  FilePlus2,
  FileSearch,
  FolderGit2,
  LayoutDashboard,
  MessagesSquare,
  Mic,
  NotebookPen,
  RotateCcw,
  Rss,
  Trophy,
  Users,
} from 'lucide-vue-next'

export const NAV_ICONS = {
  LayoutDashboard,
  FilePlus2,
  FileSearch,
  FolderGit2,
  RotateCcw,
  NotebookPen,
  BookOpen,
  Users,
  Trophy,
  MessagesSquare,
  Rss,
  Mic,
}

/** 아이콘 이름을 Lucide 컴포넌트로 바꾼다. 모르는 이름·빈 값은 Circle */
export function navIcon(name) {
  return NAV_ICONS[name] ?? Circle
}
