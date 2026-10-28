import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AiFormItem from '@/components/ai-form/AiFormItem.vue'
import UserSelectModal from '../UserSelectModal.vue'

const { get } = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/utils/http', () => ({ request: { get } }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: {}, params: {}, path: '/', fullPath: '/', name: 'identity-test' }) }))

const TreeSelect = { name: 'NTreeSelect', props: ['options', 'onLoad', 'value'], emits: ['update:value'], template: '<div class="tree" />' }
const DataTable = { name: 'NDataTable', props: ['data', 'checkedRowKeys'], emits: ['update:checked-row-keys'], template: '<div class="table" />' }
const Button = { name: 'NButton', props: ['disabled'], template: '<button :disabled="disabled"><slot /></button>' }
const Wrap = { template: '<div><slot /><slot name="footer" /></div>' }
const stubs = {
  ...Object.fromEntries(['NInputNumber', 'NSelect', 'NRadio', 'NRadioGroup', 'NRadioButton', 'NCheckbox', 'NCheckboxGroup', 'NSwitch', 'NDatePicker', 'NTimePicker', 'NUpload', 'NSlider', 'NRate', 'NColorPicker', 'NCascader', 'NTransfer', 'NInputGroup'].map(name => [name, true])),
  NModal: Wrap,
  NSpace: Wrap,
  NFormItem: Wrap,
  NInput: true,
  NTreeSelect: TreeSelect,
  NDataTable: DataTable,
  NButton: Button,
  NAlert: Wrap,
  NPagination: true,
  NTooltip: Wrap,
  NIcon: true,
  AiRecordSelectorModal: true,
}
const reference = { base: '/ai/crud/config/references/ownerId', operation: 'add' }

describe('identity reference component consumers', () => {
  beforeEach(() => {
    get.mockReset()
  })

  it.each([
    { context: { identityReferenceConfigKey: 'config', modalStatus: 'add' }, base: '/ai/crud/config/references' },
    { context: { identityReferenceApiBase: '/quality/record/references', modalStatus: 'add' }, base: '/quality/record/references' },
  ])('wires the organization form field to scoped roots and lazy children at $base', async ({ context, base }) => {
    get.mockResolvedValueOnce({ code: 200, data: [{ orgId: '10', parentId: '0', orgName: '部门' }] })
      .mockResolvedValueOnce({ code: 200, data: [{ orgId: '11', parentId: '10', orgName: '下级' }] })
    const wrapper = mount(AiFormItem, { props: { field: { field: 'orgId', type: 'orgTreeSelect' }, context }, global: { stubs } })
    await flushPromises()
    const tree = wrapper.getComponent(TreeSelect)
    expect(get.mock.calls[0][0]).toBe(`${base}/orgId/organizations`)
    expect(tree.props('options')[0]).toMatchObject({ value: '10', label: '部门' })
    const root = tree.props('options')[0]
    await tree.props('onLoad')(root)
    expect(get.mock.calls[1][1].params.parentId).toBe('10')
    expect(root.children[0].value).toBe('11')
    wrapper.unmount()
  })

  it('keeps explicitly configured external organization options without a directory request', async () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: { field: 'contractorId', type: 'orgTreeSelect', options: [{ value: 'outside', label: '外部单位' }] },
        context: { identityReferenceConfigKey: 'config', modalStatus: 'add' },
      },
      global: { stubs },
    })
    await flushPromises()
    expect(wrapper.getComponent(TreeSelect).props('options')).toEqual([{ value: 'outside', label: '外部单位' }])
    expect(wrapper.getComponent(TreeSelect).props('onLoad')).toBeUndefined()
    expect(get).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('uses real cursor pages and preserves a selection from a previous page', async () => {
    get.mockImplementation(async (url, config) => {
      if (url.endsWith('/organizations'))
        return { code: 200, data: [{ orgId: '10', parentId: '0', orgName: '部门' }] }
      const start = config.params.afterUserId ? 11 : 1
      const count = start === 1 ? 11 : 1
      return { code: 200, data: Array.from({ length: count }, (_, index) => ({ userId: String(start + index), username: `u${start + index}`, displayName: '同名' })) }
    })
    const wrapper = mount(UserSelectModal, { props: { reference, show: false, multiple: true }, global: { stubs } })
    await wrapper.setProps({ show: true })
    await flushPromises()
    expect(wrapper.findComponent({ name: 'NPagination' }).exists()).toBe(false)
    expect(wrapper.getComponent(TreeSelect).props('onLoad')).toBeTypeOf('function')
    const table = wrapper.getComponent(DataTable)
    expect(table.props('data')).toHaveLength(10)
    table.vm.$emit('update:checked-row-keys', ['1'])
    await wrapper.vm.$nextTick()
    await wrapper.findAll('button').find(button => button.text() === '下一页').trigger('click')
    await flushPromises()
    expect(get.mock.calls.at(-1)[1].params.afterUserId).toBe('10')
    expect(table.props('data').map(row => row.id)).toEqual(['11'])
    expect(wrapper.findAll('button').find(button => button.text() === '下一页').element.disabled).toBe(true)
    table.vm.$emit('update:checked-row-keys', ['1', '11'])
    await wrapper.vm.$nextTick()
    await wrapper.findAll('button').find(button => button.text() === '确定').trigger('click')
    expect(wrapper.emitted('confirm')[0][0].map(row => row.id)).toEqual(['1', '11'])
    expect(get.mock.calls.every(([url]) => url.startsWith(reference.base))).toBe(true)
    wrapper.unmount()
  })

  it('shows a business failure and does not fall back to system administration APIs', async () => {
    get.mockResolvedValue({ code: 403, msg: '无权选择此字段' })
    const errorLog = vi.spyOn(console, 'error').mockImplementation(() => {})
    const wrapper = mount(UserSelectModal, { props: { reference, show: false }, global: { stubs } })
    await wrapper.setProps({ show: true })
    await flushPromises()
    expect(wrapper.text()).toContain('无权选择此字段')
    expect(get).toHaveBeenCalledTimes(1)
    expect(wrapper.getComponent(DataTable).props('data')).toEqual([])
    wrapper.unmount()
    errorLog.mockRestore()
  })
})
