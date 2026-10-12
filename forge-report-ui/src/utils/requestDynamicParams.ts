import { uiText } from '@/i18n/display'
import axiosInstance from '@/api/axios'
import dayjs from 'dayjs'
import { unref } from 'vue'
import type { InjectionKey, Ref } from 'vue'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import type {
  DynamicRequestParamBinding,
  DynamicRequestParamTarget
} from '@/store/modules/chartEditStore/chartEditStore.d'
import type { CreateComponentType, CreateComponentGroupType } from '@/packages/index.d'

export interface DynamicParamOption {
  label: string
  value: string
}

export interface ResolvedDynamicRequestParams {
  Params: Record<string, unknown>
  Header: Record<string, unknown>
  Body: Record<string, unknown>
}

export type DynamicParamComponent = CreateComponentType | CreateComponentGroupType
export type DynamicPageContext = Record<string, any> | Ref<Record<string, any>>
export type DynamicComponentList = DynamicParamComponent[] | Ref<DynamicParamComponent[]>
export const PREVIEW_PAGE_CONTEXT_KEY: InjectionKey<DynamicPageContext> = Symbol('PREVIEW_PAGE_CONTEXT_KEY')
export const PREVIEW_COMPONENT_LIST_KEY: InjectionKey<DynamicComponentList> = Symbol('PREVIEW_COMPONENT_LIST_KEY')

interface ApiResponse<T = unknown> {
  code?: number
  data?: T
  msg?: string
  message?: string
}

interface LoginUserContext {
  userId?: number | string
  username?: string
  realName?: string
  phone?: string
  email?: string
  tenantId?: number | string
  roleIds?: Array<number | string>
  roleKeys?: string[]
  roles?: string[]
  permissions?: string[]
  apiPermissions?: string[]
  orgIds?: Array<number | string>
  mainOrgId?: number | string
  deptName?: string
  regionCode?: string
  regionName?: string
  regionLevel?: number
  regionFullName?: string
  regionAncestors?: string
  [key: string]: unknown
}

const USER_CACHE_TTL = 60 * 1000
let userCache: { expireAt: number; value: LoginUserContext | null } = {
  expireAt: 0,
  value: null
}

export const contextParamOptions: DynamicParamOption[] = [
  { get label() { return uiText("用户ID") }, value: 'userId' },
  { get label() { return uiText("账号") }, value: 'username' },
  { get label() { return uiText("姓名") }, value: 'realName' },
  { get label() { return uiText("手机号") }, value: 'phone' },
  { get label() { return uiText("邮箱") }, value: 'email' },
  { get label() { return uiText("租户ID") }, value: 'tenantId' },
  { get label() { return uiText("部门/主组织ID") }, value: 'mainOrgId' },
  { get label() { return uiText("组织ID列表") }, value: 'orgIds' },
  { get label() { return uiText("部门名称") }, value: 'deptName' },
  { get label() { return uiText("角色ID列表") }, value: 'roleIds' },
  { get label() { return uiText("角色编码列表") }, value: 'roleKeys' },
  { get label() { return uiText("权限标识列表") }, value: 'permissions' },
  { get label() { return uiText("行政区划编码") }, value: 'regionCode' },
  { get label() { return uiText("行政区划名称") }, value: 'regionName' },
  { get label() { return uiText("行政区划级别") }, value: 'regionLevel' },
  { get label() { return uiText("行政区划全名") }, value: 'regionFullName' },
  { get label() { return uiText("路由参数") }, value: 'route.params' },
  { get label() { return uiText("自定义上下文") }, value: 'reportContext' }
]

export const pageContextParamOptions: DynamicParamOption[] = [
  { get label() { return uiText("行政区划编码") }, value: 'regionCode' },
  { get label() { return uiText("行政区划名称") }, value: 'regionName' },
  { get label() { return uiText("对象ID") }, value: 'id' },
  { get label() { return uiText("对象名称") }, value: 'name' },
  { get label() { return uiText("指标编码") }, value: 'metricCode' },
  { get label() { return uiText("指标名称") }, value: 'metricName' }
]

export const componentParamOptions: DynamicParamOption[] = [
  { get label() { return uiText("当前值") }, value: 'value' },
  { get label() { return uiText("开始时间") }, value: 'startTime' },
  { get label() { return uiText("结束时间") }, value: 'endTime' },
  { get label() { return uiText("时间范围") }, value: 'dateRange' },
  { get label() { return uiText("分页页码") }, value: 'pageNum' },
  { get label() { return uiText("分页条数") }, value: 'pageSize' }
]

export const presetParamOptions: DynamicParamOption[] = [
  { get label() { return uiText("T-N 开始时间") }, value: 'tn-day-start' },
  { get label() { return uiText("T-N 结束时间") }, value: 'tn-day-end' }
]

const targetKeys: DynamicRequestParamTarget[] = ['Params', 'Header', 'Body']

export const createDynamicParamBinding = (): DynamicRequestParamBinding => ({
  id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
  enabled: true,
  target: 'Params',
  targetKey: '',
  source: 'context',
  sourceKey: 'userId',
  componentId: undefined,
  componentField: 'value',
  customValue: '',
  presetType: undefined,
  offsetDays: 1,
  dateFormat: 'YYYY-MM-DD HH:mm:ss',
  fallbackValue: ''
})

const isPlainObject = (value: unknown): value is Record<string, unknown> => {
  return Object.prototype.toString.call(value) === '[object Object]'
}

const getByPath = (source: unknown, path?: string): unknown => {
  if (!path) return undefined
  return path.split('.').reduce<unknown>((current, key) => {
    if (current === undefined || current === null) return undefined
    if (isPlainObject(current)) return current[key]
    return undefined
  }, source)
}

const getCurrentUserContext = async (): Promise<LoginUserContext> => {
  const now = Date.now()
  if (userCache.value && userCache.expireAt > now) {
    return userCache.value
  }

  try {
    const res = await axiosInstance({
      url: '/forge-report-api/auth/userInfo',
      method: 'get'
    }) as unknown as ApiResponse<LoginUserContext>
    const user = res.data || {}
    userCache = {
      expireAt: now + USER_CACHE_TTL,
      value: {
        ...user,
        roles: user.roles || user.roleKeys,
        orgId: user.mainOrgId,
        deptId: user.mainOrgId,
        name: user.realName
      }
    }
    return userCache.value || {}
  } catch (error) {
    userCache = { expireAt: now + USER_CACHE_TTL, value: {} }
    return {}
  }
}

const getContextValue = async (sourceKey?: string): Promise<unknown> => {
  if (sourceKey === 'route.params') {
    return (window as unknown as { route?: { params?: Record<string, unknown> } }).route?.params
  }
  if (sourceKey === 'reportContext') {
    return (window as unknown as { reportContext?: Record<string, unknown> }).reportContext
  }
  const user = await getCurrentUserContext()
  return getByPath(user, sourceKey)
}

const getPageContextValue = (
  sourceKey?: string,
  pageContext?: DynamicPageContext
): unknown => {
  const chartEditStore = useChartEditStore()
  const currentPageContext = pageContext ? unref(pageContext) : undefined
  return getByPath(currentPageContext || chartEditStore.getRuntimePageContext, sourceKey)
}

const getTabValue = (option: Record<string, unknown>) => {
  const label = option.tabLabel
  const dataset = option.dataset
  if (Array.isArray(dataset)) {
    const matched = dataset.find(item => isPlainObject(item) && item.label === label)
    if (isPlainObject(matched)) return matched.value
  }
  return label
}

export const getComponentDynamicValue = (
  component: DynamicParamComponent | undefined,
  field = 'value'
): unknown => {
  if (!component) return undefined
  const option = component.option as Record<string, unknown>
  const dataset = option.dataset

  if (field === 'startTime') {
    return Array.isArray(dataset) ? dataset[0] : undefined
  }
  if (field === 'endTime') {
    return Array.isArray(dataset) ? dataset[1] : undefined
  }
  if (field === 'dateRange') {
    return Array.isArray(dataset) ? dataset.join(',') : dataset
  }
  if (field === 'pageNum') {
    return option.pageValue
  }
  if (field === 'pageSize') {
    return option.pageSize
  }

  if (option.selectValue !== undefined) return option.selectValue
  if (option.inputValue !== undefined) return option.dataset ?? option.inputValue
  if (option.tabLabel !== undefined) return getTabValue(option)
  if (option.pageValue !== undefined) return option.pageValue
  return dataset
}

const stringifyDependencyValue = (value: unknown) => {
  if (value === undefined || value === null) return ''
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value)
  try {
    return JSON.stringify(value)
  } catch (error) {
    return String(value)
  }
}

export const getDynamicRequestParamDependencySnapshot = (
  bindings?: DynamicRequestParamBinding[],
  componentList: DynamicParamComponent[] = [],
  pageContext?: DynamicPageContext
) => {
  if (!bindings?.length) return []
  return bindings
    .filter(binding => binding.enabled && (
      (binding.source === 'component' && binding.componentId) ||
      (binding.source === 'pageContext' && binding.sourceKey)
    ))
    .map(binding => {
      if (binding.source === 'pageContext') {
        return `${binding.id}:pageContext:${binding.sourceKey}:${stringifyDependencyValue(getPageContextValue(binding.sourceKey, pageContext))}`
      }
      const component = componentList.find(item => item.id === binding.componentId)
      const field = binding.componentField || 'value'
      const value = getComponentDynamicValue(component, field)
      return `${binding.id}:${binding.componentId}:${field}:${stringifyDependencyValue(value)}`
    })
}

const getComponentValue = (
  binding: DynamicRequestParamBinding,
  componentList: DynamicParamComponent[] = []
): unknown => {
  const component = componentList.find(item => item.id === binding.componentId)
  return getComponentDynamicValue(component, binding.componentField)
}

const getPresetValue = (binding: DynamicRequestParamBinding): unknown => {
  const offsetDays = Number(binding.offsetDays ?? 1)
  const targetDate = dayjs().subtract(Number.isNaN(offsetDays) ? 1 : offsetDays, 'day')
  const format = binding.dateFormat || 'YYYY-MM-DD HH:mm:ss'
  if (binding.presetType === 'tn-day-start') {
    return targetDate.startOf('day').format(format)
  }
  if (binding.presetType === 'tn-day-end') {
    return targetDate.endOf('day').format(format)
  }
  return undefined
}

const getBindingValue = async (
  binding: DynamicRequestParamBinding,
  componentList: DynamicParamComponent[] = [],
  pageContext?: DynamicPageContext
): Promise<unknown> => {
  let value: unknown
  if (binding.source === 'context') {
    value = await getContextValue(binding.sourceKey)
  } else if (binding.source === 'pageContext') {
    value = getPageContextValue(binding.sourceKey, pageContext)
  } else if (binding.source === 'component') {
    value = getComponentValue(binding, componentList)
  } else if (binding.source === 'preset') {
    value = getPresetValue(binding)
  } else {
    value = binding.customValue
  }

  if (value === undefined || value === null || value === '') {
    return binding.fallbackValue
  }
  return value
}

export const resolveDynamicRequestParams = async (
  bindings?: DynamicRequestParamBinding[],
  componentList: DynamicParamComponent[] = [],
  pageContext?: DynamicPageContext
): Promise<ResolvedDynamicRequestParams> => {
  const result: ResolvedDynamicRequestParams = {
    Params: {},
    Header: {},
    Body: {}
  }

  if (!bindings?.length) return result

  for (const binding of bindings) {
    if (!binding.enabled || !binding.targetKey || !targetKeys.includes(binding.target)) {
      continue
    }
    const value = await getBindingValue(binding, componentList, pageContext)
    if (value === undefined || value === null || value === '') {
      continue
    }
    result[binding.target][binding.targetKey] = value
  }

  return result
}
