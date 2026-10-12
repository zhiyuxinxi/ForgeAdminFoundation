import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, expect, it, vi } from 'vitest'
import { setLocale } from '@/locales'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import TenantDatasourceGrants from '../TenantDatasourceGrants.vue'

vi.mock('@/utils', () => ({ request: { get: vi.fn(), post: vi.fn() } }))
const wrappers = []

function panel(admin) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const user = useUserStore()
  user.setUser({ id: 100, tenantId: 7, admin })
  const wrapper = shallowMount(TenantDatasourceGrants, {
    global: { plugins: [pinia], stubs: { NModal: true, NDataTable: true } },
  })
  wrappers.push(wrapper)
  return { wrapper, state: wrapper.vm.$.setupState, user }
}

afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.resetAllMocks()
  setLocale('zh-CN')
})

it.each(['zh-CN', 'en-US'])('maintains a grant through the real tenant endpoint in %s', async (locale) => {
  setLocale(locale)
  const { wrapper, state, user } = panel(true)
  const option = { datasourceId: 3, datasourceCode: 'business-a', selectable: true, granted: false }
  request.get.mockResolvedValue({ code: 200, data: [option] })
  request.post.mockResolvedValue({ code: 200 })
  await state.open({ id: 7 })
  expect(request.get).toHaveBeenCalledWith('/system/tenant/7/datasource-grants')
  expect(wrapper.findComponent({ name: 'NModal' }).attributes('title'))
    .toBe(locale === 'en-US' ? 'Business datasource grants' : '业务数据源授权')
  await state.setGrant(option, true)
  expect(request.post).toHaveBeenCalledWith('/system/tenant/7/datasource-grants/3', null, { params: { granted: true } })
  expect(option.granted).toBe(true)
  option.selectable = false
  await state.setGrant(option, false)
  expect(option.granted).toBe(false)
  const calls = request.post.mock.calls.length
  await state.setGrant(option, true)
  expect(request.post).toHaveBeenCalledTimes(calls)
  user.userInfo.admin = false
  option.selectable = true
  await state.setGrant(option, true)
  expect(request.post).toHaveBeenCalledTimes(calls)
})

it('does not let a tenant administrator fetch grant choices or post grants', async () => {
  const { state } = panel(false)
  await state.open({ id: 7 })
  await state.setGrant({ datasourceId: 3, selectable: true }, true)
  expect(request.get).not.toHaveBeenCalled()
  expect(request.post).not.toHaveBeenCalled()
})
