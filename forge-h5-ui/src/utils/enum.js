/**
 * 枚举管理工具
 */
import { reactive } from 'vue'
import { request } from './index'

const enumCache = reactive({})

export async function getEnum(type, version = 'v1', forceRefresh = false) {
  if (!type) {
    console.warn('getEnum: 枚举类型不能为空')
    return []
  }

  if (!forceRefresh && enumCache[type] && enumCache[type].length) {
    return enumCache[type]
  }

  try {
    const url = version === 'v1' ? `/dictionary/${type}` : '/elementData/dictionary'
    const params = version === 'v1' ? {} : { typeId: type }

    const res = await request.get(url, { params })

    if (res && res.length) {
      let cacheData
      if (version === 'v1') {
        cacheData = res.map(item => ({
          label: item.dataName || '',
          value: item.dataId || '',
          ...item
        }))
      } else {
        cacheData = res
      }
      enumCache[type] = cacheData
      return cacheData
    }

    enumCache[type] = []
    return []
  } catch (error) {
    console.error(`获取枚举 ${type} 失败:`, error)
    return []
  }
}

export async function batchGetEnum(types) {
  if (!types || !Array.isArray(types) || types.length === 0) {
    console.warn('batchGetEnum: 枚举类型数组不能为空')
    return {}
  }

  const promises = types.map(item => {
    if (typeof item === 'string') {
      return getEnum(item)
    } else if (item && item.type) {
      return getEnum(item.type, item.version || 'v1', item.forceRefresh)
    }
    return Promise.resolve([])
  })

  const results = await Promise.all(promises)
  const enumData = {}
  types.forEach((item, index) => {
    const type = typeof item === 'string' ? item : item.type
    enumData[type] = results[index]
  })

  return enumData
}

export function clearEnumCache(types) {
  if (!types) return
  const typeArray = Array.isArray(types) ? types : [types]
  typeArray.forEach(type => {
    if (enumCache[type]) {
      delete enumCache[type]
    }
  })
}

export function clearAllEnumCache() {
  Object.keys(enumCache).forEach(key => {
    delete enumCache[key]
  })
}

export function getEnumCache() {
  return { ...enumCache }
}

export function getEnumLabel(type, value) {
  if (!type || value === undefined || value === null) return ''
  const enumList = enumCache[type]
  if (!enumList || !Array.isArray(enumList)) return ''
  const item = enumList.find(e => String(e.value) === String(value))
  return item ? item.label : ''
}

export function getEnumValue(type, label) {
  if (!type || !label) return ''
  const enumList = enumCache[type]
  if (!enumList || !Array.isArray(enumList)) return ''
  const item = enumList.find(e => e.label === label)
  return item ? item.value : ''
}

export function hasEnumCache(type) {
  return !!(enumCache[type] && enumCache[type].length)
}

export { enumCache }

export default {
  getEnum,
  batchGetEnum,
  clearEnumCache,
  clearAllEnumCache,
  getEnumCache,
  getEnumLabel,
  getEnumValue,
  hasEnumCache,
  enumCache
}
