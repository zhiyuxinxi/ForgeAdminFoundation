import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import UserSelectModal from '../UserSelectModal.vue'
import UserSelectPicker from '../UserSelectPicker.vue'
import UserSelectPanel from '../../UserSelectPanel.vue'
import { request } from '@/utils/http'

vi.mock('@/utils/http', () => ({ request: { get: vi.fn() } }))
vi.mock('@/utils', () => ({ request: { get: vi.fn() } }))
const global = { stubs: Object.fromEntries(['NModal', 'NInput', 'NSpace', 'NButton', 'NTreeSelect', 'NDataTable', 'NPagination', 'NAlert', 'NSpin', 'NCheckboxGroup', 'NCheckbox', 'NTree'].map(name => [name, true])) }

describe('public user selection contract', () => {
  it('keeps large IDs as strings and returns selected records without changing the existing first argument', () => {
    const wrapper = shallowMount(UserSelectPanel, { props: { assignedUserIds: [7] }, global })
    const state = wrapper.vm.$.setupState
    expect(state.assignedSet.has('7')).toBe(true)
    state.userList = [{ id: '9223372036854775806', username: 'large-id' }]
    state.checkedUserIds = ['9223372036854775806']
    state.handleConfirm()
    expect(wrapper.emitted('confirm')[0]).toEqual([['9223372036854775806'], [{ id: '9223372036854775806', username: 'large-id' }]])
    wrapper.unmount()
  })

  it('keeps default selection compatible and enforces optional profile requirements at confirmation', async () => {
    const wrapper = shallowMount(UserSelectModal, { props: { multiple: true }, global })
    const state = wrapper.vm.$.setupState
    state.userList = [{ id: '1', deptName: '', postName: '' }, { id: '2', deptName: 'Operations', postName: 'Engineer' }]
    state.handleCheck(['1', '2'])
    expect(state.checkedKeys).toEqual(['1', '2'])
    await wrapper.setProps({ requireDepartment: true, requirePost: true })
    state.handleConfirm()
    expect(wrapper.emitted('confirm')[0][0]).toEqual([{ id: '2', deptName: 'Operations', postName: 'Engineer' }])
    wrapper.unmount()
  })

  it('passes the scoped reference and both optional profile requirements through the picker', () => {
    const reference = { base: '/ai/crud/order/references/ownerId', operation: 'edit' }
    const wrapper = shallowMount(UserSelectPicker, { props: { reference, requireDepartment: true, requirePost: true }, global })
    expect(wrapper.findComponent(UserSelectModal).props()).toMatchObject({ reference, requireDepartment: true, requirePost: true })
    wrapper.unmount()
  })

  it('uses scoped organization context for department eligibility while keeping unknown posts ineligible', async () => {
    const reference = { base: '/ai/crud/order/references/ownerId', operation: 'edit' }
    request.get.mockReset().mockImplementation(async (url) => {
      if (url.endsWith('/organizations'))
        return { code: 200, data: [{ orgId: '42', orgName: 'Operations' }] }
      return { code: 200, data: [{ userId: '9223372036854775806', username: 'operator', displayName: 'Operator' }] }
    })
    const wrapper = shallowMount(UserSelectModal, { props: { reference, multiple: true, requireDepartment: true }, global })
    const state = wrapper.vm.$.setupState
    await state.loadDeptTree()
    await state.loadUserList()
    expect(state.userList[0]).toMatchObject({ id: '9223372036854775806', deptId: '42', deptName: 'Operations' })
    state.handleCheck(['9223372036854775806'])
    expect(state.checkedKeys).toEqual(['9223372036854775806'])
    await wrapper.setProps({ requirePost: true })
    state.handleConfirm()
    expect(wrapper.emitted('confirm')[0][0]).toEqual([])
    expect(request.get.mock.calls.every(([url]) => url.startsWith(reference.base))).toBe(true)
    wrapper.unmount()
    request.get.mockReset()
  })
})
