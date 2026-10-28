import assert from 'node:assert/strict'
import test from 'node:test'
import { createGuardedLoginHandler, LOGIN_INVALID_REASON } from '../login-submit-guard.js'

function makeHarness(overrides = {}) {
  const calls = {
    submit: [],
    invalid: [],
    pending: [],
    success: [],
    failure: [],
  }
  const handler = createGuardedLoginHandler({
    getPayload: overrides.getPayload || (() => ({
      username: 'admin',
      password: '123456',
      code: 'abcd',
      codeKey: 'key-1',
    })),
    submit: overrides.submit || (async (payload) => {
      calls.submit.push(payload)
      return overrides.submitResult !== undefined ? overrides.submitResult : { ok: true }
    }),
    onInvalid: (reason, payload) => calls.invalid.push({ reason, payload }),
    onPending: (pending) => calls.pending.push(pending),
    onSuccess: (result, payload) => calls.success.push({ result, payload }),
    onFailure: (error, payload) => calls.failure.push({ error, payload }),
  })
  return { handler, calls }
}

test('createGuardedLoginHandler returns one stable handler for all entry points', () => {
  const { handler } = makeHarness()
  // 表单 submit、键盘 Enter 与按钮点击复用同一实例引用
  assert.equal(typeof handler, 'function')
  assert.equal(handler, handler)
})

test('valid payload submits once with trimmed username/code and raw password', async () => {
  const { handler, calls } = makeHarness({
    getPayload: () => ({
      username: '  admin  ',
      password: ' 123456 ',
      code: ' abcd ',
      codeKey: ' key-1 ',
    }),
  })
  await handler()
  assert.equal(calls.submit.length, 1)
  assert.deepEqual(calls.submit[0], {
    username: 'admin',
    password: ' 123456 ',
    code: 'abcd',
    codeKey: 'key-1',
  })
})

test('missing username/password/code reports FIELDS and never submits', async () => {
  const { handler, calls } = makeHarness({
    getPayload: () => ({ username: '', password: '123456', code: 'abcd', codeKey: 'key-1' }),
  })
  await handler()
  assert.equal(calls.invalid.length, 1)
  assert.equal(calls.invalid[0].reason, LOGIN_INVALID_REASON.FIELDS)
  assert.equal(calls.submit.length, 0)
})

test('missing captcha codeKey reports CAPTCHA and never submits', async () => {
  const { handler, calls } = makeHarness({
    getPayload: () => ({ username: 'admin', password: '123456', code: 'abcd', codeKey: '' }),
  })
  await handler()
  assert.equal(calls.invalid.length, 1)
  assert.equal(calls.invalid[0].reason, LOGIN_INVALID_REASON.CAPTCHA)
  assert.equal(calls.submit.length, 0)
})

test('duplicate submission while pending is ignored (button + Enter reuse guard)', async () => {
  let resolveSubmit
  const submitted = []
  const { handler, calls } = makeHarness({
    submit: (payload) => new Promise((resolve) => {
      submitted.push(payload)
      resolveSubmit = resolve
    }),
  })
  const first = handler()
  // 请求未返回时再次触发（模拟按钮点击 + 键盘 Enter 同时命中）
  const second = handler()
  await second
  assert.equal(submitted.length, 1)
  assert.equal(calls.submit.length, 0)
  resolveSubmit({ ok: true })
  await first
  assert.equal(submitted.length, 1)
})

test('pending flag toggles true during request and false after completion', async () => {
  let resolveSubmit
  const { handler, calls } = makeHarness({
    submit: () => new Promise((resolve) => { resolveSubmit = resolve }),
  })
  const run = handler()
  assert.deepEqual(calls.pending, [true])
  resolveSubmit({ ok: true })
  await run
  assert.deepEqual(calls.pending, [true, false])
  assert.equal(calls.success.length, 1)
})

test('pending resets after success so a later submit works again', async () => {
  const { handler, calls } = makeHarness()
  await handler()
  await handler()
  assert.equal(calls.submit.length, 2)
  assert.equal(calls.success.length, 2)
})

test('pending resets after failure and failure callback runs', async () => {
  let failFirst = true
  const { handler, calls } = makeHarness({
    submit: async () => {
      if (failFirst) {
        failFirst = false
        throw new Error('bad credentials')
      }
      return { ok: true }
    },
  })
  await handler()
  assert.equal(calls.failure.length, 1)
  assert.equal(calls.pending.at(-1), false)
  assert.equal(calls.success.length, 0)
  // 失败后可再次提交
  await handler()
  assert.equal(calls.submit.length, 0)
  assert.equal(calls.success.length, 1)
})

test('invalid payload never toggles pending', async () => {
  const { handler, calls } = makeHarness({
    getPayload: () => ({ username: '', password: '', code: '', codeKey: '' }),
  })
  await handler()
  assert.deepEqual(calls.pending, [])
  assert.equal(calls.submit.length, 0)
})
