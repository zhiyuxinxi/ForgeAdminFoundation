// 待办/流程列表加载错误分类：区分 loading、成功空、网络错误、权限错误、服务错误。
// 网络错误与 403 权限错误**不能**降级为空列表（曾导致离线/无权限用户看到
// “暂无待处理/暂时已清空”）。

export const TODO_LOAD_STATE = {
  LOADING: 'loading',
  SUCCESS: 'success',
  EMPTY: 'empty',
  NETWORK_ERROR: 'network-error',
  FORBIDDEN: 'forbidden',
  SERVER_ERROR: 'server-error',
}

/**
 * 判定是否为网络错误。
 * 兼容 axios 1.x 的 ERR_NETWORK、本项目拦截器归一化的 NETWORK_ERROR，
 * 以及无 response 且非业务错误的情况。
 *
 * @param {*} error
 * @returns {boolean}
 */
export function isNetworkError(error) {
  if (!error) {
    return false
  }
  const code = String(error?.code || error?.error?.code || '')
  if (code === 'ERR_NETWORK' || code === 'NETWORK_ERROR' || code === 'ECONNABORTED') {
    return true
  }
  if (error?.isNetworkError === true) {
    return true
  }
  // axios 网络错误：无 response、无业务错误标记，且 message 含网络语义
  if (!error?.isBusinessError && !error?.response) {
    const message = String(error?.message || error?.error?.message || '')
    return /网络|network|ERR_|timeout|ECONN/i.test(message)
  }
  return false
}

/**
 * 判定是否为权限不足（403）。
 * 兼容业务响应体 code=403（“权限不足”）与 HTTP 403 状态。
 *
 * @param {*} error
 * @returns {boolean}
 */
export function isForbiddenError(error) {
  if (!error) {
    return false
  }
  // 逐个候选判断：任一 code/status 数值化等于 403 即命中。
  // 不能先用空值合并取单个值——Axios HTTP 403 形态 error.code='ERR_BAD_REQUEST'
  // 是字符串，Number('ERR_BAD_REQUEST') 为 NaN，会把 response.status=403 漏掉。
  const candidates = [
    error?.code,
    error?.error?.code,
    error?.status,
    error?.error?.status,
    error?.response?.status,
  ]
  return candidates.some(candidate => Number(candidate) === 403)
}

/**
 * 分类待办列表加载错误。
 *
 * @param {*} error
 * @returns {string} TODO_LOAD_STATE 之一；无错误返回 TODO_LOAD_STATE.SUCCESS
 */
export function classifyTodoLoadError(error) {
  if (!error) {
    return TODO_LOAD_STATE.SUCCESS
  }
  if (isNetworkError(error)) {
    return TODO_LOAD_STATE.NETWORK_ERROR
  }
  if (isForbiddenError(error)) {
    return TODO_LOAD_STATE.FORBIDDEN
  }
  return TODO_LOAD_STATE.SERVER_ERROR
}
