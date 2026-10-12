import { flushPromises, shallowMount } from '@vue/test-utils'
import { expect, it, vi } from 'vitest'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import PremiumTree from '@/components/common/PremiumTree.vue'
import TreeCrudTemplate from '../TreeCrudTemplate.vue'

const request = vi.hoisted(() => vi.fn())
vi.mock('@/utils', () => ({ request }))
vi.mock('@/components/ai-form/AiCrudPage.vue', () => ({ default: { name: 'AiCrudPage', props: ['publicParams'], render: () => null } }))

it('keeps tree selection, lazy child loading and clear filtering within the shared workspace', async () => {
  request.mockResolvedValueOnce({ code: 200, data: [{ id: 10, name: 'Parent', isLeaf: false }] })
    .mockResolvedValueOnce({ code: 200, data: [{ id: 11, name: 'Child', isLeaf: true }] })
  const wrapper = shallowMount(TreeCrudTemplate, { props: { crudProps: { apiConfig: { tree: 'get@/sample/tree' }, options: { treeConfig: { loadMode: 'lazy', filterField: 'orgId' } } } }, global: { stubs: { MasterDetailWorkspace: { template: '<section><slot name="aside" /><slot /></section>' }, NSpin: { template: '<div><slot /></div>' }, NButton: true } } })
  await flushPromises()
  expect(wrapper.findComponent(MasterDetailWorkspace).exists()).toBe(true)
  const tree = wrapper.findComponent(PremiumTree)
  const parent = tree.props('data')[0]
  tree.vm.$emit('update:selected-keys', [10], parent)
  await flushPromises()
  expect(wrapper.findComponent(AiCrudPage).props('publicParams')).toMatchObject({ orgId: 10, orgId_includeChildren: true })
  tree.vm.$emit('update:expanded-keys', [10], parent)
  await flushPromises()
  expect(request.mock.calls[1][0].params).toMatchObject({ parentValue: 10, loadMode: 'lazy' })
  expect(tree.props('data')[0].children[0].key).toBe(11)
  await wrapper.find('n-button-stub').trigger('click')
  await flushPromises()
  expect(wrapper.findComponent(AiCrudPage).props('publicParams')).not.toHaveProperty('orgId')
  wrapper.unmount()
})

it('preserves recursive expansion for existing full-load tree configurations', async () => {
  request.mockResolvedValueOnce({ code: 200, data: [{ id: 10, name: 'A', children: [{ id: 11, name: 'B', children: [{ id: 12, name: 'C' }] }] }] })
  const wrapper = shallowMount(TreeCrudTemplate, { props: { crudProps: { apiConfig: { tree: 'get@/sample/tree' }, options: { treeConfig: {} } } }, global: { stubs: { MasterDetailWorkspace: { template: '<section><slot name="aside" /><slot /></section>' }, NSpin: { template: '<div><slot /></div>' }, NButton: true } } })
  await flushPromises()
  expect(wrapper.findComponent(PremiumTree).props('expandedKeys')).toEqual([10, 11])
  wrapper.unmount()
})
