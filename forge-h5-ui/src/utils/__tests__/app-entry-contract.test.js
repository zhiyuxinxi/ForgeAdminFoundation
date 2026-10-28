import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

// app-entry / lowcode 路由传输契约（真实点击回归的源码层基线）：
// 统一使用 versioned base64url JSON payload（route-payload），禁止散落明文拼接；
// path 仅本地受控解析，禁止任何外链/重定向；接收端只解包一次。

const indexSource = readFileSync(fileURLToPath(new URL('../../pages/index/index.vue', import.meta.url)), 'utf8')
const appEntrySource = readFileSync(fileURLToPath(new URL('../../pages/app-entry.vue', import.meta.url)), 'utf8')
const lowcodeSource = readFileSync(fileURLToPath(new URL('../../pages/lowcode-runtime.vue', import.meta.url)), 'utf8')

test('home navigates to app-entry with a single payload param', () => {
  assert.match(indexSource, /`\/pages\/app-entry\?q=\$\{buildRoutePayload\([^`]*title: item\.label[^`]*\)\}`/)
  assert.doesNotMatch(indexSource, /app-entry\?title=/, 'no raw/encoded title param in app-entry URL')
})

test('home navigates to lowcode-runtime with a single payload param', () => {
  assert.match(indexSource, /`\/pages\/lowcode-runtime\?q=\$\{buildRoutePayload\([^`]*configKey: lowcodeConfigKey[^`]*\)\}`/)
  assert.doesNotMatch(indexSource, /lowcode-runtime\?configKey=/, 'no raw/encoded configKey param in lowcode URL')
})

test('app-entry unpacks payload once and forwards via payload only', () => {
  assert.match(appEntrySource, /import \{ buildRoutePayload, unpackRoutePayload \} from '@\/utils\/route-payload'/)
  assert.match(appEntrySource, /const params = unpackRoutePayload\(query \|\| \{\}\)/)
  assert.match(appEntrySource, /uni\.redirectTo\(\{ url: `\/pages\/lowcode-runtime\?q=\$\{payload\}` \}\)/)
  assert.doesNotMatch(appEntrySource, /join\('&'\)|query\.title \|\||encodeURIComponent\(query/, 'no plain query concatenation')
})

test('app-entry never navigates or redirects using the raw path param', () => {
  assert.match(appEntrySource, /path 仅用于解析本地低代码 configKey/)
  assert.match(appEntrySource, /resolveConfigKey\(path\)/)
  const redirectCalls = appEntrySource.match(/uni\.redirectTo\(\{ url: `\/pages\/lowcode-runtime\?q=/g) || []
  assert.equal(redirectCalls.length, 1, 'only one local redirect target allowed')
  assert.doesNotMatch(appEntrySource, /redirectTo\(\{ url: path \}|navigateTo\(\{ url: path \}|location\.href/)
})

test('lowcode-runtime unpacks payload and rebuilds redirect from validated fields', () => {
  assert.match(lowcodeSource, /import \{ buildRoutePayload, unpackRoutePayload \} from '@\/utils\/route-payload'/)
  assert.match(lowcodeSource, /const params = unpackRoutePayload\(query \|\| \{\}\)/)
  assert.match(lowcodeSource, /ensureLogin\(\{ redirect: `\/pages\/lowcode-runtime\?q=\$\{buildRoutePayload\(params\)\}` \}\)/)
  assert.doesNotMatch(lowcodeSource, /queryString\(|decodeURIComponent/, 'no plain query builder or receiver decode')
})
