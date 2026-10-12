import { L } from '@/locales'
import { toast } from '@/utils/notify'
import { redirectToLogin as routeToLogin } from '@/utils/route'

let isConfirming = false

function redirectToLogin() {
  routeToLogin()
}

function normalizeErrorMessage(message) {
  if (!message) {
    return message
  }
  return String(message)
    .replace(/^com\.[\w.$]+Exception:\s*/i, '')
    .replace(/^.*BusinessException:\s*/i, '')
    .trim()
}

export function resolveResError(code, message, needTip = true) {
  message = normalizeErrorMessage(message)
  switch (code) {
    case '-8': // 令牌无效
    case 401:
      if (isConfirming || !needTip) return
      isConfirming = true
      toast(message || L("登录已过期，请重新登录"), { type: 'warning', duration: 1600 })
      setTimeout(() => {
        isConfirming = false
        redirectToLogin()
      }, 350)
      return false
    case 11007:
    case 11008:
      if (isConfirming || !needTip) return
      isConfirming = true
      toast(message || L("登录状态已失效，请重新登录"), { type: 'warning', duration: 1600 })
      setTimeout(() => {
        isConfirming = false
        redirectToLogin()
      }, 350)
      return false
    case 403:
      message = message || L("请求被拒绝")
      break
    case 404:
      message = message || L("请求资源或接口不存在")
      break
    case 500:
      message = message || L("服务器发生异常")
      break
    default:
      message = message ?? L("【{0}】: 未知异常!", [code])
      break
  }
  if (needTip) {
    toast(message, { type: code === 403 ? 'warning' : 'error', duration: 2200 })
  }
  return message
}
