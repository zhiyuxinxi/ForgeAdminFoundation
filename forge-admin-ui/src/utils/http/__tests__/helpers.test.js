import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { resolveResError } from '../helpers'
import { showRequestErrorDialog } from '../error-dialog'

const state = vi.hoisted(() => ({
  auth: { isLoggingOut: false, resetToken: vi.fn(), logout: vi.fn() },
}))

vi.mock('@/locales', () => ({
  localizeLabel: value => value,
  t: key => key,
}))

vi.mock('@/store', () => ({
  useAuthStore: () => state.auth,
}))

vi.mock('../error-dialog', () => ({
  showRequestErrorDialog: vi.fn(),
}))

beforeEach(() => {
  window.$message = { error: vi.fn() }
  window.$dialog = undefined
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('http 技术异常文案脱敏', () => {
  it.each([
    [500, 'I/O error while reading input message'],
    [500, 'java.lang.NullPointerException: target is null'],
    [502, 'org.springframework.web.client.ResourceAccessException: upstream failed'],
  ])('服务端状态 %s 不向用户暴露技术错误', (code, technicalMessage) => {
    const message = resolveResError(code, technicalMessage, true, {
      method: 'POST',
      url: '/business/leadership/assessplan/add',
      traceId: 'trace-assess',
    })

    expect(message).toBe('操作失败，请稍后重试或联系管理员')
    expect(window.$message.error).toHaveBeenCalledWith('操作失败，请稍后重试或联系管理员')
    expect(window.$message.error).not.toHaveBeenCalledWith(technicalMessage)
  })

  it('业务校验提示保持具体文案', () => {
    expect(resolveResError(400, '考核对象第 3 行缺少评分人', false))
      .toBe('考核对象第 3 行缺少评分人')
  })

  it('查询类错误使用轻量 Toast，不弹出模态错误框', () => {
    window.$dialog = { error: vi.fn() }
    resolveResError(404, '请求资源不存在', true, {
      method: 'GET',
      url: '/business/leadership/resplist/page',
    }, 'get')

    expect(window.$message.error).toHaveBeenCalledWith('请求资源不存在')
    expect(window.$dialog.error).not.toHaveBeenCalled()
    expect(showRequestErrorDialog).not.toHaveBeenCalled()
  })

  it('提交类错误保留模态错误框', () => {
    window.$dialog = { error: vi.fn() }
    resolveResError(500, '服务端异常', true, {
      method: 'POST',
      url: '/business/leadership/assessplan/add',
    }, 'post')

    expect(showRequestErrorDialog).toHaveBeenCalled()
    expect(window.$message.error).not.toHaveBeenCalled()
  })
})
