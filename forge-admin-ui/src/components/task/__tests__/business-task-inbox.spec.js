import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { openLinkedBusinessTask } from '@/api/business-task'
import BusinessTaskInbox from '../BusinessTaskInbox.vue'

const mocks = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), user: { userId: '20' } }))
vi.mock('@/utils/http', () => ({ request: { get: mocks.get, post: mocks.post } }))
vi.mock('@/store', () => ({ useUserStore: () => mocks.user }))
vi.mock('@/components/ai-form/AiForm.vue', () => ({ default: {
  name: 'AiForm',
  props: ['schema', 'value'],
  emits: ['update:value'],
  methods: { validate: async () => {} },
  template: '<form><label v-for="field in schema" :key="field.field">{{ field.label }}<input :disabled="field.disabled" :value="value[field.field]" @input="$emit(\'update:value\', { ...value, [field.field]: $event.target.value })"></label></form>',
} }))
const Wrap = { template: '<div><slot /></div>' }
const Button = { props: ['disabled', 'loading'], template: '<button :disabled="disabled || loading"><slot /></button>' }
const Modal = { props: ['show'], template: '<div v-if="show"><slot /></div>' }
const Select = { props: ['options'], template: '<div />' }
const stubs = { NSpace: Wrap, NList: Wrap, NListItem: Wrap, NSpin: Wrap, NAlert: Wrap, NEmpty: Wrap, NButton: Button, NModal: Modal, NSelect: Select }
const task = { id: '9007199254740997', title: '检查现场', assigneeId: '20', assigneeName: '张工', sourceType: 'lowcode.record', state: 'ASSIGNED', sourceKey: 'do-not-display-source', resultJson: '{"hidden":"raw-json"}' }

function prepare() {
  mocks.get.mockImplementation(async (url) => {
    if (url === '/api/task/assigned')
      return { code: 200, data: [task] }
    if (url.endsWith('/form'))
      return { code: 200, data: { fields: [{ field: 'comment', label: '检查结论', type: 'textarea', required: true }], values: {} } }
    return { code: 200, data: task }
  })
  mocks.post.mockImplementation(async url => ({ code: 200, data: { ...task, state: url.endsWith('/start') ? 'IN_PROGRESS' : 'COMPLETED' } }))
}

describe('business task inbox', () => {
  beforeEach(() => {
    mocks.get.mockReset()
    mocks.post.mockReset()
    mocks.user.userId = '20'
    prepare()
  })

  it('discovers assigned tasks, starts and completes with mapped form values through stable APIs', async () => {
    const wrapper = mount(BusinessTaskInbox, { global: { stubs } })
    await flushPromises()
    await wrapper.get(`[data-task-id="${task.id}"]`).trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('检查结论')
    expect(wrapper.text()).not.toContain('do-not-display-source')
    expect(wrapper.text()).not.toContain('raw-json')
    await wrapper.get('[data-start]').trigger('click')
    await flushPromises()
    await wrapper.get('input').setValue('符合要求')
    await wrapper.get('[data-complete]').trigger('click')
    await flushPromises()
    expect(mocks.post.mock.calls[0][0]).toBe(`/api/task/${task.id}/start`)
    expect(mocks.post.mock.calls[1]).toEqual([`/api/task/${task.id}/complete`, { requestKey: expect.any(String), resultJson: '{"comment":"符合要求"}' }])
    expect(wrapper.find('[data-complete]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('shows business errors delivered with HTTP 200 and preserves a completion key across retries', async () => {
    mocks.get.mockImplementation(async url => ({ code: 200, data: url === '/api/task/assigned' ? [] : url.endsWith('/form') ? { fields: [], values: {} } : { ...task, state: 'IN_PROGRESS' } }))
    mocks.post.mockResolvedValueOnce({ code: 403, msg: '无权更新来源记录' }).mockResolvedValueOnce({ code: 200, data: { ...task, state: 'COMPLETED' } })
    const wrapper = mount(BusinessTaskInbox, { props: { taskId: task.id }, global: { stubs } })
    await flushPromises()
    await wrapper.get('[data-complete]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('无权更新来源记录')
    await wrapper.get('[data-complete]').trigger('click')
    await flushPromises()
    expect(mocks.post.mock.calls[0][1]).toEqual(mocks.post.mock.calls[1][1])
    expect(mocks.post.mock.calls[0][1].resultJson).toBe('{}')
    wrapper.unmount()
  })

  it('does not offer another assignee lifecycle actions and rejects inaccessible result forms', async () => {
    mocks.user.userId = '21'
    mocks.get.mockImplementation(async url => url.endsWith('/form') ? { code: 403, msg: '来源不在业务数据范围' } : { code: 200, data: url === '/api/task/assigned' ? [] : task })
    const wrapper = mount(BusinessTaskInbox, { props: { taskId: task.id }, global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('来源不在业务数据范围')
    expect(wrapper.find('[data-start]').exists()).toBe(false)
    expect(wrapper.find('[data-complete]').exists()).toBe(false)
    expect(mocks.post).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('dispatches from a real source record without client assignee/tenant mapping and reads linked instances', async () => {
    const action = { configKey: 'records', bindingKey: 'review', taskIdField: 'taskId' }
    await openLinkedBusinessTask(action, { id: 'record-1', assignee: 'malicious-client-value' })
    expect(mocks.post).toHaveBeenCalledWith('/ai/crud/records/tasks/review/record-1')
    await openLinkedBusinessTask(action, { id: 'record-1', taskId: task.id })
    expect(mocks.get).toHaveBeenCalledWith('/ai/crud/records/tasks/review/record-1')
    mocks.post.mockResolvedValueOnce({ code: 409, msg: '记录状态不允许派发' })
    await expect(openLinkedBusinessTask(action, { id: 'record-2' })).rejects.toThrow('记录状态不允许派发')
  })

  it('confirms the explicit periodic source with an empty result without treating unknown sources as compatible', async () => {
    mocks.get.mockImplementation(async url => ({ code: 200, data: url === '/api/task/assigned' ? [] : { ...task, sourceType: 'task.periodic', state: 'IN_PROGRESS' } }))
    const wrapper = mount(BusinessTaskInbox, { props: { taskId: task.id }, global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('完成本周期')
    expect(mocks.get.mock.calls.some(([url]) => url.endsWith('/form'))).toBe(false)
    await wrapper.get('[data-complete]').trigger('click')
    await flushPromises()
    expect(mocks.post.mock.calls[0][1].resultJson).toBe('{}')
    wrapper.unmount()
    mocks.get.mockImplementation(async url => ({ code: 200, data: url === '/api/task/assigned' ? [] : { ...task, sourceType: 'custom.source', state: 'IN_PROGRESS' } }))
    const other = mount(BusinessTaskInbox, { props: { taskId: task.id }, global: { stubs } })
    await flushPromises()
    expect(other.get('[data-complete]').attributes('disabled')).toBeDefined()
    expect(other.text()).toContain('来源业务页面')
    other.unmount()
  })

  it('keeps a newly opened detail when an older start response arrives late', async () => {
    let finishStart
    mocks.post.mockImplementationOnce(() => new Promise((resolve) => {
      finishStart = resolve
    }))
    const wrapper = mount(BusinessTaskInbox, { props: { taskId: task.id }, global: { stubs } })
    await flushPromises()
    await wrapper.get('[data-start]').trigger('click')
    mocks.get.mockImplementation(async url => ({ code: 200, data: url === '/api/task/assigned' ? [] : url.endsWith('/form') ? { fields: [], values: {} } : { ...task, id: '201', title: '另一个任务' } }))
    await wrapper.setProps({ taskId: '201' })
    await flushPromises()
    finishStart({ code: 200, data: { ...task, state: 'IN_PROGRESS' } })
    await flushPromises()
    expect(wrapper.vm.$.setupState.selected.id).toBe('201')
    expect(wrapper.vm.$.setupState.selected.state).toBe('ASSIGNED')
    wrapper.unmount()
  })
})
