import assert from 'node:assert/strict'
import test from 'node:test'
import { HOME_PAGE, buildLoginUrl, normalizeRedirectTarget } from '../route.js'

// 子路径部署登录重定向回归（单元层）：
// 守卫登录重定向必须基于同源 hash 路由；redirect 参数防开放重定向。

test('buildLoginUrl keeps the login page path with a single-encoded redirect', () => {
  assert.equal(buildLoginUrl('/pages/index/index'), '/pages/login/index?redirect=%2Fpages%2Findex%2Findex')
  assert.equal(buildLoginUrl('/pages/todo'), '/pages/login/index?redirect=%2Fpages%2Ftodo')
  assert.equal(buildLoginUrl(''), '/pages/login/index')
})

test('normalizeRedirectTarget accepts local page paths', () => {
  assert.equal(normalizeRedirectTarget('/pages/index/index'), '/pages/index/index')
  assert.equal(normalizeRedirectTarget('/pages/todo'), '/pages/todo')
  assert.equal(normalizeRedirectTarget('/pages/foo?tab=1'), '/pages/foo?tab=1')
})

test('normalizeRedirectTarget rejects open-redirect vectors and falls back to home', () => {
  for (const evil of [
    'https://evil.example/x',
    'http://evil.example',
    '//evil.example/x',
    'javascript:alert(1)',
    '/pages\\evil',
    '/pages/../secret',
    'https://evil.example/%2Fpages%2Findex%2Findex',
  ]) {
    assert.equal(normalizeRedirectTarget(evil), HOME_PAGE, `redirect ${evil} must be rejected`)
  }
  assert.equal(normalizeRedirectTarget(null), HOME_PAGE)
  assert.equal(normalizeRedirectTarget(undefined), HOME_PAGE)
  assert.equal(normalizeRedirectTarget(''), HOME_PAGE)
  assert.equal(normalizeRedirectTarget('   '), HOME_PAGE)
})

test('normalizeRedirectTarget tolerates whitespace around a valid path', () => {
  assert.equal(normalizeRedirectTarget('  /pages/index/index  '), '/pages/index/index')
})
