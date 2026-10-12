import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { h, nextTick } from 'vue'
import AiModal from '../index.vue'

const wrappers = []
const stubs = {
  NModal: { name: 'NModal', props: ['show', 'maskClosable', 'closeOnEsc'], template: '<section v-if="show"><slot /></section>' },
  NCard: { name: 'NCard', props: ['closable'], template: '<article><slot name="header" /><slot /><slot name="footer" /></article>' },
  NButton: { name: 'NButton', props: ['disabled', 'loading'], template: '<button :disabled="disabled || loading"><slot /></button>' },
}

function modal() {
  const wrapper = mount(AiModal, {
    props: { draggable: false },
    slots: { default: () => h('input', { 'aria-label': 'Draft' }) },
    global: { stubs },
  })
  wrappers.push(wrapper)
  return wrapper
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}

afterEach(() => {
  wrappers.splice(0).forEach(wrapper => wrapper.unmount())
  vi.restoreAllMocks()
})

describe('aiModal submission lifecycle', () => {
  it('runs one submission and prevents cancel, close, Escape and replacing its input while pending', async () => {
    const request = deferred()
    const onOk = vi.fn(() => request.promise)
    const onCancel = vi.fn()
    const onClose = vi.fn()
    const wrapper = modal()
    await wrapper.vm.open({ onOk, onCancel, onClose, maskClosable: true })
    await wrapper.get('input').setValue('Unsaved draft')
    const submitted = wrapper.vm.handleOk()
    await nextTick()
    expect(wrapper.vm.okLoading).toBe(true)
    expect(wrapper.findComponent({ name: 'NModal' }).props()).toMatchObject({ show: true, maskClosable: false, closeOnEsc: false })
    expect(wrapper.findComponent({ name: 'NCard' }).props('closable')).toBe(false)
    expect(wrapper.findAll('button').every(button => button.attributes('disabled') !== undefined)).toBe(true)
    await wrapper.vm.handleOk()
    await wrapper.vm.handleCancel()
    wrapper.vm.close()
    wrapper.findComponent({ name: 'NModal' }).vm.$emit('update:show', false)
    await wrapper.vm.open({ title: 'Replacement' })
    expect(onOk).toHaveBeenCalledOnce()
    expect(onCancel).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
    expect(wrapper.vm.show).toBe(true)
    expect(wrapper.get('input').element.value).toBe('Unsaved draft')
    request.resolve(true)
    await submitted
    expect(wrapper.vm.show).toBe(false)
    expect(wrapper.vm.okLoading).toBe(false)
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('retains the input after a failed request, releases loading and allows retry', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const onOk = vi.fn().mockRejectedValueOnce(new Error('Save failed')).mockResolvedValueOnce(true)
    const wrapper = modal()
    await wrapper.vm.open({ onOk })
    await wrapper.get('input').setValue('Keep this draft')
    await wrapper.vm.handleOk()
    expect(wrapper.vm.show).toBe(true)
    expect(wrapper.vm.okLoading).toBe(false)
    expect(wrapper.get('input').element.value).toBe('Keep this draft')
    await wrapper.vm.handleOk()
    expect(onOk).toHaveBeenCalledTimes(2)
    expect(wrapper.vm.show).toBe(false)
  })

  it('keeps callback vetoes open and releases externally assigned loading in finally', async () => {
    const wrapper = modal()
    await wrapper.vm.open({
      onOk: () => {
        wrapper.vm.okLoading = true
        return false
      },
    })
    await wrapper.get('input').setValue('Needs correction')
    await wrapper.vm.handleOk()
    expect(wrapper.vm.show).toBe(true)
    expect(wrapper.vm.okLoading).toBe(false)
    expect(wrapper.get('input').element.value).toBe('Needs correction')
    wrapper.vm.close()
    expect(wrapper.vm.show).toBe(false)
  })

  it('honors an asynchronous cancel veto without allowing overlapping actions', async () => {
    const request = deferred()
    const onOk = vi.fn()
    const onCancel = vi.fn(() => request.promise)
    const wrapper = modal()
    await wrapper.vm.open({ onOk, onCancel })
    const cancelled = wrapper.vm.handleCancel()
    await wrapper.vm.handleCancel()
    await wrapper.vm.handleOk()
    expect(onCancel).toHaveBeenCalledOnce()
    expect(onOk).not.toHaveBeenCalled()
    request.resolve(false)
    await cancelled
    expect(wrapper.vm.show).toBe(true)
    wrapper.vm.close()
    expect(wrapper.vm.show).toBe(false)
  })
})
