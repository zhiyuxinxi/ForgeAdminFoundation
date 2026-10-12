import { mount, shallowMount } from '@vue/test-utils'
import { NSelect, NSwitch } from 'naive-ui'
import { afterEach, expect, it } from 'vitest'
import { resolveActionColumnWidth, resolveSemanticCell } from '@/components/ai-form/table-presentation'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import { setLocale } from '@/locales'
import FieldValueRenderer from '../FieldValueRenderer.vue'
import SemanticColumnSettings from '../SemanticColumnSettings.vue'

afterEach(() => setLocale('zh-CN'))

it('keeps omitted identifier fields empty and treats blank action limits as the shared default', () => {
  expect(resolveSemanticCell({ name: 'Pump' }, 'name', { renderType: 'entity' })).toMatchObject({ title: 'Pump', subtitle: '' })
  expect(resolveSemanticCell({ name: 'Pump' }, '', { renderType: 'entity' })).toMatchObject({ title: '', subtitle: '' })
  expect(resolveSemanticCell({ name: 'Pump' }, 'name', { renderType: 'entity', subtitleField: ' ' }).subtitle).toBe('')
  for (const maxActionButtons of [undefined, null, ''])
    expect(resolveActionColumnWidth({ maxActionButtons }, 6)).toBe(174)
  expect(resolveActionColumnWidth({ width: 280, maxActionButtons: 1 }, 6)).toBe(280)
})

it('renders entity identity from saved fields without translating business values', async () => {
  const props = { value: '原始名称', row: { name: '设备检查', code: 'CHECK-01' }, field: { field: 'name' }, setting: { renderType: 'entity', subtitleField: 'code', avatar: true } }
  const wrapper = mount(FieldValueRenderer, { props })
  expect(wrapper.find('.system-table-cell__primary').text()).toBe('设备检查')
  expect(wrapper.find('.system-table-cell__secondary').text()).toBe('CHECK-01')
  expect(wrapper.find('button').exists()).toBe(false)
  await wrapper.setProps({ onNavigate: () => {}, setting: { ...props.setting, clickAction: 'navigate' } })
  await wrapper.find('button').trigger('click')
  expect(wrapper.emitted('navigate')).toHaveLength(1)
  wrapper.unmount()
})

it('reuses related item aggregation and localizes only the action label', async () => {
  setLocale('en-US')
  const wrapper = mount(FieldValueRenderer, { props: { row: { names: ['生产部', '设备部', '生产部'] }, field: { field: 'names' }, setting: { renderType: 'relations' } } })
  expect(wrapper.findComponent(SystemTableCell).props('values')).toEqual(['生产部', '设备部', '生产部'])
  expect(wrapper.find('.system-table-cell__primary').text()).toBe('生产部')
  expect(wrapper.find('.system-table-cell__count').text()).toBe('+1')
  expect(wrapper.find('.system-table-cell__count').attributes('aria-label')).toBe('View all 2 items')
  wrapper.unmount()
})

it('emits editable metadata patches from the shared designer controls', () => {
  const wrapper = shallowMount(SemanticColumnSettings, { props: { renderType: 'entity', setting: {}, fields: [{ label: '编码', value: 'code' }] } })
  wrapper.findAllComponents(NSelect)[1].vm.$emit('update:value', 'code')
  wrapper.findComponent(NSwitch).vm.$emit('update:value', true)
  expect(wrapper.emitted('update')).toEqual([[{ subtitleField: 'code' }], [{ avatar: true }]])
  wrapper.unmount()
})
