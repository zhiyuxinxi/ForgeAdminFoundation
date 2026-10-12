import { afterEach, expect, it, vi } from 'vitest'
import { localizeLabel, setLocale } from '@/locales'
import { confirmDirtyTabs, copyText } from '../tab-interactions'

afterEach(() => {
  setLocale('zh-CN')
  delete window.$dialog
  delete window.$message
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

it('keeps customer titles verbatim and honors confirm/cancel/close in both languages', async () => {
  let options
  window.$dialog = {
    warning: (value) => {
      options = value
    },
  }
  setLocale('en-US')
  for (const [callback, expected] of [['onPositiveClick', true], ['onNegativeClick', false], ['onClose', false]]) {
    const result = confirmDirtyTabs([{ title: '客户{甲}', dirty: true }], '关闭')
    expect(options.title).toBe('Unsaved changes')
    expect(options.content).toContain('客户{甲}')
    expect(options.content).toContain('Close may discard these changes.')
    expect(options.positiveText).toBe('Close')
    expect(options.negativeText).toBe('Cancel')
    options[callback]()
    expect(await result).toBe(expected)
  }
  setLocale('zh-CN')
  const result = confirmDirtyTabs([{ title: '客户{甲}', dirty: true }], '关闭')
  expect(options.content).toBe('“客户{甲}”存在未保存的更改，关闭可能会丢失这些更改。')
  options.onNegativeClick()
  expect(await result).toBe(false)
})

it('localizes the native fallback and multiple-tab count without prompting for clean tabs', async () => {
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false)
  setLocale('en-US')
  expect(await confirmDirtyTabs([{ dirty: false }])).toBe(true)
  expect(confirm).not.toHaveBeenCalled()
  expect(await confirmDirtyTabs(Array.from({ length: 4 }, (_, n) => ({ title: `Page ${n}`, dirty: true })))).toBe(false)
  expect(confirm.mock.calls[0][0]).toContain('4 total')
  expect(confirm.mock.calls[0][0]).toContain('\nContinue?')
})

it('localizes clipboard feedback and unknown file errors while preserving copied content', async () => {
  const writeText = vi.fn().mockResolvedValue()
  vi.stubGlobal('isSecureContext', true)
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } })
  window.$message = { success: vi.fn(), error: vi.fn() }
  setLocale('en-US')
  await copyText('客户甲\nhttps://example.test/page', '页面名称和地址已复制')
  expect(writeText).toHaveBeenCalledWith('客户甲\nhttps://example.test/page')
  expect(window.$message.success).toHaveBeenCalledWith('Page title and address copied')
  writeText.mockRejectedValue(new Error('denied'))
  await copyText('same data')
  expect(window.$message.error).toHaveBeenCalledWith('Copy failed. Please copy manually.')
  expect(localizeLabel('未知错误')).toBe('Unknown error')
})
