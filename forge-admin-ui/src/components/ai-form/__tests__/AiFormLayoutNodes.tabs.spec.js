import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AiFormLayoutNodes from '../AiFormLayoutNodes.vue'

vi.mock('vue-router', () => ({ useRoute: () => ({ query: {}, params: {}, path: '/example' }) }))
vi.mock('../AiFormItem.vue', () => ({ default: { render: () => null } }))
vi.mock('@/components/lowcode-builder/shared/PageWidgetRenderer.vue', () => ({ default: { render: () => null } }))

it('uses the selected schema tab and forwards user changes with the current form record', async () => {
  const onUpdateValue = vi.fn()
  const node = { nodeType: 'tabs', type: 'tabs', props: { value: 'review', onUpdateValue }, children: [] }
  const formValue = { id: '9007199254740993' }
  const wrapper = shallowMount(AiFormLayoutNodes, {
    props: { nodes: [node], formValue },
    global: { renderStubDefaultSlot: true, stubs: {
      NGrid: { template: '<div><slot /></div>' },
      NGi: { template: '<div><slot /></div>' },
      NTabs: { name: 'NTabs', props: ['value'], emits: ['update:value'], template: '<div class="tabs" :data-value="value"><slot /></div>' },
      NTabPane: true, NCollapse: true, NCollapseItem: true, NCard: true, NButton: true, NTable: true,
    } },
  })
  expect(wrapper.find('.tabs').attributes('data-value')).toBe('review')
  wrapper.findComponent({ name: 'NTabs' }).vm.$emit('update:value', 'details')
  expect(onUpdateValue).toHaveBeenCalledWith('details', formValue)
  expect(wrapper.emitted('nodeAction')[0][0]).toMatchObject({ type: 'tabs-change', value: 'details', formData: formValue })
  wrapper.unmount()
})
