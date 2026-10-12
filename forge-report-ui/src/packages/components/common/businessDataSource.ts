import { get, post, put, del } from '@/api/http'

export type BusinessRequestMethod = 'get' | 'post' | 'put' | 'delete'

export interface BusinessDataSourceConfig {
  url: string
  method: BusinessRequestMethod
  dataPath: string
  paramMap: Record<string, string>
  autoLoad?: boolean
}

export const defaultDataSourceConfig: BusinessDataSourceConfig = {
  url: '',
  method: 'get',
  dataPath: 'data',
  paramMap: {},
  autoLoad: true
}

export const getByPath = (target: any, path?: string) => {
  if (!path) return target
  return path.split('.').reduce((current, key) => current?.[key], target)
}

export const ensureDataSourceConfig = (value?: Partial<BusinessDataSourceConfig>): BusinessDataSourceConfig => ({
  ...defaultDataSourceConfig,
  ...(value || {}),
  paramMap: {
    ...(value?.paramMap || {})
  }
})

export const buildParamsFromContext = (
  paramMap: Record<string, string> | undefined,
  context: Record<string, any> = {}
) => {
  const params: Record<string, any> = {}
  Object.entries(paramMap || {}).forEach(([targetKey, sourceKey]) => {
    const value = getByPath(context, sourceKey)
    if (value !== undefined && value !== null && value !== '') params[targetKey] = value
  })
  return params
}

export const fetchBusinessData = async (
  source: Partial<BusinessDataSourceConfig> | undefined,
  context: Record<string, any> = {},
  extraParams: Record<string, any> = {}
) => {
  const config = ensureDataSourceConfig(source)
  if (!config.url) return undefined
  const params = {
    ...buildParamsFromContext(config.paramMap, context),
    ...extraParams
  }
  const methodMap = { get, post, put, delete: del }
  const method = methodMap[config.method || 'get']
  const payload = await method(config.url, params)
  return config.dataPath ? getByPath(payload, config.dataPath) : payload
}

export const normalizeArrayData = (value: any): any[] => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.records)) return value.records
  if (Array.isArray(value?.list)) return value.list
  return []
}

export const normalizeObjectData = (value: any): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}
