import { mount } from '@vue/test-utils'
import { afterEach, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import { setLocale } from '@/locales'
import FileUpload from '../index.vue'

vi.mock('@/store', () => ({ useAuthStore: () => ({ accessToken: 'test-token' }) }))
vi.mock('@/composables/useStorageConfig', () => ({ useStorageConfig: () => ({ storageType: ref('local'), fileSize: ref(10), loadConfig: vi.fn() }) }))
vi.mock('@/utils', () => ({ downloadFile: vi.fn(), generateUUID: () => 'test-nonce', getFileUrl: () => '', request: { get: vi.fn(), put: vi.fn() }, resolveRenderableFileUrl: async () => '' }))

afterEach(() => {
  setLocale('zh-CN')
  delete window.$message
})

it('sends the selected upload language and keeps file restrictions effective', async () => {
  window.$message = { error: vi.fn() }
  const wrapper = mount(FileUpload, {
    props: { fileType: ['pdf'], fileSize: 2 },
    global: { stubs: { NIcon: true, NText: true, NModal: true, NInput: true } },
  })
  const upload = wrapper.findComponent({ name: 'Upload' })
  expect(upload.props('headers')['Accept-Language']).toBe('zh-CN')
  setLocale('en-US')
  await nextTick()
  expect(upload.props('headers')['Accept-Language']).toBe('en-US')
  expect(upload.props('onBeforeUpload')({ file: { name: '原名.exe', file: { size: 1 } } })).toBe(false)
  expect(window.$message.error).toHaveBeenLastCalledWith('Upload a file in one of these formats: pdf')
  expect(upload.props('onBeforeUpload')({ file: { name: '原名.pdf', file: { size: 3 * 1024 * 1024 } } })).toBe(false)
  expect(window.$message.error).toHaveBeenLastCalledWith('Files must not exceed 2 MB')
  expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  wrapper.unmount()
})
