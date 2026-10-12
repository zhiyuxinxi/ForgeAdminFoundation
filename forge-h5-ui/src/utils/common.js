/**
 * @param {(object | string | number)} time
 * @param {string} format
 * @returns {string | null} 格式化后的时间字符串
 */
export function formatDateTime(time = undefined, format = 'YYYY-MM-DD HH:mm:ss') {
  // uni-app 环境简单实现，可引入 dayjs 替代
  const d = time ? new Date(time) : new Date()
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return format
    .replace('YYYY', d.getFullYear())
    .replace('MM', pad(d.getMonth() + 1))
    .replace('DD', pad(d.getDate()))
    .replace('HH', pad(d.getHours()))
    .replace('mm', pad(d.getMinutes()))
    .replace('ss', pad(d.getSeconds()))
}

export function formatDate(date = undefined, format = 'YYYY-MM-DD') {
  return formatDateTime(date, format)
}

/**
 * @param {Function} fn
 * @param {number} wait
 * @returns {Function}  节流函数
 */
export function throttle(fn, wait) {
  let context, args
  let previous = 0

  return function (...argArr) {
    const now = +new Date()
    context = this
    args = argArr
    if (now - previous > wait) {
      fn.apply(context, args)
      previous = now
    }
  }
}

/**
 * @param {Function} method
 * @param {number} wait
 * @param {boolean} immediate
 * @return {*} 防抖函数
 */
export function debounce(method, wait, immediate) {
  let timeout
  return function (...args) {
    const context = this
    if (timeout) {
      clearTimeout(timeout)
    }
    if (immediate) {
      const callNow = !timeout
      timeout = setTimeout(() => {
        timeout = null
      }, wait)
      if (callNow) {
        method.apply(context, args)
      }
    } else {
      timeout = setTimeout(() => {
        method.apply(context, args)
      }, wait)
    }
  }
}

/**
 * @param {number} time 毫秒数
 * @returns 睡一会儿
 */
export function sleep(time) {
  return new Promise(resolve => setTimeout(resolve, time))
}

/**
 * 判断字符串是否能转成json
 */
export const canParseToJson = (str) => {
  try {
    JSON.parse(str)
    return true
  } catch (e) {
    return false
  }
}
