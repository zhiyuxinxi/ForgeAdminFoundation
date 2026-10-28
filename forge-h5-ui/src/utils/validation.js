// 移动端表单字段校验（与 PC 端共享同一规则）。
// 手机号规则与 forge-admin-ui/src/utils/validation-presets.js 的 PHONE 预设一致：
// ^1[3-9]\d{9}$（11 位国内手机号）；空值允许（与既有行为一致），非空必须匹配。

export const PHONE_PATTERN = /^1[3-9]\d{9}$/

export const PHONE_ERROR_MESSAGE = '请输入正确的手机号'

/**
 * 可选手机号校验：空值（null / 空白）允许，非空必须符合项目手机号规则。
 *
 * @param {*} value 手机号
 * @returns {boolean} 通过返回 true
 */
export function isValidOptionalPhone(value) {
  const phone = String(value ?? '').trim()
  return phone === '' || PHONE_PATTERN.test(phone)
}
