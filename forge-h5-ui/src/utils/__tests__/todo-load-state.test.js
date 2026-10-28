import assert from 'node:assert/strict'
import test from 'node:test'
import {
  TODO_LOAD_STATE,
  classifyTodoLoadError,
  isForbiddenError,
  isNetworkError,
} from '../todo-load-state.js'

// 待办错误态回归：离线（ERR_NETWORK）与 403 权限不足必须识别为错误，
// 不能降级为“暂无待处理”；在线成功返回空列表仍是正常空态。

test('network error (axios ERR_NETWORK) is classified as network-error', () => {
  assert.equal(isNetworkError({ code: 'ERR_NETWORK' }), true)
  assert.equal(classifyTodoLoadError({ code: 'ERR_NETWORK' }), TODO_LOAD_STATE.NETWORK_ERROR)
})

test('network error (normalized NETWORK_ERROR) is classified as network-error', () => {
  assert.equal(isNetworkError({ code: 'NETWORK_ERROR' }), true)
  assert.equal(classifyTodoLoadError({ code: 'NETWORK_ERROR', message: '网络连接失败' }), TODO_LOAD_STATE.NETWORK_ERROR)
})

test('business 403 "权限不足" is classified as forbidden, never empty', () => {
  const error = { code: 403, message: '权限不足', isBusinessError: true }
  assert.equal(isForbiddenError(error), true)
  assert.equal(classifyTodoLoadError(error), TODO_LOAD_STATE.FORBIDDEN)
})

test('HTTP 403 status is classified as forbidden', () => {
  assert.equal(classifyTodoLoadError({ response: { status: 403 } }), TODO_LOAD_STATE.FORBIDDEN)
})

test('axios HTTP 403 with ERR_BAD_REQUEST code is classified as forbidden', () => {
  // Axios 4xx 形态：error.code 是字符串 'ERR_BAD_REQUEST'，真正状态在 response.status。
  // 逐个候选判断，不能被字符串 code 的 NaN 遮蔽。
  const error = { code: 'ERR_BAD_REQUEST', response: { status: 403 }, message: 'Request failed with status code 403' }
  assert.equal(isForbiddenError(error), true)
  assert.equal(classifyTodoLoadError(error), TODO_LOAD_STATE.FORBIDDEN)
})

test('nested business code 403 is classified as forbidden', () => {
  const error = { error: { code: 403 }, message: '权限不足' }
  assert.equal(isForbiddenError(error), true)
  assert.equal(classifyTodoLoadError(error), TODO_LOAD_STATE.FORBIDDEN)
})

test('axios HTTP 400 is NOT forbidden (falls to server-error)', () => {
  const error = { code: 'ERR_BAD_REQUEST', response: { status: 400 }, message: 'Request failed with status code 400' }
  assert.equal(isForbiddenError(error), false)
  assert.equal(classifyTodoLoadError(error), TODO_LOAD_STATE.SERVER_ERROR)
})

test('ordinary 500 is NOT forbidden (falls to server-error)', () => {
  assert.equal(isForbiddenError({ code: 500, message: '服务异常' }), false)
  assert.equal(classifyTodoLoadError({ code: 500, message: '服务异常' }), TODO_LOAD_STATE.SERVER_ERROR)
  assert.equal(isForbiddenError({ response: { status: 500 } }), false)
  assert.equal(classifyTodoLoadError({ response: { status: 500 } }), TODO_LOAD_STATE.SERVER_ERROR)
})

test('success (no error) is success; empty records remain a normal empty state', () => {
  assert.equal(classifyTodoLoadError(null), TODO_LOAD_STATE.SUCCESS)
  assert.equal(classifyTodoLoadError(undefined), TODO_LOAD_STATE.SUCCESS)
})

test('unknown server error is classified as server-error (not empty)', () => {
  assert.equal(classifyTodoLoadError({ code: 500, message: '服务异常' }), TODO_LOAD_STATE.SERVER_ERROR)
})

test('404 is not network and not forbidden', () => {
  assert.equal(isNetworkError({ code: 404 }), false)
  assert.equal(isForbiddenError({ code: 404 }), false)
})
