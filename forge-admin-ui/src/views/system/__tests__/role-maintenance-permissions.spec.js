import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import SystemRole from '../role.vue'

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
  useRoute: () => ({ query: {}, params: {}, path: '/system/role', meta: {} }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

const wrappers = []
let originalMessage
beforeEach(() => {
  originalMessage = window.$message
  window.$message = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }
})
afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.clearAllMocks()
  window.$message = originalMessage
})

function rolePage(userInfo) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useUserStore().setUser({ id: 100, tenantId: 7, roleIds: [31], ...userInfo })
  const wrapper = shallowMount(SystemRole, {
    global: { plugins: [pinia] },
  })
  wrappers.push(wrapper)
  return wrapper.vm.$.setupState
}

describe('role maintenance policy consumed by the real role page', () => {
  it('offers tenant administrators edit, scope, authorization and delete for another tenant role', () => {
    const page = rolePage({ userType: 1 })
    expect(page.getRoleActionOptions({ id: 42, tenantId: 7, isSystem: 0 }).map(item => item.key))
      .toEqual(['edit', 'org-scope', 'auth', 'delete-divider', 'delete'])
  })

  it.each([
    [{ userType: 2, permissions: ['system:role:edit'] }, { id: 42, tenantId: 7 }],
    [{ userType: 1 }, { id: 42, tenantId: 99 }],
    [{ userType: 1 }, { id: 42, tenantId: 7, isSystem: 1 }],
    [{ userType: 1 }, { id: '31', tenantId: '7' }],
    [{ userType: 1, tenantId: null }, { id: 42, tenantId: null }],
  ])('hides and rejects direct maintenance action for protected role %#', async (user, role) => {
    const page = rolePage(user)
    expect(page.getRoleActionOptions(role)).toEqual([])
    await page.handleRoleCardAction('auth', role)
    expect(request.post).not.toHaveBeenCalled()
  })

  it('retains super administrator maintenance of a built-in role in another tenant', () => {
    const page = rolePage({ userType: 0, admin: true })
    expect(page.getRoleActionOptions({ id: 42, tenantId: 99, isSystem: 1 }).map(item => item.key))
      .toContain('edit')
  })
  it('uses user edit for organization-role writes while preserving administrators and user wildcards', async () => {
    const page = rolePage({ userType: 2, permissions: ['system:role:edit'] })
    await flushPromises()
    const user = useUserStore()
    page.currentRole = { id: 42, tenantId: 7 }
    page.roleUserOrgId = 12
    request.post.mockClear()
    expect(page.hasUserEditPermission()).toBe(false)
    await page.handleConfirmAddUsers([99])
    await page.handleRemoveUserRole({ id: 99 })
    expect(request.post).not.toHaveBeenCalled()
    for (const permission of ['system:user:edit', '*', '*:*:*', 'system:*', 'system:user:*']) {
      user.userInfo.permissions = [permission]
      expect(page.hasUserEditPermission()).toBe(true)
    }
    for (const identity of [{ admin: true }, { tenantAdmin: true }]) {
      user.setUser({ id: 100, tenantId: 7, userType: 2, permissions: [], ...identity })
      expect(page.hasUserEditPermission()).toBe(true)
    }
    user.setUser({ id: 100, tenantId: 7, userType: 2, permissions: ['system:user:edit'] })
    await page.handleConfirmAddUsers([99])
    expect(request.post).toHaveBeenCalledWith('/system/user/99/org-roles', {
      tenantId: 7,
      orgId: 12,
      roleIds: [42],
    })
  })

  it('rechecks user edit after the remove confirmation and after the add lookup', async () => {
    const page = rolePage({ userType: 2, permissions: ['system:user:edit'] })
    await flushPromises()
    const user = useUserStore()
    page.currentRole = { id: 42, tenantId: 7 }
    page.roleUserOrgId = 12
    const originalDialog = window.$dialog
    const warning = vi.fn()
    window.$dialog = { ...originalDialog, warning }
    try {
      request.post.mockClear()
      await page.handleRemoveUserRole({ id: 99, username: 'fixture' })
      const confirmation = warning.mock.calls[0][0]
      user.userInfo.permissions = ['system:role:edit']
      await confirmation.onPositiveClick()
      expect(request.post).not.toHaveBeenCalled()

      user.userInfo.permissions = ['system:user:edit']
      request.get.mockResolvedValueOnce({ code: 200, data: [42, 43] })
      await page.handleRemoveUserRole({ id: 99, username: 'fixture' })
      await warning.mock.calls[1][0].onPositiveClick()
      expect(request.post).toHaveBeenCalledWith('/system/user/99/org-roles', {
        tenantId: 7,
        orgId: 12,
        roleIds: [43],
      })

      request.post.mockClear()
      request.get.mockImplementationOnce(async () => {
        user.userInfo.permissions = ['system:role:edit']
        return { code: 200, data: [] }
      })
      await page.handleConfirmAddUsers([99])
      expect(request.post).not.toHaveBeenCalled()
    }
    finally {
      window.$dialog = originalDialog
    }
  })
})
