import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import flowApi from '@/api/flow'
import NodeDetailPopover from '@/components/flow-designer/viewer/NodeDetailPopover.vue'
import { setLocale } from '@/locales'
import InteractiveProcessDiagram from '../InteractiveProcessDiagram.vue'
import ProcessNodeTaskList from '../ProcessNodeTaskList.vue'

vi.mock('@/api/flow', () => ({ default: { getProcessDiagramInfo: vi.fn() } }))

afterEach(() => setLocale('zh-CN'))

const tasks = [
  { taskId: 'old-alice', executionId: 'execution-old', assigneeName: 'Alice', status: 'completed', startTime: '2026-10-08', endTime: '2026-10-09' },
  { taskId: 'new-alice', executionId: 'execution-new', assigneeName: 'Alice', status: 'running' },
  { taskId: 'new-bob', assigneeName: 'Bob', status: 'running' },
]

describe('node task occurrences', () => {
  it('renders all instances and candidate names in the interactive diagram consumer', async () => {
    flowApi.getProcessDiagramInfo.mockResolvedValue({
      code: 200,
      data: {
        status: 'running',
        diagramBase64: 'data:image/png;base64,AA==',
        nodes: [{ nodeId: 'review', nodeType: 'UserTask', status: 'running', x: 10, y: 10, assigneeNames: [], candidateUserIds: ['candidate-id'], candidateUserNames: ['Candidate'], taskInstances: tasks }],
      },
    })
    const wrapper = mount(InteractiveProcessDiagram, {
      props: { processInstanceId: 'process-1' },
      global: { stubs: { Teleport: true, NSpin: { template: '<div><slot /></div>' }, NTag: { template: '<span><slot /></span>' }, NDivider: true, NEmpty: true } },
    })
    await flushPromises()
    await wrapper.find('img').trigger('load')
    await wrapper.find('.node-indicator').trigger('mouseenter')
    expect(wrapper.findAll('[data-task-id]')).toHaveLength(3)
    expect(wrapper.find('.node-tooltip').text()).toContain('Candidate')
    wrapper.unmount()
  })

  it('keeps parallel and historical tasks separate, including the same assignee twice', async () => {
    const wrapper = mount(ProcessNodeTaskList, { props: { tasks } })
    expect(wrapper.findAll('li')).toHaveLength(3)
    expect(wrapper.find('[data-task-id="old-alice"]').text()).toContain('已完成')
    expect(wrapper.find('[data-task-id="new-alice"]').text()).toContain('处理中')
    setLocale('en-US')
    await nextTick()
    expect(wrapper.find('[data-task-id="old-alice"]').text()).toContain('Completed')
    expect(wrapper.find('[data-task-id="new-alice"]').text()).toContain('In progress')
    expect(wrapper.text()).toContain('Alice')
    wrapper.unmount()
  })

  it('renders the taskInstances received by the real node detail consumer', () => {
    const wrapper = mount(NodeDetailPopover, {
      props: { visible: true, node: { id: 'review' }, taskInfo: { status: 'running', taskInstances: tasks } },
    })
    expect(wrapper.findAll('[data-task-id]')).toHaveLength(3)
    wrapper.unmount()
  })

  it('preserves legacy detail without an instances field', () => {
    const wrapper = mount(NodeDetailPopover, {
      props: { visible: true, node: { id: 'review' }, taskInfo: { assigneeName: 'Alice', status: 'running' } },
    })
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.findAll('[data-task-id]')).toHaveLength(0)
    wrapper.unmount()
  })
})
