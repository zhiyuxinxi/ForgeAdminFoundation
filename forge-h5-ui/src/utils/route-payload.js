// 路由 query 单次传输协议：versioned base64url JSON payload。
//
// 背景：uni-app H5 的 navigateTo/redirectTo 会把 url 字符串中的 query 值**再编码一次**
// （% → %25），路由解析再解码一次；直接明文拼接会在值含 & / = / # / ? 时截断或参数走私，
// 对中文预编码又会造成 %25E8… 双编码。因此本模块把所有路由参数打包为
// 版本化 JSON → base64url（仅 URL-safe 字符、无 padding），整体放进单个 query 参数 q=。
// base64url 字符集（A-Za-z0-9-_）不会被运行时再编码，跨 uni-app H5 路由稳定。
//
// 字段白名单：title、path、configKey、mode、recordId、subtitle。
// 接收端只解包一次（不做递归 URL decode）；解析失败或字段非法时安全降级为默认值。
// legacy query（无 q 的既有深链）只读且逐字段严格校验后使用。
// path 仅允许本地受控解析（/pages/ 前缀），任何协议/双斜线/反斜线/javascript: 均拒绝，
// 且 path 只用于本地 configKey 提取，绝不用于跳转；mode/configKey 注入值无法引发外链或越权跳转。

export const ROUTE_PAYLOAD_VERSION = 1

const PAYLOAD_MAX_LENGTH = 2048

const FIELD_MAX = {
  title: 64,
  subtitle: 64,
  path: 256,
  configKey: 64,
  recordId: 64,
  mode: 16,
}

const ALLOWED_FIELDS = ['title', 'path', 'configKey', 'mode', 'recordId', 'subtitle']

const MODE_WHITELIST = new Set(['', 'list', 'create', 'edit', 'detail', 'readonly', 'todo'])

// configKey 只允许安全字符，注入的 &mode=edit 等直接判非法
const CONFIG_KEY_PATTERN = /^[A-Za-z0-9_.-]{1,64}$/

const RECORD_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/

// 本地页面路径：仅 /pages/ 前缀的相对路径；禁止协议、双斜线、反斜线、javascript:、控制字符、.. 段
const LOCAL_PATH_PATTERN = /^\/pages\/[A-Za-z0-9_\-./]+$/
const LOCAL_PATH_FORBIDDEN = /(^|\/)\.\.\/|https?:|javascript\s*:|\\|[\u0000-\u001f\u007f]/

// ---------- base64url ----------

function toBase64Url(text) {
  const bytes = new TextEncoder().encode(text)
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function fromBase64Url(value) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  const binary = atob(padded)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new TextDecoder().decode(bytes)
}

// ---------- 字段校验 ----------

function cleanText(value, max) {
  const text = String(value ?? '').replace(/[\u0000-\u001f\u007f]/g, '').trim()
  return text.length > max ? text.slice(0, max) : text
}

function sanitizeFields(raw = {}) {
  const fields = {}
  for (const key of ALLOWED_FIELDS) {
    fields[key] = ''
  }
  fields.title = cleanText(raw?.title, FIELD_MAX.title)
  fields.subtitle = cleanText(raw?.subtitle, FIELD_MAX.subtitle)

  const configKey = cleanText(raw?.configKey, FIELD_MAX.configKey)
  fields.configKey = CONFIG_KEY_PATTERN.test(configKey) ? configKey : ''

  const recordId = cleanText(raw?.recordId, FIELD_MAX.recordId)
  fields.recordId = RECORD_ID_PATTERN.test(recordId) ? recordId : ''

  const mode = cleanText(raw?.mode, FIELD_MAX.mode)
  fields.mode = MODE_WHITELIST.has(mode) ? mode : ''

  const path = cleanText(raw?.path, FIELD_MAX.path)
  fields.path = LOCAL_PATH_PATTERN.test(path) && !LOCAL_PATH_FORBIDDEN.test(path) ? path : ''

  return fields
}

// ---------- 对外 API ----------

/**
 * 发送端：把路由字段打包为 versioned base64url payload（仅 URL-safe 字符，无 padding）。
 * 全部字段为空或超长时返回空字符串（调用方不携带 query）。
 *
 * @param {Object} fields { title, path, configKey, mode, recordId, subtitle }
 * @returns {string} base64url payload 或 ''
 */
export function buildRoutePayload(fields = {}) {
  const clean = sanitizeFields(fields)
  const hasAny = ALLOWED_FIELDS.some(key => clean[key] !== '')
  if (!hasAny) {
    return ''
  }
  const json = JSON.stringify({ v: ROUTE_PAYLOAD_VERSION, ...clean })
  if (json.length > PAYLOAD_MAX_LENGTH) {
    return ''
  }
  return toBase64Url(json)
}

function tryDecodePayload(payloadStr) {
  try {
    const obj = JSON.parse(fromBase64Url(payloadStr))
    if (!obj || typeof obj !== 'object' || obj.v !== ROUTE_PAYLOAD_VERSION) {
      return null
    }
    return sanitizeFields(obj)
  }
  catch {
    return null
  }
}

/**
 * 接收端：解包路由 query。
 * - 优先解包 q=<payload>（versioned base64url JSON），只解包一次、不做递归 URL decode；
 *   缺失/超长/非法/版本不符时安全降级；
 * - 无 q 时兼容既有深链：legacy query 只读、逐字段严格校验后使用。
 *
 * @param {Object} query onLoad 收到的 query
 * @returns {{title:string, path:string, configKey:string, mode:string, recordId:string, subtitle:string}}
 */
export function unpackRoutePayload(query = {}) {
  const payloadStr = String(query?.q || '').trim()
  if (payloadStr) {
    if (payloadStr.length > PAYLOAD_MAX_LENGTH) {
      return sanitizeFields()
    }
    const fields = tryDecodePayload(payloadStr)
    if (fields) {
      return fields
    }
    // payload 非法：继续走 legacy（若存在）或默认降级
  }
  return sanitizeFields({
    title: query?.title,
    path: query?.path,
    configKey: query?.configKey ?? query?.config,
    mode: query?.mode,
    recordId: query?.recordId ?? query?.id,
    subtitle: query?.subtitle,
  })
}
