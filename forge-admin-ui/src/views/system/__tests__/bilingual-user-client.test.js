import { readFileSync } from 'node:fs'
import { afterEach, describe, expect, it } from 'vitest'
import { formatUiText, localizeLabel, setLocale } from '@/locales'

describe('login and system user English resource coverage', () => {
  afterEach(() => setLocale('zh-CN'))

  it('fills confirmed login and user fixed-display resource gaps', () => {
    setLocale('en-US')
    expect(localizeLabel('开箱即用的后台模板')).toBe('Ready-to-use admin template')
    expect(localizeLabel('点击刷新验证码')).toBe('Click to refresh the verification code')
    expect(localizeLabel('三方登录失败，请重试')).toBe('Third-party sign-in failed. Try again')
    expect(localizeLabel('新密码')).toBe('New Password')
    expect(localizeLabel('请输入新密码')).toBe('Enter a new password')
    expect(localizeLabel('配置用户的角色、组织、岗位和租户关系')).toBe('Configure the user\'s roles, organizations, positions and tenants')
  })

  it('keeps current pages connected to the shared locale resolver', () => {
    const login = readFileSync('src/views/login/index.vue', 'utf8')
    const user = readFileSync('src/views/system/user.vue', 'utf8')
    expect(login).toContain("localizeLabel('开箱即用的后台模板')")
    expect(login).toContain("localizeLabel('三方登录失败，请重试')")
    expect(user).toContain(":placeholder=\"localizeLabel('请输入新密码')\"")
    expect(user).toContain("localizeLabel('配置用户的角色、组织、岗位和租户关系')")
  })
})

describe('public client display localization', () => {
  afterEach(() => setLocale('zh-CN'))
  it('resolves client duration labels without changing user data', () => {
    setLocale('en-US')
    expect(formatUiText('查看客户端：{0}', ['Client A'])).toBe('View client: Client A')
    expect(formatUiText('{0} 天', ['2'])).toBe('2 days')
    const client = readFileSync('src/views/system/client.vue', 'utf8')
    expect(client).toContain('computed(() => translateUiMetadata([')
    expect(client).toContain('formatUiText as translateUiText')
  })
})
