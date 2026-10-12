import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AddNodeButton from '../AddNodeButton.vue'
import AddNodePopover from '../AddNodePopover.vue'
import BranchAddButton from '../BranchAddButton.vue'

describe('addNodePopover', () => {
  it('渲染所有 3 个分组', () => {
    const wrapper = mount(AddNodePopover)
    expect(wrapper.text()).toContain('审批流')
    expect(wrapper.text()).toContain('分支')
    expect(wrapper.text()).toContain('高级')
  })

  it('渲染所有 9 个节点类型按钮', () => {
    const wrapper = mount(AddNodePopover)
    expect(wrapper.findAll('button').length).toBe(9)
  })

  it('approver 按钮存在并带 data-type', () => {
    const wrapper = mount(AddNodePopover)
    const approverBtn = wrapper.find('button[data-type="approver"]')
    expect(approverBtn.exists()).toBe(true)
    expect(approverBtn.text()).toContain('审批人')
  })

  it('allowTypes 限制可见类型', () => {
    const wrapper = mount(AddNodePopover, {
      props: { allowTypes: ['approver', 'carbonCopy'] },
    })
    expect(wrapper.findAll('button').length).toBe(2)
    expect(wrapper.text()).toContain('审批流')
    expect(wrapper.text()).not.toContain('条件分支')
  })
})

describe('addNodeButton', () => {
  it('按 position 绝对定位', () => {
    const wrapper = mount(AddNodeButton, {
      props: { position: { x: 100, y: 50 } },
    })
    const div = wrapper.find('.add-node-button-wrap')
    // 新样式用 translate(-50%, 0) 居中，left 直接等于 position.x
    expect(div.attributes('style')).toContain('left: 100px')
    expect(div.attributes('style')).toContain('top: 50px')
  })

  it('点击按钮后 popover 显示', async () => {
    const wrapper = mount(AddNodeButton, { props: { position: { x: 0, y: 0 } } })
    expect(wrapper.findComponent(AddNodePopover).exists()).toBe(false)
    await wrapper.find('button').trigger('click')
    expect(wrapper.findComponent(AddNodePopover).exists()).toBe(true)
  })

  it('readonly 模式禁用按钮', () => {
    const wrapper = mount(AddNodeButton, {
      props: { position: { x: 0, y: 0 }, readonly: true },
    })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })
})

describe('branchAddButton', () => {
  it('按 position 绝对定位并触发 click', async () => {
    const wrapper = mount(BranchAddButton, {
      props: { position: { x: 120, y: 88 } },
    })
    expect(wrapper.find('.branch-add-button-wrap').attributes('style')).toContain('left: 120px')
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('readonly 模式禁用按钮', () => {
    const wrapper = mount(BranchAddButton, {
      props: { position: { x: 0, y: 0 }, readonly: true },
    })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })
})
