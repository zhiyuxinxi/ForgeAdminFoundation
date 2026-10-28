import assert from 'node:assert/strict'
import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import test from 'node:test'

// 登录页 form 契约：账号/密码/验证码必须位于真实 <form> 内（修复
// "Password field is not contained in a form" 浏览器告警），submit、Enter 与
// 按钮点击必须复用同一 guarded handler，且提交带防重复保护。
//
// 关键背景：uni-app 会把模板 <form> 编译成 <uni-form> 自定义元素（非原生
// form），因此 H5 分支必须通过 NativeForm 组件（render() 直接创建原生
// <form>）实现；非 H5 平台保留 uni-app <form>（各端原生表单组件）。
// 本文件同时校验源码契约（条件编译结构）与编译后 H5 产物（dist 存在时）。

const loginPageSource = readFileSync(
  fileURLToPath(new URL('../../pages/login/index.vue', import.meta.url)),
  'utf8',
)

const H5_DIST_ASSETS = fileURLToPath(new URL('../../../dist/build/h5/assets', import.meta.url))

function readLoginChunk() {
  if (!existsSync(H5_DIST_ASSETS)) {
    return null
  }
  const files = readdirSync(H5_DIST_ASSETS).filter(name => /^pages-login-index\..+\.js$/.test(name))
  if (!files.length) {
    return null
  }
  return readFileSync(path.join(H5_DIST_ASSETS, files[0]), 'utf8')
}

// ---------- 源码契约（构建前基线，条件编译结构） ----------

test('login page H5 branch wraps fields in NativeForm, non-H5 keeps uni-app form', () => {
  assert.match(loginPageSource, /#ifdef H5/, 'H5 条件编译分支必须存在')
  assert.match(loginPageSource, /<native-form class="form-stack" @submit="handleLogin">/)
  assert.match(loginPageSource, /#ifndef H5/, '非 H5 条件编译分支必须存在')
  assert.match(loginPageSource, /<form class="form-stack" @submit="handleLogin">/)

  // 密码输入必须位于 H5 分支的 native-form 之后（即原生 form 容器内）
  const nativeFormOpen = loginPageSource.indexOf('<native-form')
  const passwordInput = loginPageSource.indexOf(':password="!showPassword"')
  assert.ok(nativeFormOpen >= 0)
  assert.ok(passwordInput > nativeFormOpen, 'password input must be inside the H5 native form block')
})

test('login page marks all three inputs as named form members', () => {
  assert.match(loginPageSource, /name="username"/)
  assert.match(loginPageSource, /name="password"/)
  assert.match(loginPageSource, /name="code"/)
})

test('form submit, Enter confirm and button click reuse the same guarded handler', () => {
  // 两个分支（native-form / uni form）的 @submit 都绑定 handleLogin
  assert.equal(loginPageSource.match(/@submit="handleLogin"/g)?.length, 2)
  assert.equal(loginPageSource.match(/@confirm="handleLogin"/g)?.length, 4, 'password/code Enter 必须复用 handleLogin')
  assert.equal(loginPageSource.match(/@click="handleLogin"/g)?.length, 2)
})

test('login page imports, registers NativeForm and delegates to the guarded submit module', () => {
  assert.match(loginPageSource, /import NativeForm from '@\/components\/NativeForm\.vue'/)
  // LanguageSwitch may be registered alongside NativeForm; keep both consumers.
  assert.match(loginPageSource, /components:\s*\{[^}]*\bNativeForm\s*(?:,|\})/)
  assert.match(loginPageSource, /components:\s*\{[^}]*\bLanguageSwitch\s*(?:,|\})/)
  assert.match(loginPageSource, /import \{ createGuardedLoginHandler, LOGIN_INVALID_REASON \} from '@\/utils\/login-submit-guard'/)
  assert.match(loginPageSource, /createGuardedLoginHandler\(\{/)
  // handleLogin 只是转发到共享 guarded handler，不再内联校验/loading 逻辑
  assert.match(loginPageSource, /async handleLogin\(\) \{\s*return this\.loginHandler\(\)\s*\}/)
})

// ---------- 编译后契约（H5 生产构建产物） ----------

test('NativeForm component renders a native <form> element (source check)', () => {
  const nativeFormSource = readFileSync(
    fileURLToPath(new URL('../../components/NativeForm.vue', import.meta.url)),
    'utf8',
  )
  assert.match(nativeFormSource, /render\(\)\s*\{[\s\S]*h\('form'/)
  assert.match(nativeFormSource, /preventDefault/)
  assert.match(nativeFormSource, /\$emit\('submit'/)
})

test('compiled H5 login chunk creates a native <form>, not uni-form (requires build:h5)', (t) => {
  const chunk = readLoginChunk()
  if (!chunk) {
    t.skip('H5 dist 未构建：先执行 pnpm run build:h5 后再跑本契约测试')
    return
  }
  // NativeForm 的 render() 用原生标签 "form" 创建元素，且绑定 onSubmit
  assert.match(chunk, /["']form["'][^)]*onSubmit/, 'compiled login chunk must create a native form element with onSubmit')
  assert.match(chunk, /preventDefault/, 'native form submit must be prevented')
  // H5 登录页不再把 <form> 编译为 uni-form 自定义元素
  assert.doesNotMatch(chunk, /"uni-form"/, 'compiled H5 login chunk must not rely on uni-form')
  // 三个提交入口仍然编译为同一个 handleLogin（表单 submit / confirm / click）
  assert.match(chunk, /onSubmit[^,}]*handleLogin|handleLogin[^)]*onSubmit/)
})
