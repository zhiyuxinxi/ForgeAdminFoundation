import assert from 'node:assert/strict'
import test from 'node:test'
import { adaptBusinessTaskFields } from '../business-task-form-adapter.js'

test('business task form adapter preserves range controls and writable metadata for H5', () => {
  const fields = adaptBusinessTaskFields([{
    field: 'period',
    label: '有效期',
    componentType: 'daterange',
    writable: true,
    readable: true,
    props: { startPlaceholder: '开始日期', endPlaceholder: '结束日期' },
  }])

  assert.equal(fields[0].field, 'period')
  assert.equal(fields[0].type, 'daterange')
  assert.equal(fields[0].readonly, false)
  assert.deepEqual(fields[0].props, {
    disabled: false,
    readonly: false,
    dictType: undefined,
    startPlaceholder: '开始日期',
    endPlaceholder: '结束日期',
  })
})
