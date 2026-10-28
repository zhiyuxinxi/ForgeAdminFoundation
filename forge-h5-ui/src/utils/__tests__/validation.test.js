import assert from 'node:assert/strict'
import test from 'node:test'
import { isValidOptionalPhone, PHONE_PATTERN, PHONE_ERROR_MESSAGE } from '../validation.js'

// 手机号校验负例：与 PC 端 validation-presets.js 的 PHONE 预设（^1[3-9]\d{9}$）
// 共享同一规则；空值允许（与既有行为一致），非空必须合法。

test('PHONE_PATTERN matches the PC shared rule ^1[3-9]\\d{9}$', () => {
  assert.equal(PHONE_PATTERN.source, '^1[3-9]\\d{9}$')
  assert.equal(PHONE_ERROR_MESSAGE, '请输入正确的手机号')
})

test('valid China mobile numbers pass', () => {
  assert.equal(isValidOptionalPhone('13800138000'), true)
  assert.equal(isValidOptionalPhone('19912345678'), true)
  assert.equal(isValidOptionalPhone('15000000000'), true)
})

test('surrounding whitespace is trimmed before validation', () => {
  assert.equal(isValidOptionalPhone(' 13800138000 '), true)
})

test('empty phone stays allowed (current behavior preserved)', () => {
  assert.equal(isValidOptionalPhone(''), true)
  assert.equal(isValidOptionalPhone(null), true)
  assert.equal(isValidOptionalPhone(undefined), true)
  assert.equal(isValidOptionalPhone('   '), true)
})

test('defect reproduction: phone "123" must be rejected', () => {
  assert.equal(isValidOptionalPhone('123'), false)
})

test('too short phone rejected', () => {
  assert.equal(isValidOptionalPhone('1380013800'), false)
})

test('too long phone rejected', () => {
  assert.equal(isValidOptionalPhone('138001380001'), false)
})

test('invalid prefix rejected', () => {
  assert.equal(isValidOptionalPhone('23800138000'), false)
  assert.equal(isValidOptionalPhone('03800138000'), false)
})

test('non-numeric phone rejected', () => {
  assert.equal(isValidOptionalPhone('abc'), false)
  assert.equal(isValidOptionalPhone('13800abc000'), false)
  assert.equal(isValidOptionalPhone('138 0013 8000'), false)
})
