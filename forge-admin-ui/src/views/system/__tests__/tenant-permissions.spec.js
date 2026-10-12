import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import { setLocale } from '@/locales'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import SystemTenant from '../tenant.vue'

vi.mock('@/composables/useDict', async () => {
  const { ref } = await import('vue')
  return { useDict: () => ({ dict: ref({}) }) }
})
vi.mock('@/utils', async importOriginal => ({
  ...(await importOriginal()),
  request: Object.assign(vi.fn().mockResolvedValue({ code: 200, data: { records: [], total: 0 } }), {
    get: vi.fn().mockResolvedValue({ code: 200, data: { records: [], total: 0 } }),
    post: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  }),
}))

const wrappers = []
const platformFields = ['tenantStatus', 'userLimit', 'expireTime', 'defaultBusinessDatasourceId', 'defaultBusinessDatasourceCode']

function tenantPage(identity = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const user = useUserStore()
  user.setUser({ id: 100, tenantId: 7, ...identity })
  window.$dialog = { warning: vi.fn() }
  window.$message = { success: vi.fn(), error: vi.fn(), warning: vi.fn() }
  const page = shallowMount(SystemTenant, {
    global: {
      plugins: [pinia],
      stubs: Object.fromEntries([
        'NModal',
        'NSpace',
        'NButton',
        'NSpin',
        'NDataTable',
        'NInput',
        'NSelect',
        'NIcon',
        'NDivider',
      ].map(name => [name, true])),
    },
  })
  wrappers.push(page)
  const crud = page.findComponent(AiCrudPage)
  const state = page.vm.$.setupState
  const actions = crud.props('columns').find(column => column.prop === 'action').actions
  return { page, crud, state, actions, user }
}

afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.clearAllMocks()
  setLocale('zh-CN')
})

describe('tenant administration permission consumers', () => {
  it.each(['zh-CN', 'en-US'])('denies ordinary members and keeps the rejection localized in %s', async (locale) => {
    setLocale(locale)
    const { crud, state, actions } = tenantPage({ permissions: ['*'] })
    expect(crud.props('hideAdd')).toBe(true)
    expect(crud.props('hideBatchDelete')).toBe(true)
    expect(actions.filter(action => action.visible({ id: 7 }))).toEqual([])
    await state.handleViewUsers({ id: 7 })
    state.handleEdit({ id: 7 })
    expect(state.usersModalVisible).toBe(false)
    expect(() => crud.props('beforeSubmit')({ id: 7, tenantStatus: 0 }))
      .toThrow(locale === 'en-US' ? 'You do not have access to this tenant' : '无权访问该租户')
    expect(request.get).not.toHaveBeenCalled()
    expect(request.post).not.toHaveBeenCalled()
  })

  it('shows tenant administrators only tenant fields and submits no copied platform fields', () => {
    const { crud, actions } = tenantPage({ tenantAdmin: true })
    const fields = crud.props('editSchema').map(item => item.field)
    expect(fields).toContain('systemName')
    expect(fields).toContain('contactPerson')
    platformFields.forEach(field => expect(fields).not.toContain(field))
    expect(actions.filter(action => action.visible({ id: 7 })).map(action => action.key)).toEqual(['users', 'edit'])
    const original = {
      id: 7,
      tenantStatus: 1,
      userLimit: 100,
      expireTime: '2030-01-01',
      defaultBusinessDatasourceId: 3,
      defaultBusinessDatasourceCode: 'existing-business',
      systemName: 'Acme 中文',
      contactPerson: '张三',
    }
    const submitted = crud.props('beforeSubmit')({ ...original })
    platformFields.forEach(field => expect(submitted).not.toHaveProperty(field))
    expect(submitted.systemName).toBe('Acme 中文')
    expect(submitted.contactPerson).toBe('张三')
    expect(original.defaultBusinessDatasourceCode).toBe('existing-business')
    expect(request.get).not.toHaveBeenCalled()
  })

  it('rejects another tenant and rejects an open form after administrator status is removed', async () => {
    const { crud, actions, user } = tenantPage({ tenantAdmin: true })
    expect(actions.filter(action => action.visible({ id: 99 }))).toEqual([])
    expect(() => crud.props('beforeSubmit')({ id: 99 })).toThrow()
    user.userInfo.tenantAdmin = false
    await nextTick()
    expect(() => crud.props('beforeSubmit')({ id: 7, systemName: 'Unauthorized' })).toThrow()
  })

  it('allows ordinary member removal, protects privileged and current users, and checks confirmation again', async () => {
    const { state, user } = tenantPage({ tenantAdmin: true })
    state.currentTenant = { id: 7, tenantName: 'Current tenant' }
    const action = state.userTableColumns.find(column => column.key === 'actions')
    expect(action.render({ id: 200, userType: 2 }).type).toBe('a')
    expect(action.render({ id: 200, userType: 0 })).toBe('-')
    expect(action.render({ id: 200, userType: 1 })).toBe('-')
    expect(action.render({ id: 200 })).toBe('-')
    expect(action.render({ id: 100, userType: 2 }).type).toBe('span')
    state.handleRemoveTenantUser({ id: 200, userType: 2, username: 'member' })
    const confirm = window.$dialog.warning.mock.calls[0][0].onPositiveClick
    user.userInfo.tenantAdmin = false
    await nextTick()
    await confirm()
    expect(request.post).not.toHaveBeenCalled()
  })

  it('keeps platform fields and cross-tenant actions available to the super administrator', async () => {
    const { crud, state, actions } = tenantPage({ admin: true })
    const fields = crud.props('editSchema').map(item => item.field)
    expect(fields).toEqual(expect.arrayContaining(platformFields.filter(field => field !== 'defaultBusinessDatasourceCode')))
    expect(actions.filter(action => action.visible({ id: 99 })).map(action => action.key)).toEqual(['users', 'edit', 'datasources', 'delete'])
    expect(crud.props('hideAdd')).toBe(false)
    expect(crud.props('hideBatchDelete')).toBe(false)
    await nextTick()
    state.businessDatasources = [{ datasourceId: 3, datasourceCode: 'trusted-business' }]
    state.businessDatasourceTenant = 99
    const submitted = crud.props('beforeSubmit')({ id: 99, userLimit: 100, defaultBusinessDatasourceId: 3 })
    expect(submitted.userLimit).toBe(100)
    expect(submitted.defaultBusinessDatasourceCode).toBe('trusted-business')
  })

  it('loads grants for the edited tenant and rejects stale or missing authorization instead of clearing it', async () => {
    const { crud, state } = tenantPage({ admin: true })
    request.get.mockResolvedValueOnce({ code: 200, data: [{ datasourceId: 3, datasourceCode: 'trusted-business' }] })
    await crud.props('beforeRenderDetail')({ id: 7 })
    expect(request.get).toHaveBeenCalledWith('/system/tenant/7/datasources')
    expect(crud.props('beforeSubmit')({ id: 7, defaultBusinessDatasourceId: 3 }).defaultBusinessDatasourceCode)
      .toBe('trusted-business')
    expect(() => crud.props('beforeSubmit')({ id: 99, defaultBusinessDatasourceId: 3 })).toThrow()
    state.businessDatasources = []
    expect(() => crud.props('beforeSubmit')({ id: 7, defaultBusinessDatasourceId: 3 })).toThrow()
    expect(crud.props('beforeSubmit')({ id: 7, defaultBusinessDatasourceId: null }).defaultBusinessDatasourceCode)
      .toBe('')
  })
})
