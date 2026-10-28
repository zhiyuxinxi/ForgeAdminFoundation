import assert from 'node:assert/strict'
import test from 'node:test'
import { buildRoutePayload, unpackRoutePayload, ROUTE_PAYLOAD_VERSION } from '../route-payload.js'

// 版本化 base64url JSON payload 单次传输协议回归：
// 仅 URL-safe 字符、无 padding、接收端只解包一次；字段白名单与严格校验，
// 注入/超长/非法 payload 安全降级，path 仅本地受控解析。

const SCENARIOS = [
  { title: '联系方式', path: '/pages/test' },
  { title: '研发与号A等号B井号1问号', path: '/pages/test' },
  { title: '完成进度100%', path: '/pages/test' },
  { title: '正常中文标题', path: '/pages/lowcode/crud-page/orders', configKey: 'orders', mode: 'edit', recordId: '42', subtitle: '副标题' },
]

// 真实 ASCII 保留字符：&(38) =(61) #(35) ?(63) +(43) %(37)，与中文混排
const RESERVED_CHARS = String.fromCharCode(38, 61, 35, 63, 43, 37)
const RESERVED_TITLE = `中文${RESERVED_CHARS}标题`

for (const scenario of SCENARIOS) {
  test(`round trip preserves fields: ${scenario.title}`, () => {
    const payload = buildRoutePayload(scenario)
    // 仅 URL-safe 字符（A-Za-z0-9-_），无 padding '='，无 + / %
    assert.match(payload, /^[A-Za-z0-9_-]+$/, 'payload must be URL-safe base64url without padding')
    const unpacked = unpackRoutePayload({ q: payload })
    for (const key of ['title', 'path', 'configKey', 'mode', 'recordId', 'subtitle']) {
      assert.equal(unpacked[key], scenario[key] || '', `field ${key} round-trips`)
    }
  })
}

test('payload embeds the protocol version', () => {
  const payload = buildRoutePayload({ title: 'x' })
  const json = Buffer.from(payload.replace(/-/g, '+').replace(/_/g, '/') + '==', 'base64').toString('utf8')
  assert.equal(JSON.parse(json).v, ROUTE_PAYLOAD_VERSION)
})

test('title with REAL ASCII reserved chars (& = # ? + %) round-trips character-exactly', () => {
  // 字符码 38/61/35/63/43/37 → & = # ? + %；必须逐字符完全一致，不得截断/转义/走私
  assert.equal(RESERVED_CHARS, '&=#?+%')
  const payload = buildRoutePayload({ title: RESERVED_TITLE, path: '/pages/test' })
  assert.match(payload, /^[A-Za-z0-9_-]+$/, 'payload stays URL-safe despite reserved chars in title')
  const unpacked = unpackRoutePayload({ q: payload })
  assert.equal(unpacked.title, RESERVED_TITLE)
  for (let i = 0; i < RESERVED_TITLE.length; i += 1) {
    assert.equal(unpacked.title[i], RESERVED_TITLE[i], `char ${i} must match exactly`)
  }
  assert.equal(unpacked.path, '/pages/test')
})

test('title with literal percent-hex text is preserved as plain text (no mis-decode)', () => {
  // "%E8%81%94" 只是普通文本，接收端不得把它解码为"联"；"100%25" 也不得变成 "100%"
  const literal = '进度%E8%81%94%25测试'
  const unpacked = unpackRoutePayload({ q: buildRoutePayload({ title: literal }) })
  assert.equal(unpacked.title, literal, 'literal percent-hex must not be decoded')
  const percentLiteral = '完成100%25'
  assert.equal(unpackRoutePayload({ q: buildRoutePayload({ title: percentLiteral }) }).title, percentLiteral)
})

test('empty fields produce empty payload', () => {
  assert.equal(buildRoutePayload({}), '')
  assert.equal(buildRoutePayload({ title: '', path: '', configKey: '' }), '')
})

test('missing q falls back to strictly validated legacy query', () => {
  const unpacked = unpackRoutePayload({ title: '旧链接标题', path: '/pages/test', configKey: 'orders', mode: 'edit' })
  assert.equal(unpacked.title, '旧链接标题')
  assert.equal(unpacked.path, '/pages/test')
  assert.equal(unpacked.configKey, 'orders')
  assert.equal(unpacked.mode, 'edit')
})

test('invalid payload degrades safely to defaults', () => {
  assert.deepEqual(unpackRoutePayload({ q: 'not-base64url!!!' }), {
    title: '', path: '', configKey: '', mode: '', recordId: '', subtitle: '',
  })
  // 版本不符
  const wrongVersion = Buffer.from(JSON.stringify({ v: 99, title: 'x' })).toString('base64')
  assert.equal(unpackRoutePayload({ q: wrongVersion }).title, '')
  // 超长 payload
  assert.equal(unpackRoutePayload({ q: 'a'.repeat(3000) }).title, '')
})

test('over-long fields are truncated to limits; over-long raw payload is rejected', () => {
  const unpacked = unpackRoutePayload({ q: buildRoutePayload({ title: 'x'.repeat(200) }) })
  assert.equal(unpacked.title.length, 64)
  // 300 字符标题会被截断为 64，payload 仍在长度上限内 → 有效 payload（截断而非拒绝）
  assert.ok(buildRoutePayload({ title: 'x'.repeat(300), path: '/pages/test' }).length > 0)
  // 超长原始 payload（绕过构建器直接注入）→ 整体安全降级
  assert.equal(unpackRoutePayload({ q: 'a'.repeat(3000) }).title, '')
})

test('path rejects external/unsafe values and never enables navigation', () => {
  for (const evil of [
    'https://evil.example/x',
    'http://evil.example',
    '//evil.example',
    'javascript:alert(1)',
    '/pages/../secret',
    '/pages\\evil',
    '/pages/test?x=1',
  ]) {
    const unpacked = unpackRoutePayload({ q: buildRoutePayload({ title: 't', path: evil }) })
    assert.equal(unpacked.path, '', `path ${evil} must be rejected`)
  }
  assert.equal(unpackRoutePayload({ path: 'https://evil.example' }).path, '')
})

test('configKey injection (mode smuggling) is rejected', () => {
  const unpacked = unpackRoutePayload({ q: buildRoutePayload({ configKey: 'orders&mode=edit', title: 't' }) })
  assert.equal(unpacked.configKey, '')
  assert.equal(unpacked.mode, '')
  assert.equal(unpackRoutePayload({ configKey: 'a b c' }).configKey, '')
  assert.equal(unpackRoutePayload({ configKey: 'orders' }).configKey, 'orders')
})

test('mode is whitelisted; injected mode is dropped', () => {
  for (const mode of ['list', 'create', 'edit', 'detail', 'readonly', 'todo']) {
    assert.equal(unpackRoutePayload({ q: buildRoutePayload({ title: 't', mode }) }).mode, mode)
  }
  assert.equal(unpackRoutePayload({ q: buildRoutePayload({ title: 't', mode: 'admin' }) }).mode, '')
  assert.equal(unpackRoutePayload({ mode: 'system&title=x' }).mode, '')
})

test('legacy recordId/id alias and subtitle are validated', () => {
  assert.equal(unpackRoutePayload({ recordId: '42' }).recordId, '42')
  assert.equal(unpackRoutePayload({ id: '42' }).recordId, '42')
  assert.equal(unpackRoutePayload({ recordId: 'a b' }).recordId, '')
  assert.equal(unpackRoutePayload({ subtitle: '副标题' }).subtitle, '副标题')
})

test('payload is stable through an extra encodeURIComponent round trip (URL-safe)', () => {
  const payload = buildRoutePayload({ title: '研发与号A等号B井号1问号', path: '/pages/test' })
  // base64url 字符经 encodeURIComponent 不变 → 运行时再编码不会改变 payload
  assert.equal(encodeURIComponent(payload), payload)
})
