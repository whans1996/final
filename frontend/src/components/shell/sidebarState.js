// 옆 메뉴 접힘 상태 (F-29b AC3). 새로고침 뒤에도 유지되게 localStorage에 둔다.
import { ref } from 'vue'

export const SIDEBAR_KEY = 'vibenote.sidebar'

function readStored() {
  try {
    return localStorage.getItem(SIDEBAR_KEY) === 'collapsed'
  } catch {
    return false // 저장소가 막힌 브라우저
  }
}

/** 지금 저장된 값으로 접힘 상태를 만든다. setCollapsed는 저장까지 한다 */
export function useSidebarState() {
  const collapsed = ref(readStored())
  function setCollapsed(value) {
    collapsed.value = value
    try {
      localStorage.setItem(SIDEBAR_KEY, value ? 'collapsed' : 'expanded')
    } catch {
      // 저장하지 못해도 이번 방문에서는 반영한다
    }
  }
  return { collapsed, setCollapsed }
}
