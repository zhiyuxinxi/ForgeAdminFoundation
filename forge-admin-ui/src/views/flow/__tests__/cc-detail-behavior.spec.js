import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import CcPage from '../cc.vue'

const mocks = vi.hoisted(() => ({
  getMyCc: vi.fn(),
  getSentCc: vi.fn(),
  getCcFormInfo: vi.fn(),
  markCcRead: vi.fn(),
  businessContext: vi.fn(),
}))
vi.mock('@/api/flow', () => ({ default: mocks }))
vi.mock('@/api/business-app', () => ({ businessCcTaskFormReadonlyContext: mocks.businessContext }))
vi.mock('@/store', () => ({ useUserStore: () => ({ isAdmin: true, getDataPermission: [] }) }))
vi.mock('@/composables/useDict', () => ({ useDict: () => ({ dict: ref({ flow_read_status: [] }), getLabel: () => 'Read' }) }))
vi.mock('@/components/common/FlowBusinessForm.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/common/UserAvatar.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/flow/FlowTaskCardList.vue', () => ({ default: { render: () => null } }))

const wrappers = []
function page() {
  const wrapper = shallowMount(CcPage, {
    global: {
      stubs: {
        NModal: { props: ['show'], template: '<section v-if="show"><slot name="header" /><slot /></section>' },
        NTabs: true, NTab: true, NButton: true, NSpace: true, NSelect: true, NEmpty: true, NSpin: true, AiForm: true,
      },
      renderStubDefaultSlot: true,
    },
  })
  wrappers.push(wrapper)
  return wrapper
}

beforeEach(() => {
  mocks.getMyCc.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
  mocks.getSentCc.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
})
afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.resetAllMocks()
})

describe('CC detail recipient boundary', () => {
  it('opens sent metadata without requesting recipient forms or marking the record read', async () => {
    const wrapper = page()
    const setup = wrapper.vm.$.setupState
    setup.activeTab = 'sent'
    await setup.openCcDetail({ id: 'sent-1', ccUserName: 'Recipient', content: 'Sent notice', isRead: 0 })
    await flushPromises()
    expect(wrapper.text()).toContain('Recipient')
    expect(wrapper.text()).toContain('Sent notice')
    expect(wrapper.find('.cc-business-form-panel').exists()).toBe(false)
    expect(mocks.getCcFormInfo).not.toHaveBeenCalled()
    expect(mocks.businessContext).not.toHaveBeenCalled()
    expect(mocks.markCcRead).not.toHaveBeenCalled()
    expect(setup.ccFormLoading).toBe(false)
  })

  it('still requests the received record through the recipient-only business form context', async () => {
    mocks.getCcFormInfo.mockResolvedValue({ code: 200, data: { formMode: 'BUSINESS_CODE_FORM' } })
    mocks.businessContext.mockResolvedValue({ code: 200, data: { configured: true, formType: 'business-code', fields: [], recordData: { name: 'Received record' } } })
    const wrapper = page()
    await wrapper.vm.$.setupState.openCcDetail({ id: 'received-1', isRead: 1 })
    await flushPromises()
    expect(mocks.getCcFormInfo).toHaveBeenCalledWith('received-1')
    expect(mocks.businessContext).toHaveBeenCalledWith('received-1')
    expect(wrapper.find('.cc-business-form-panel').exists()).toBe(true)
    expect(wrapper.vm.$.setupState.ccBusinessFormData).toEqual({ name: 'Received record' })
  })

  it('ignores an outstanding received-form response after opening the same record in sent', async () => {
    let resolveForm
    mocks.getCcFormInfo.mockReturnValue(new Promise(resolve => { resolveForm = resolve }))
    const wrapper = page()
    const setup = wrapper.vm.$.setupState
    await setup.openCcDetail({ id: 'shared-id', isRead: 1 })
    setup.activeTab = 'sent'
    await setup.openCcDetail({ id: 'shared-id', content: 'Sent metadata', isRead: 1 })
    resolveForm({ code: 200, data: { formMode: 'BUSINESS_CODE_FORM' } })
    await flushPromises()
    expect(mocks.getCcFormInfo).toHaveBeenCalledTimes(1)
    expect(mocks.businessContext).not.toHaveBeenCalled()
    expect(setup.ccFormInfo).toBeNull()
    expect(setup.ccFormLoading).toBe(false)
    expect(wrapper.find('.cc-business-form-panel').exists()).toBe(false)
  })
})
