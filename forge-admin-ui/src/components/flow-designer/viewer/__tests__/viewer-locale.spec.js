import { mount } from '@vue/test-utils'
import { afterEach, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { setLocale } from '@/locales'
import NodeCard from '../../nodes/NodeCard.vue'
import NodeDetailPopover from '../NodeDetailPopover.vue'

afterEach(() => setLocale('zh-CN'))

it('reactively translates readonly badges and node details without translating customer names or comments', async () => {
  const node = { id: 'approve1', name: '客户审批节点', nodeType: 'approver' }
  const card = mount(NodeCard, { props: { node, readonly: true, status: 'completed' } })
  const detail = mount(NodeDetailPopover, { props: { visible: true, node, taskInfo: { status: 'completed', assigneeName: '张三', result: 'approved', comment: '同意保留原文', startTime: '2026-09-27T08:00:00' } } })
  expect(card.text()).toContain('已完成')
  setLocale('en-US')
  await nextTick()
  expect(card.text()).toContain('Completed')
  expect(card.text()).toContain('客户审批节点')
  expect(detail.text()).toContain('Assignee')
  expect(detail.text()).toContain('张三')
  expect(detail.text()).toContain('同意保留原文')
  expect(detail.find('.node-status-badge').text()).toBe('Completed')
  expect(detail.find('[aria-label]').attributes('aria-label')).toBe('Close details')
  setLocale('zh-CN')
  await nextTick()
  expect(card.text()).toContain('已完成')
  card.unmount()
  detail.unmount()
})
