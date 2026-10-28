// 登录提交守卫：表单 submit、键盘确认（Enter）与按钮点击复用同一个 guarded handler。
// 请求进行中忽略重复触发，避免重复提交；页面仅通过 createGuardedLoginHandler 接入。

export const LOGIN_INVALID_REASON = {
  FIELDS: 'fields',
  CAPTCHA: 'captcha',
}

function validateLoginPayload({ username, password, code, codeKey }) {
  if (!username || !password || !code) {
    return { ok: false, reason: LOGIN_INVALID_REASON.FIELDS }
  }
  if (!codeKey) {
    return { ok: false, reason: LOGIN_INVALID_REASON.CAPTCHA }
  }
  return { ok: true }
}

/**
 * 创建登录提交守卫。
 *
 * @param {Object} options
 * @param {() => {username: string, password: string, code: string, codeKey: string}} options.getPayload
 *   返回当前表单与验证码状态，守卫会统一 trim 用户名/验证码，密码保持原值。
 * @param {(payload: {username: string, password: string, code: string, codeKey: string}) => Promise} options.submit
 *   实际登录请求，返回 Promise。
 * @param {(reason: string, payload: Object) => void} options.onInvalid
 *   校验失败回调，reason 为 LOGIN_INVALID_REASON 之一。
 * @param {(pending: boolean) => void} options.onPending
 *   请求开始/结束回调，用于同步页面 loading 态。
 * @param {(result: any, payload: Object) => void} options.onSuccess
 *   登录成功回调。
 * @param {(error: any, payload: Object) => void} options.onFailure
 *   登录失败回调。
 * @returns {() => Promise<undefined|*>} 稳定可复用的 guarded handler
 */
export function createGuardedLoginHandler({
  getPayload,
  submit,
  onInvalid,
  onPending,
  onSuccess,
  onFailure,
}) {
  let pending = false

  async function guardedSubmit() {
    if (pending) {
      return undefined
    }

    const raw = getPayload()
    const payload = {
      username: String(raw?.username || '').trim(),
      password: String(raw?.password || ''),
      code: String(raw?.code || '').trim(),
      codeKey: String(raw?.codeKey || '').trim(),
    }

    const validation = validateLoginPayload(payload)
    if (!validation.ok) {
      onInvalid(validation.reason, payload)
      return undefined
    }

    pending = true
    onPending(true)
    try {
      const result = await submit(payload)
      onSuccess(result, payload)
      return result
    }
    catch (error) {
      onFailure(error, payload)
      return undefined
    }
    finally {
      pending = false
      onPending(false)
    }
  }

  return guardedSubmit
}
