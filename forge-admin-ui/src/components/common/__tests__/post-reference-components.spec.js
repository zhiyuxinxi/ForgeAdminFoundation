import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AiFormItem from '@/components/ai-form/AiFormItem.vue'
import { resolveFieldComponentDefaults } from '@/views/app-center/components/designer/form-first/fieldComponentCatalog'
import PostSelectPicker from '../PostSelectPicker.vue'

const { get } = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/utils/http', () => ({ request: { get } }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: {}, params: {}, path: '/', fullPath: '/', name: 'post-test' }) }))
const Tree = { name: 'NTreeSelect', props: ['options', 'onLoad'], emits: ['update:value'], template: '<div class="tree" />' }
const Select = { name: 'NSelect', props: ['options', 'value', 'disabled'], emits: ['update:value', 'search'], template: '<div class="select" />' }
const Wrap = { template: '<div><slot /></div>' }
const Button = { props: ['disabled'], template: '<button :disabled="disabled"><slot /></button>' }
const stubs = { NSpace: Wrap, NAlert: Wrap, NFormItem: Wrap, NButton: Button, NTreeSelect: Tree, NSelect: Select, AiRecordSelectorModal: true }
const reference = { base: '/ai/crud/config/references/postId', operation: 'add' }
let tenantWideAllowed

function mockLookups(posts = []) {
  get.mockImplementation(async (url) => {
    if (url.endsWith('/post-scope'))
      return { code: 200, data: { tenantWideAllowed } }
    if (url.endsWith('/organizations'))
      return { code: 200, data: [{ orgId: '10', parentId: '0', orgName: '部门' }] }
    return { code: 200, data: posts }
  })
}

describe('post reference consumers', () => {
  beforeEach(() => {
    get.mockReset()
    tenantWideAllowed = false
  })

  it('generates bigint POST metadata and emits stable IDs with a separate label snapshot', async () => {
    expect(resolveFieldComponentDefaults('postSelect')).toMatchObject({ fieldType: 'POST', dataType: 'bigint', componentType: 'postSelect' })
    mockLookups([{ postId: '9007199254740995', orgId: '10', postCode: 'A', postName: '同名岗位' }])
    const patchFormData = vi.fn()
    const wrapper = mount(AiFormItem, {
      props: { field: { field: 'postId', type: 'postSelect', props: { labelValueField: 'postName' } }, context: { identityReferenceConfigKey: 'config', modalStatus: 'add', patchFormData } },
      global: { stubs },
    })
    await flushPromises()
    wrapper.getComponent(Tree).vm.$emit('update:value', '10')
    await flushPromises()
    const select = wrapper.getComponent(Select)
    expect(select.props('options')[0]).toMatchObject({ value: '9007199254740995', label: '同名岗位' })
    select.vm.$emit('update:value', '9007199254740995')
    await flushPromises()
    expect(wrapper.emitted('update:value').at(-1)).toEqual(['9007199254740995'])
    expect(patchFormData).toHaveBeenCalledWith(expect.objectContaining({ postName: '同名岗位' }))
    expect(get.mock.calls.at(-1)[1].params).toMatchObject({ directOrgId: '10', tenantWide: false, limit: 21 })
    wrapper.unmount()
  })

  it('uses a separate server-authorized tenant-wide mode without passing a fake organization', async () => {
    tenantWideAllowed = true
    mockLookups([{ postId: '103', orgId: null, postCode: 'GLOBAL', postName: '全局岗位' }])
    const wrapper = mount(PostSelectPicker, { props: { reference }, global: { stubs } })
    await flushPromises()
    expect(wrapper.getComponent(Tree).props('options').map(option => option.value)).toEqual(['tenant-wide', '10'])
    wrapper.getComponent(Tree).vm.$emit('update:value', 'tenant-wide')
    await flushPromises()
    expect(get.mock.calls.at(-1)[1].params).toMatchObject({ tenantWide: true, directOrgId: undefined })
    wrapper.unmount()
  })

  it('preserves an old disabled snapshot and ignores stale search results after organization changes', async () => {
    mockLookups()
    const wrapper = mount(PostSelectPicker, { props: { reference, modelValue: '99', labelValue: '历史岗位' }, global: { stubs } })
    await flushPromises()
    expect(wrapper.getComponent(Tree).props('options').map(option => option.value)).toEqual(['10'])
    let finishOld
    get.mockImplementationOnce(() => new Promise((resolve) => {
      finishOld = resolve
    }))
    wrapper.getComponent(Tree).vm.$emit('update:value', '10')
    await flushPromises()
    get.mockResolvedValueOnce({ code: 200, data: [{ postId: '201', orgId: '20', postCode: 'B', postName: '新结果' }] })
    wrapper.getComponent(Tree).vm.$emit('update:value', '20')
    await flushPromises()
    finishOld({ code: 200, data: [{ postId: '101', orgId: '10', postCode: 'A', postName: '过期结果' }] })
    await flushPromises()
    expect(wrapper.getComponent(Select).props('options')).toEqual([
      { value: '99', label: '历史岗位', disabled: true },
      expect.objectContaining({ value: '201', label: '新结果' }),
    ])
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    wrapper.unmount()
  })

  it('shows a 200 transport business rejection without calling administrative APIs', async () => {
    mockLookups()
    const wrapper = mount(PostSelectPicker, { props: { reference }, global: { stubs } })
    await flushPromises()
    get.mockResolvedValueOnce({ code: 403, msg: '岗位不在业务范围' })
    wrapper.getComponent(Tree).vm.$emit('update:value', '10')
    await flushPromises()
    expect(wrapper.text()).toContain('岗位不在业务范围')
    expect(wrapper.getComponent(Select).props('options')).toEqual([])
    expect(get.mock.calls.every(([url]) => url.startsWith(reference.base))).toBe(true)
    wrapper.unmount()
  })
})
