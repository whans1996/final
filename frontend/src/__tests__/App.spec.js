import { describe, expect, it } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import App from '@/App.vue'
import { useAuthStore } from '@/stores/auth'
import { mountAt } from '@/views/__tests__/viewTestUtils'

function loginAs(role) {
  return () =>
    useAuthStore().setSession({
      accessToken: 'access-1',
      user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role },
    })
}

async function headerLinks(options) {
  const { wrapper } = await mountAt(App, '/', options)
  await flushPromises()
  return { wrapper, links: wrapper.findAll('header nav a').map((a) => a.text()) }
}

// F-00b AC8: 헤더 메뉴는 route meta.nav로 만들고, 닉네임·로그아웃은 App이 고정으로 붙인다
describe('App 헤더 (F-00b AC8)', () => {
  it('AC8 비로그인은 개념 카드·커뮤니티·로그인·회원가입을 본다 (F-09 AC9, F-17 AC13)', async () => {
    const { wrapper, links } = await headerLinks()

    expect(links).toEqual(['개념 카드', '커뮤니티', '로그인', '회원가입'])
    expect(wrapper.find('header button').exists()).toBe(false)
  })

  it('AC8 로그인하면 대시보드(F-16 AC9)·개념 카드·커뮤니티(F-17 AC13)·피드(F-04 AC11)·분석 요청·내 분석·복습(F-15 AC12), 닉네임, 로그아웃을 본다 (관리자 없음)', async () => {
    const { wrapper, links } = await headerLinks({ beforeNavigate: loginAs('USER') })

    expect(links).toEqual([
      '대시보드',
      '개념 카드',
      '커뮤니티',
      '피드',
      '분석 요청',
      '내 분석',
      '복습',
      '코딩초보',
    ])
    expect(wrapper.find('header button').text()).toBe('로그아웃')
  })

  it('AC8 ADMIN은 관리자 메뉴가 /admin으로 더해진다', async () => {
    const { wrapper, links } = await headerLinks({ beforeNavigate: loginAs('ADMIN') })

    expect(links).toEqual([
      '대시보드',
      '개념 카드',
      '커뮤니티',
      '피드',
      '분석 요청',
      '내 분석',
      '복습',
      '관리자',
      '코딩초보',
    ])
    expect(wrapper.find('header a[href="/admin"]').text()).toBe('관리자')
  })
})
