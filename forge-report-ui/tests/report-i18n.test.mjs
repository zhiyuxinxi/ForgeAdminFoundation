import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import vm from 'node:vm'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'

const require = createRequire(import.meta.url)
const requireVite = createRequire(require.resolve('vite/package.json'))
const { buildSync } = requireVite('esbuild')
const root = fileURLToPath(new URL('../', import.meta.url))
const bundle = buildSync({
  stdin: { contents: `export * from './src/i18n/locale'; export * from './src/i18n/display'; export { computed, watch } from 'vue'; export { axisConfig } from './src/packages/chartConfiguration/echarts/axis';`, resolveDir: root, loader: 'ts' },
  absWorkingDir: root, bundle: true, write: false, platform: 'node', format: 'cjs',
  define: { 'process.env.NODE_ENV': '"test"' },
  logLevel: 'silent'
}).outputFiles[0].text

function runtime(preference) {
  const draft = '{"title":"颜色","dataset":[{"name":"保存","value":"EN"}],"id":"静态-ID"}'
  const stored = new Map([['GO_CHART_STORAGE_LIST', draft]])
  if (preference !== undefined) stored.set('GO_LANG', preference)
  const events = new Map()
  let reloads = 0
  const document = { documentElement: { lang: '' }, createElement: () => ({}) }
  const localStorage = { getItem: key => stored.get(key) ?? null, setItem: (key, value) => stored.set(key, value) }
  const sandbox = { module: { exports: {} }, exports: {}, console, process, setTimeout, clearTimeout, document, localStorage,
    window: { addEventListener: (event, listener) => events.set(event, listener), location: { reload: () => reloads++ } } }
  vm.runInNewContext(bundle, sandbox)
  return { api: sandbox.module.exports, stored, document, draft, events, reloads: () => reloads }
}

test('report language defaults safely and restores only supported preferences', () => {
  for (const value of [undefined, 'broken-json', '{"lang":"de"}', 'null']) {
    const r = runtime(value)
    assert.equal(r.api.reportLanguage.value, 'ZH')
    assert.equal(r.document.documentElement.lang, 'zh-CN')
    assert.equal(r.api.uiText('保存'), '保存')
  }
  assert.equal(runtime('{"lang":"EN"}').api.requestLanguage(), 'en-US')
})

test('report language switches live, persists and preserves unsaved report bytes', () => {
  const r = runtime()
  const label = r.api.computed(() => r.api.uiText('保存'))
  r.api.changeReportLanguage('EN')
  assert.equal(label.value, 'Save')
  assert.equal(r.api.requestLanguage(), 'en-US')
  assert.equal(r.document.documentElement.lang, 'en-US')
  assert.equal(JSON.parse(r.stored.get('GO_LANG')).lang, 'EN')
  assert.equal(r.stored.get('GO_CHART_STORAGE_LIST'), r.draft)
  assert.equal(r.reloads(), 0)
  r.api.changeReportLanguage('ZH')
  assert.equal(label.value, '保存')
  assert.equal(r.stored.get('GO_CHART_STORAGE_LIST'), r.draft)
  r.stored.set('GO_LANG', '{"lang":"EN"}')
  r.events.get('storage')({ key: 'GO_LANG' })
  assert.equal(label.value, 'Save')
})

test('display translations preserve interpolation, unknown content and option values', () => {
  const r = runtime('{"lang":"EN"}')
  assert.equal(r.api.uiText('确定删除素材“{0}”吗？', { 0: '保存/中文客户数据' }), 'Delete asset “保存/中文客户数据”?')
  assert.equal(r.api.uiText('客户填入的标题与数据'), '客户填入的标题与数据')
  assert.equal(r.api.uiText('分', {}, 'currency'), 'Cents')
  const serializeValues = value => Array.isArray(value)
    ? value.map(serializeValues)
    : value && typeof value === 'object'
      ? Object.fromEntries(Object.entries(value).filter(([key]) => key !== 'label').map(([key, item]) => [key, serializeValues(item)]))
      : value
  const before = JSON.stringify(serializeValues(r.api.axisConfig))
  r.api.changeReportLanguage('ZH')
  assert.equal(JSON.stringify(serializeValues(r.api.axisConfig)), before)
  const dictionary = JSON.parse(readFileSync(new URL('../src/i18n/display-messages.json', import.meta.url)))
  assert.ok(Object.keys(dictionary).length > 1700)
  assert.ok(Object.values(dictionary).every(value => typeof value === 'string' && value.trim()))
})
