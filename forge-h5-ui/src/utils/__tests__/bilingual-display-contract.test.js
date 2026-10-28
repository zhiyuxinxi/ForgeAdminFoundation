import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'
import { normalizeLocale, translateDisplay } from '../../locales/display.js'
import labels from '../../locales/labels.js'

function source(relativePath) {
  return readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8')
}

const localeSource = source('../../locales/index.js')
const appEntrySource = source('../../pages/app-entry.vue')
const lowcodeSource = source('../../pages/lowcode-runtime.vue')
const todoSource = source('../../pages/todo.vue')
const mineSource = source('../../pages/mine/index.vue')
const tabsSource = source('../../components/AiTabs.vue')
const lowcodeFieldSource = source('../../components/lowcode/LowcodeField.vue')
const interceptorSource = source('../http/interceptors.js')
const fileSource = source('../file.js')
const fileUploadSource = source('../../components/AiFileUpload.vue')
const imageUploadSource = source('../../components/AiImageUpload.vue')
const signatureSource = source('../../components/AiSignaturePad.vue')

let activeLocale = 'zh-CN'
const L = (value, parameters) => translateDisplay(value, activeLocale, parameters)

function withLocale(nextLocale, run) {
  const originalLocale = activeLocale
  try {
    activeLocale = normalizeLocale(nextLocale)
    run()
  } finally {
    activeLocale = originalLocale
  }
}

test('fixed H5 validation, retry and shared tabs have English display consumers', () => {
  assert.equal(labels['请输入正确的手机号'], 'Enter a valid mobile number')
  assert.match(mineSource, /toast\(L\(PHONE_ERROR_MESSAGE\)/)
  assert.match(todoSource, /<button class="retry-button"[^>]*>\{\{ L\("重新加载"\) \}\}<\/button>/)
  assert.match(tabsSource, /\{\{ L\(tab\.label \|\| tab\) \}\}/)
})

test('low-code fields localize display metadata without rewriting schema values', () => {
  assert.match(lowcodeFieldSource, /localizeLabel\(source\.label, source\.labelI18n\)/)
  assert.match(lowcodeFieldSource, /const translations = source\[`\$\{key\}I18n`\] \|\| settings\[`\$\{key\}I18n`\]/)
  assert.match(lowcodeFieldSource, /\.\.\.option, label: option\.labelIsData \? option\.label : localizeLabel\(/)
})

test('language changes recompute defaults instead of storing a translated snapshot', () => {
  assert.match(localeSource, /export const locale = ref\(readSavedLocale\(\)\)/)
  assert.match(localeSource, /translateDisplay\(value, locale\.value, parameters\)/)
  assert.match(appEntrySource, /title\.value = params\.title \|\| '应用功能'/)
  assert.doesNotMatch(appEntrySource, /title\.value = params\.title \|\| L\(/)
  assert.match(lowcodeSource, /title\.value = params\.title \|\| '低代码应用'/)
  assert.match(lowcodeSource, /subtitle\.value = params\.subtitle \|\| '移动端运行页'/)
  assert.match(todoSource, /categoryOptions\.value = \[\{ get "label"\(\) \{ return L\("全部流程"\) \}/)

  withLocale('en-US', () => {
    assert.equal(activeLocale, 'en-US')
    assert.equal(L('应用功能'), 'App features')
    assert.equal(L('低代码应用'), 'Low-code app')
    assert.equal(L('重新加载'), 'Reload')
  })
})

test('switching locale preserves user drafts, filters, opinions and business payload identities', () => {
  const state = {
    login: { username: 'example-user', password: '原始密码', code: '1234' },
    profile: { realName: '用户输入姓名', phone: '13800138000' },
    approvalComment: '用户填写的审批意见',
    filters: { keyword: '客户自填条件', category: 'stable-category-code' },
    payload: { id: '9007199254740993123', status: 'PENDING', perms: 'example:record:submit' },
  }
  const before = JSON.stringify(state)
  withLocale('en-US', () => {
    assert.equal(JSON.stringify(state), before)
  })
})

test('unknown customer text and machine identities remain verbatim in English mode', () => {
  withLocale('en-US', () => {
    assert.equal(L('客户自定义审批意见'), '客户自定义审批意见')
    assert.equal(L('example:record:add'), 'example:record:add')
    assert.equal(L('9007199254740993123'), '9007199254740993123')
  })
})

test('axios and direct API fetch consumers read the current locale for Accept-Language', () => {
  assert.match(interceptorSource, /config\.headers\['Accept-Language'\] = getLocale\(\)/)
  for (const consumer of [fileSource, fileUploadSource, imageUploadSource, signatureSource]) {
    assert.match(consumer, /['"]Accept-Language['"]\s*:\s*getLocale\(\)/)
  }
})
