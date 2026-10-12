import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NodeDetailPopover from '../NodeDetailPopover.vue'

function mountPopover(props = {}) {
  return mount(NodeDetailPopover, {
    props: {
      visible: true,
      position: { x: 120, y: 80 },
      node: { id: 'T1', name: '部门经理审批' },
      taskInfo: {
        status: 'running',
        assigneeName: '超级管理员',
        startTime: '2026-06-20T09:12:00',
        endTime: null,
        result: 'approved',
        comment: '同意提交。',
      },
      ...props,
    },
  })
}

describe('nodeDetailPopover', () => {
  it('使用紧凑详情布局展示节点运行信息', () => {
    const wrapper = mountPopover()

    expect(wrapper.find('.node-detail-popover').attributes('style')).toContain('width: 360px')
    expect(wrapper.find('.detail-list').exists()).toBe(true)
    expect(wrapper.find('.detail-grid').exists()).toBe(false)
    expect(wrapper.text()).toContain('部门经理审批')
    expect(wrapper.text()).toContain('处理人')
    expect(wrapper.text()).toContain('超级管理员')
    expect(wrapper.text()).toContain('审批意见')
  })

  it('点击关闭按钮触发 close 事件', async () => {
    const wrapper = mountPopover()

    await wrapper.find('button[aria-label="关闭详情"]').trigger('click')

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('无运行数据时展示空状态', () => {
    const wrapper = mountPopover({ taskInfo: null })

    expect(wrapper.text()).toContain('暂无处理记录')
  })
})
