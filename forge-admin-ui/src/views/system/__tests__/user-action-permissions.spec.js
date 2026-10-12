import { flushPromises, mount, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import { setLocale } from '@/locales'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import SystemUser from '../user.vue'

vi.mock('@/api/system/password', () => ({
  resetUserPassword: vi.fn().mockResolvedValue({ code: 200 }),
}))
vi.mock('@/composables/useDict', async (importOriginal) => {
  const actual = await importOriginal()
  const { ref } = await import('vue')
  return { ...actual, useDict: () => ({ dict: ref({}) }) }
})
vi.mock('@/utils', async importOriginal => ({
  ...(await importOriginal()),
  request: Object.assign(vi.fn().mockResolvedValue({ code: 200, data: { records: [], total: 0 } }), {
    get: vi.fn().mockResolvedValue({ code: 200, data: [] }),
    post: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  }),
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {}, params: {}, path: '/system/user', meta: {} }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

const WorkspaceStub = defineComponent({
  name: 'MasterDetailWorkspace',
  template: '<div><slot /></div>',
})
const AiTableStub = defineComponent({
  name: 'AiTable',
  setup(_, { expose }) {
    expose({ getCheckedRows: () => [], clearSelection: () => {} })
    return () => null
  },
})
const uiStubs = Object.fromEntries([
  'NCard',
  'NSpace',
  'NButton',
  'NIcon',
  'NDropdown',
  'NTooltip',
  'NModal',
  'NDrawer',
  'NDrawerContent',
  'NSpin',
  'NTabs',
  'NTabPane',
  'NAlert',
  'NTag',
  'NProgress',
  'NInput',
  'NEmpty',
  'NDataTable',
  'NScrollbar',
  'NResult',
  'NFormItem',
  'NSelect',
  'NForm',
  'NInputNumber',
  'NTree',
  'NCheckbox',
  'NDatePicker',
  'NPopconfirm',
].map(name => [name, true]))
const wrappers = []
let originalMessage
beforeEach(() => {
  originalMessage = window.$message
  window.$message = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }
})

function userPage(userInfo) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const user = useUserStore()
  user.setUser({ id: 8, tenantId: 1, ...userInfo })
  const page = shallowMount(SystemUser, {
    global: {
      plugins: [pinia],
      stubs: { ...uiStubs, AiCrudPage: false, AiTable: AiTableStub, MasterDetailWorkspace: WorkspaceStub },
    },
  })
  const crud = page.findComponent(AiCrudPage)
  const props = crud.props()
  wrappers.push(page)
  const actions = props.columns.find(column => column.prop === 'action').actions
  return { page, crud, actions, user }
}

function actionText(crud, actions, row = { id: 9, userStatus: 1 }) {
  const view = mount({ render: () => crud.vm.$.setupState.renderActionColumn(row, actions, 10) })
  const text = view.findAll('a').map(link => link.text()).join('') || view.text()
  view.unmount()
  return text
}

afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.clearAllMocks()
  window.$message = originalMessage
  setLocale('zh-CN')
})

describe('system user action permission consumers', () => {
  it('hides the scoped write actions from a read-only user, including function-visible row actions', async () => {
    const { page, crud, actions, user } = userPage({ permissions: ['system:user:list', 'system:user:query'] })
    expect(actionText(crud, actions)).toBe('用户关联配置')
    expect(crud.vm.$.setupState.hasActionPermission('add')).toBe(false)
    expect(crud.vm.$.setupState.hasActionPermission('batchDelete')).toBe(false)
    await crud.vm.$.setupState.handleCustomActionClick(actions.find(action => action.key === 'resetPwd'), { id: 9 })
    expect(page.vm.$.setupState.resetPwdModalVisible).toBe(false)

    user.userInfo.permissions.push('system:user:edit')
    await nextTick()
    expect(actionText(crud, actions)).toContain('重置密码')
    user.userInfo.permissions = []
    await nextTick()
    expect(actionText(crud, actions)).toBe('用户关联配置')
  })

  it('keeps add, edit and remove grants independent and preserves original target restrictions', async () => {
    const { crud, actions, user } = userPage({ permissions: ['system:user:add'] })
    expect(crud.vm.$.setupState.hasActionPermission('add')).toBe(true)
    expect(actionText(crud, actions)).toBe('用户关联配置')
    user.userInfo.permissions = ['system:user:edit']
    await nextTick()
    expect(actionText(crud, actions)).toBe('编辑禁用用户关联配置重置密码')
    expect(actionText(crud, actions, { id: 8, userStatus: 1 })).toBe('编辑')
    expect(actionText(crud, actions, { id: 9, userStatus: 0 })).toBe('编辑启用用户关联配置重置密码')
    user.userInfo.permissions = ['system:user:remove']
    await nextTick()
    expect(crud.vm.$.setupState.hasActionPermission('batchDelete')).toBe(true)
    expect(actionText(crud, actions)).toBe('用户关联配置删除')
    expect(actionText(crud, actions, { id: 1, userStatus: 1 })).toBe('用户关联配置')
    expect(actionText(crud, actions, { id: 8, userStatus: 1 })).toBe('-')
  })

  it.each([{ admin: true }, { tenantAdmin: true }])(
    'preserves trusted administrator or wildcard management access: %j',
    (identity) => {
      const { crud, actions } = userPage(identity)
      expect(crud.vm.$.setupState.hasActionPermission('add')).toBe(true)
      expect(crud.vm.$.setupState.hasActionPermission('batchDelete')).toBe(true)
      expect(actionText(crud, actions)).toBe('编辑禁用用户关联配置重置密码删除')
      expect(actionText(crud, actions, { id: 8, userStatus: 1 })).toBe('编辑')
    },
  )

  it.each(['*', '*:*:*', 'system:*', 'system:user:*'])(
    'matches the backend wildcard grant %s without changing target restrictions',
    async (permission) => {
      const { page, crud, actions } = userPage({ permissions: [permission] })
      expect(crud.props('hideAdd')).toBe(false)
      expect(crud.props('hideBatchDelete')).toBe(false)
      expect(actionText(crud, actions)).toBe('编辑禁用用户关联配置重置密码删除')
      expect(actionText(crud, actions, { id: 8, userStatus: 1 })).toBe('编辑')
      await crud.vm.$.setupState.handleCustomActionClick(actions.find(action => action.key === 'resetPwd'), { id: 9 })
      expect(page.vm.$.setupState.resetPwdModalVisible).toBe(true)
    },
  )

  it.each([
    { permissions: ['**'] },
    { permissions: ['other:*', 'system:user:query'] },
    { apiPermissions: ['system:user:edit', 'system:user:add', 'system:user:remove'] },
    { dataPermission: ['system:user:edit', 'system:user:add', 'system:user:remove'] },
  ])('rejects grants that are not backend user-management permissions: %j', async (identity) => {
    const { page, crud, actions } = userPage(identity)
    expect(crud.props('hideAdd')).toBe(true)
    expect(crud.props('hideBatchDelete')).toBe(true)
    expect(actionText(crud, actions)).toBe('用户关联配置')
    await crud.vm.$.setupState.handleCustomActionClick(actions.find(action => action.key === 'resetPwd'), { id: 9 })
    expect(page.vm.$.setupState.resetPwdModalVisible).toBe(false)
  })

  it('checks current reactive permission before an old rendered action runs', async () => {
    const { page, crud, actions, user } = userPage({ permissions: ['system:user:*'] })
    const reset = actions.find(action => action.key === 'resetPwd')
    expect(actionText(crud, actions)).toContain('重置密码')
    user.userInfo.permissions = ['**']
    await nextTick()
    await crud.vm.$.setupState.handleCustomActionClick(reset, { id: 9 })
    expect(page.vm.$.setupState.resetPwdModalVisible).toBe(false)
    expect(actionText(crud, actions)).toBe('用户关联配置')
    expect(crud.props('hideAdd')).toBe(true)
    expect(crud.props('hideBatchDelete')).toBe(true)
  })
  it('gates only relationship writes by user edit and submits the actual organization and role URLs', async () => {
    const { page, actions, user } = userPage({ permissions: ['system:user:query'] })
    await flushPromises()
    const state = page.vm.$.setupState
    state.currentUser = { id: 9, tenantId: 1 }
    state.authOrgId = 12
    state.checkedRoleKeys = [21]
    state.checkedOrgKeys = [12]
    state.mainOrgId = 12
    state.selectedUserIds = [9]
    state.batchAuthTenantId = 1
    state.batchAuthOrgId = 12
    request.post.mockClear()

    expect(actions.find(action => action.key === 'relation').visible({ id: 9 })).toBe(true)
    expect(state.userBatchActionOptions.find(action => action.key === 'auth').disabled).toBe(true)
    await state.handleSubmitAuth()
    await state.handleSubmitOrg()
    await state.handleSubmitBatchAuth()
    expect(request.post).not.toHaveBeenCalled()

    user.userInfo.permissions = ['system:user:edit']
    await nextTick()
    expect(state.userBatchActionOptions.find(action => action.key === 'auth').disabled).toBe(false)
    await state.handleSubmitAuth()
    await state.handleSubmitOrg()
    await state.handleSubmitBatchAuth()
    expect(window.$message.error).not.toHaveBeenCalled()
    expect(request.post.mock.calls.map(call => call[0])).toEqual([
      '/system/user/9/org-roles',
      '/system/user/9/orgs',
      '/system/user/9/org-roles',
    ])
    expect(request.post.mock.calls[0][1]).toEqual({ orgId: 12, roleIds: [21], tenantId: 1 })
    expect(request.post.mock.calls[1][1]).toEqual({ orgIds: [12], mainOrgId: 12 })
  })

  it('rejects relationship submits after permission revocation including the pending batch read', async () => {
    const { page, user } = userPage({ permissions: ['system:user:edit'] })
    await flushPromises()
    const state = page.vm.$.setupState
    state.currentUser = { id: 9, tenantId: 1 }
    state.authOrgId = 12
    state.checkedRoleKeys = [21]
    state.checkedOrgKeys = [12]
    state.mainOrgId = 12
    state.selectedUserIds = [9]
    state.batchAuthTenantId = 1
    state.batchAuthOrgId = 12
    request.post.mockClear()
    request.get.mockImplementationOnce(async () => {
      user.userInfo.permissions = ['system:role:edit']
      return { code: 200, data: [] }
    })
    await state.handleSubmitBatchAuth()
    await state.handleSubmitAuth()
    await state.handleSubmitOrg()
    expect(request.post).not.toHaveBeenCalled()
  })
})
