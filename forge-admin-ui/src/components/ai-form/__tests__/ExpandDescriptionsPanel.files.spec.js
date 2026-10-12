import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'
import { setLocale } from '@/locales'
import ExpandDescriptionsPanel from '../expand-renderers/ExpandDescriptionsPanel.vue'

const mocks = vi.hoisted(() => ({ get: vi.fn(), download: vi.fn(), resolve: vi.fn() }))
vi.mock('@/store', () => ({ useAuthStore: () => ({ accessToken: 'test-token' }) }))
vi.mock('@/composables/useStorageConfig', () => ({ useStorageConfig: () => ({
  storageType: ref('local'),
  fileSize: ref(10),
  loadConfig: vi.fn(),
}) }))
vi.mock('@/utils', () => ({
  downloadFile: mocks.download,
  generateUUID: () => 'test-nonce',
  getFileUrl: () => '',
  request: { get: mocks.get, put: vi.fn() },
  resolveRenderableFileUrl: mocks.resolve,
}))
vi.mock('@/components/DictTag.vue', () => ({ default: { template: '<span />' } }))
vi.mock('@/components/common/AuthImage.vue', () => ({ default: { template: '<span />' } }))

const wrappers = []
const slotStub = { template: '<div><slot /></div>' }
function details(value) {
  const wrapper = mount(ExpandDescriptionsPanel, {
    props: {
      panel: { descriptions: { fields: [{ field: 'attachments', render: { type: 'fileUpload' } }] } },
      data: { attachments: value },
    },
    global: {
      stubs: {
        NDescriptions: slotStub,
        NDescriptionsItem: slotStub,
        NUpload: true,
        NIcon: true,
        NModal: true,
        NInput: true,
        NText: true,
      },
    },
  })
  wrappers.push(wrapper)
  return wrapper
}

beforeEach(() => {
  vi.resetAllMocks()
  mocks.get.mockImplementation(async url => ({ code: 200, data: { originalName: `${url.split('/').at(-1)}.pdf` } }))
  mocks.download.mockResolvedValue('downloaded')
  window.$message = { error: vi.fn() }
})

afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  setLocale('zh-CN')
  delete window.$message
})

describe('readable description attachments', () => {
  it.each([
    ['comma-separated IDs', ' 90071992547409931, 90071992547409932 '],
    ['ID array', ['90071992547409931', '90071992547409932']],
    ['metadata array', [
      { fileId: '90071992547409931', originalName: '90071992547409931.pdf' },
      { fileId: '90071992547409932', originalName: '90071992547409932.pdf' },
    ]],
  ])('displays %s through the existing read-only file list', async (_, value) => {
    const wrapper = details(value)
    await flushPromises()
    expect(wrapper.findAll('.file-card')).toHaveLength(2)
    expect(wrapper.text()).toContain('90071992547409931.pdf')
    expect(wrapper.findComponent(FileUpload).props()).toMatchObject({ disabled: true, showTip: false })
    expect(wrapper.findAll('.upload-dropzone, .file-rename-trigger, .file-action-danger, .upload-tip')).toHaveLength(0)
    expect(mocks.resolve).not.toHaveBeenCalled()
    expect(mocks.download).not.toHaveBeenCalled()
    await wrapper.find('.file-actions [title="下载"]').trigger('click')
    expect(mocks.download).toHaveBeenCalledWith('90071992547409931', '90071992547409931.pdf')
    expect(wrapper.findComponent(FileUpload).emitted('update:modelValue')).toBeUndefined()
  })

  it('renders empty values without an upload control or the previous attachment', async () => {
    const wrapper = details('old-id')
    await flushPromises()
    expect(wrapper.text()).toContain('old-id.pdf')
    for (const attachments of [null, '', []]) {
      await wrapper.setProps({ data: { attachments } })
      await flushPromises()
      expect(wrapper.text()).toBe('-')
      expect(wrapper.findComponent(FileUpload).exists()).toBe(false)
    }
  })

  it('clears the previous record immediately and ignores stale metadata after a failed read', async () => {
    const wrapper = details('first-id')
    await flushPromises()
    let resolveStale
    mocks.get.mockImplementationOnce(() => new Promise((resolve) => {
      resolveStale = resolve
    }))
    await wrapper.setProps({ data: { attachments: 'stale-id' } })
    expect(wrapper.text()).not.toContain('first-id.pdf')
    mocks.get.mockRejectedValueOnce(new Error('metadata unavailable'))
    await wrapper.setProps({ data: { attachments: 'current-id' } })
    await flushPromises()
    expect(wrapper.text()).toContain('current-id')
    resolveStale({ code: 200, data: { originalName: 'stale-name.pdf' } })
    await flushPromises()
    expect(wrapper.text()).not.toContain('stale-name.pdf')
    expect(wrapper.findAll('.file-card')).toHaveLength(1)
    await wrapper.find('.file-actions [title="下载"]').trigger('click')
    expect(mocks.download).toHaveBeenLastCalledWith('current-id', 'current-id')
  })

  it('keeps existing bilingual download feedback and user filenames unchanged', async () => {
    const wrapper = details([{ fileId: 'file-id', originalName: '用户原名.pdf' }])
    await flushPromises()
    expect(wrapper.find('.file-actions [title="下载"]').exists()).toBe(true)
    setLocale('en-US')
    await flushPromises()
    expect(wrapper.text()).toContain('用户原名.pdf')
    mocks.download.mockRejectedValueOnce(new Error('permission denied'))
    await wrapper.find('.file-actions [title="Download"]').trigger('click')
    await flushPromises()
    expect(window.$message.error).toHaveBeenCalledWith('Download failed: permission denied')
    expect(wrapper.findAll('.file-card')).toHaveLength(1)
  })
})
