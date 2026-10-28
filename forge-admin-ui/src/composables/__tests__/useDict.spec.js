import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed } from 'vue'
import { setLocale } from '@/locales'
import { clearDictCache, getDictData } from '../useDict'

const requestMocks = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('@/utils/http', () => ({ request: requestMocks }))

afterEach(() => setLocale('zh-CN'))

describe('useDict managed cache endpoint', () => {
  beforeEach(() => {
    clearDictCache()
    requestMocks.get.mockReset()
  })

  it('changes cached display labels reactively without fetching again or changing dictionary values', async () => {
    requestMocks.get.mockResolvedValue({ code: 200, data: [
      { dictCode: 1, dictLabel: '采购处理中', dictLabelEn: 'Purchase in progress', dictValue: 'IN_PROCESS' },
    ] })
    const options = await getDictData('bilingual-status')
    const labels = computed(() => options.map(item => item.label))
    expect(labels.value).toEqual(['采购处理中'])
    setLocale('en-US')
    expect(labels.value).toEqual(['Purchase in progress'])
    expect((await getDictData('bilingual-status'))[0].value).toBe('IN_PROCESS')
    expect(options[0].raw.dictLabel).toBe('采购处理中')
    expect(requestMocks.get).toHaveBeenCalledTimes(1)
  })

  it('loads business dictionaries through the managed type endpoint', async () => {
    requestMocks.get.mockResolvedValue({
      code: 200,
      data: [
        { dictCode: 2, dictLabel: '停用', dictValue: '0', dictSort: 2 },
        { dictCode: 1, dictLabel: '启用', dictValue: '1', dictSort: 1 },
      ],
    })

    const result = await getDictData('sys/org type', true)

    expect(requestMocks.get).toHaveBeenCalledWith(
      '/system/dict/data/type/sys%2Forg%20type',
      { needTip: false },
    )
    expect(result.map(item => item.value)).toEqual(['1', '0'])
  })
})
