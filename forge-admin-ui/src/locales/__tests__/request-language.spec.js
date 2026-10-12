import { afterEach, expect, it, vi } from 'vitest'
import { managedFetch } from '@/composables/useGlobalLoading'
import { consumeSSE } from '@/utils/sse'
import { setLocale, withLocaleHeaders } from '../index'

afterEach(() => { vi.unstubAllGlobals(); setLocale('zh-CN') })

it('captures export language once without mutating authorization or caller headers', async () => {
  let finish
  const fetch = vi.fn(() => new Promise(resolve => { finish = resolve }))
  vi.stubGlobal('fetch', fetch)
  const headers = new Headers({ Authorization: 'test-only', 'X-Export': 'reports' })
  setLocale('en-US')
  const pending = managedFetch('/exports/records', { headers })
  setLocale('zh-CN')
  const actual = fetch.mock.calls[0][1].headers
  expect(actual.get('Accept-Language')).toBe('en-US')
  expect(actual.get('Authorization')).toBe('test-only')
  expect(actual.get('X-Export')).toBe('reports')
  expect(headers.has('Accept-Language')).toBe(false)
  finish({ body: null, status: 204 })
  await pending
  expect(withLocaleHeaders().get('Accept-Language')).toBe('zh-CN')
})

it('sends the selected language on streaming requests while retaining original body and identity', async () => {
  const fetch = vi.fn().mockResolvedValue({ ok: true, body: { getReader: () => ({ read: () => Promise.resolve({ done: true }) }) } })
  vi.stubGlobal('fetch', fetch)
  setLocale('en-US')
  const onComplete = vi.fn()
  consumeSSE('/assistant', { headers: { Authorization: 'test-only' }, body: { value: '用户原文' }, onComplete })
  expect(fetch.mock.calls[0][1].headers.get('Accept-Language')).toBe('en-US')
  expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('test-only')
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ value: '用户原文' })
  await vi.waitFor(() => expect(onComplete).toHaveBeenCalledOnce())
})
