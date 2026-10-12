import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getDictData } from '@/composables/useDict'
import DictSelect from '../DictSelect.vue'

vi.mock('@/composables/useDict', () => ({ getDictData: vi.fn() }))

const NSelectStub = {
  name: 'NSelect',
  props: ['options', 'loading'],
  template: '<div />',
}

enableAutoUnmount(afterEach)

async function optionsFor(rows, props = {}) {
  getDictData.mockResolvedValue(rows)
  const wrapper = mount(DictSelect, {
    props: { dictType: 'test_status', ...props },
    global: { stubs: { NSelect: NSelectStub } },
  })
  await flushPromises()
  const select = wrapper.getComponent(NSelectStub)
  expect(select.props('loading')).toBe(false)
  return select.props('options')
}

beforeEach(() => vi.clearAllMocks())

describe('DictSelect cascade options', () => {
  it('keeps every loaded option without cascade constraints and preserves disabled status', async () => {
    const options = await optionsFor([
      { label: '启用', value: 'enabled', status: 1, raw: { dictStatus: 1 } },
      { label: '停用', value: 'disabled', status: 0, raw: { dictStatus: 0 } },
    ])

    expect(options).toMatchObject([
      { label: '启用', value: 'enabled', disabled: false },
      { label: '停用', value: 'disabled', disabled: true },
    ])
  })

  it('still filters by an explicit parent code, including zero', async () => {
    const options = await optionsFor([
      { label: '匹配', value: 'match', status: 1, raw: { parentDictCode: '0' } },
      { label: '其他', value: 'other', status: 1, raw: { parentDictCode: 2 } },
    ], { parentDictCode: 0 })

    expect(options.map(option => option.value)).toEqual(['match'])
  })

  it('still requires the configured linked dictionary type and value', async () => {
    const options = await optionsFor([
      { label: '匹配', value: 'match', raw: { linkedDictType: 'kind', linkedDictValue: 'A' } },
      { label: '其他类型', value: 'wrong-type', raw: { linkedDictType: 'other', linkedDictValue: 'A' } },
      { label: '其他值', value: 'wrong-value', raw: { linkedDictType: 'kind', linkedDictValue: 'B' } },
    ], { linkedDictType: 'kind', linkedDictValue: 'A' })

    expect(options.map(option => option.value)).toEqual(['match'])
  })

  it('keeps a dependent selector empty until its parent field has a value', async () => {
    const options = await optionsFor([
      { label: '子项', value: 'child', raw: { parentDictCode: 1 } },
    ], { cascade: { enabled: true, sourceField: 'parent', mode: 'parentDictCode' }, formData: {} })

    expect(options).toEqual([])
  })
})
