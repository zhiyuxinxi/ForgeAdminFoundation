import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { h } from 'vue'
import AiTable from '../AiTable.vue'

const NDataTableStub = {
  name: 'NDataTable',
  props: ['columns', 'data'],
  setup(props) {
    return () => h('div', props.columns.map(column => column.render(props.data[0], 0)))
  },
}

function renderCell(column, row) {
  return mount(AiTable, {
    props: {
      columns: [{ title: 'Value', ...column }],
      dataSource: [row],
      showToolbar: false,
      pagination: false,
      hideSelection: true,
    },
    global: {
      directives: { tableScrollEnhance: {} },
      stubs: { NDataTable: NDataTableStub },
    },
  })
}

describe('aiTable default cell renderer', () => {
  it.each([
    ['key', { key: 'value' }, { value: 'Key value' }, 'Key value'],
    ['prop', { prop: 'value' }, { value: 'Prop value' }, 'Prop value'],
    ['dataIndex', { dataIndex: 'value' }, { value: 'Data index value' }, 'Data index value'],
    ['nested key', { key: 'owner.name' }, { owner: { name: 'Nested value' } }, 'Nested value'],
    ['prop precedence', { prop: 'first', key: 'second', dataIndex: 'third' }, { first: 'First', second: 'Second', third: 'Third' }, 'First'],
    ['key precedence', { key: 'second', dataIndex: 'third' }, { second: 'Second', third: 'Third' }, 'Second'],
  ])('renders the resolved %s through the actual default renderer', (_label, column, row, expected) => {
    const wrapper = renderCell(column, row)
    expect(wrapper.find('.ai-table-cell-content').text()).toBe(expected)
    wrapper.unmount()
  })

  it.each([
    ['missing', {}, '—'],
    ['null', { value: null }, '—'],
    ['empty string', { value: '' }, '—'],
    ['zero', { value: 0 }, '0'],
    ['false', { value: false }, 'false'],
    ['missing nested value', { owner: null }, '—'],
  ])('renders missing values consistently while preserving %s', (label, row, expected) => {
    const wrapper = renderCell({ key: label === 'missing nested value' ? 'owner.name' : 'value' }, row)
    expect(wrapper.find('.ai-table-cell-content').text()).toBe(expected)
    wrapper.unmount()
  })
})
