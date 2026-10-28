import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { setLocale } from '@/locales'
import SystemUser from '../user.vue'

vi.mock('@/api/system/password', () => ({
  resetUserPassword: vi.fn().mockResolvedValue({ code: 200 }),
}))

vi.mock('@/composables/useDict', async (importOriginal) => {
  const actual = await importOriginal()
  const { ref } = await import('vue')
  return {
    ...actual,
    useDict: () => ({
      dict: ref({
        sys_user_type: [],
        sys_user_status: [],
        sys_user_sex: [],
        sys_role_data_scope: [],
        sys_role_type: [],
        sys_normal_disable: [],
      }),
    }),
  }
})

vi.mock('@/utils', async (importOriginal) => ({
  ...(await importOriginal()),
  request: {
    get: vi.fn().mockResolvedValue({ code: 200, data: [] }),
    post: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  },
}))

const AiCrudPageStub = defineComponent({
  name: 'AiCrudPage',
  props: {
    columns: {
      type: Array,
      default: () => [],
    },
  },
  template: '<div class="ai-crud-page-stub" />',
})

const MasterDetailWorkspaceStub = defineComponent({
  name: 'MasterDetailWorkspace',
  template: '<div><slot name="aside" /><slot /></div>',
})

function mountUserPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(SystemUser, {
    global: {
      plugins: [pinia],
      stubs: {
        AiCrudPage: AiCrudPageStub,
        MasterDetailWorkspace: MasterDetailWorkspaceStub,
        PremiumTree: true,
        RolePermissionModal: true,
      },
    },
  })
}

function tableContract(wrapper) {
  const columns = wrapper.findComponent(AiCrudPageStub).props('columns')
  const organization = columns.find(column => column.prop === 'orgName')
  const userType = columns.find(column => column.prop === 'userType')
  const actions = columns.find(column => column.prop === 'action')
  return {
    organization: { prop: organization.prop, label: organization.label },
    userType: { prop: userType.prop, label: userType.label },
    actionKeys: actions.actions.map(action => action.key),
  }
}

describe('system user table column localization', () => {
  afterEach(() => setLocale('zh-CN'))

  it('updates the real column consumer across locale changes without changing keys or actions', async () => {
    setLocale('zh-CN')
    const wrapper = mountUserPage()
    await nextTick()

    const chinese = tableContract(wrapper)
    expect(chinese).toEqual({
      organization: { prop: 'orgName', label: '所属组织' },
      userType: { prop: 'userType', label: '用户类型' },
      actionKeys: ['edit', 'disable', 'enable', 'relation', 'resetPwd', 'delete'],
    })

    setLocale('en-US')
    await nextTick()
    const english = tableContract(wrapper)
    expect(english.organization).toEqual({ prop: 'orgName', label: 'Organization' })
    expect(english.userType).toEqual({ prop: 'userType', label: 'User Type' })
    expect(english.actionKeys).toEqual(chinese.actionKeys)

    setLocale('zh-CN')
    await nextTick()
    expect(tableContract(wrapper)).toEqual(chinese)

    wrapper.unmount()
  })
})
