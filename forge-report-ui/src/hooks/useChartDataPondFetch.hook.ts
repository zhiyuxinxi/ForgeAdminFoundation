import { toRaw, watch, computed, ComputedRef } from 'vue'
import { customizeHttp } from '@/api/http'
import { CreateComponentType, CreateComponentGroupType, ChartFrameEnum } from '@/packages/index.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { RequestGlobalConfigType, RequestDataPondItemType } from '@/store/modules/chartEditStore/chartEditStore.d'
import { newFunctionHandle, intervalUnitHandle, normalizeDatasetForChart } from '@/utils'
import { getDynamicRequestParamDependencySnapshot } from '@/utils/requestDynamicParams'
import type { DynamicPageContext } from '@/utils/requestDynamicParams'

// 获取类型
type ChartEditStoreType = typeof useChartEditStore

// 数据池存储的数据类型
type DataPondMapType = {
  componentId: string
  updateCallback: (...args: any) => any
  filter?: string | undefined
  chartFrame?: ChartFrameEnum
}

// 数据池 Map 中请求对应 callback
const mittDataPondMap = new Map<string, DataPondMapType[]>()
const dataPondRuntimeStops: Array<() => void> = []
const dataPondRuntimeIntervals: any[] = []

const clearDataPondRuntime = () => {
  dataPondRuntimeStops.splice(0).forEach(stop => stop())
  dataPondRuntimeIntervals.splice(0).forEach(interval => clearInterval(interval))
}

const removeRuntimeStop = (stop: () => void) => {
  const index = dataPondRuntimeStops.indexOf(stop)
  if (index !== -1) dataPondRuntimeStops.splice(index, 1)
}

const removeRuntimeInterval = (interval: any) => {
  const index = dataPondRuntimeIntervals.indexOf(interval)
  if (index !== -1) dataPondRuntimeIntervals.splice(index, 1)
}

const collectComponentIds = (
  componentList: Array<CreateComponentType | CreateComponentGroupType>
) => {
  const ids = new Set<string>()
  const collect = (component: CreateComponentType | CreateComponentGroupType) => {
    ids.add(component.id)
    if (component.isGroup && Array.isArray((component as CreateComponentGroupType).groupList)) {
      (component as CreateComponentGroupType).groupList.forEach(collect)
    }
  }
  componentList.forEach(collect)
  return ids
}

// 创建单个数据项轮询接口
const newPondItemInterval = (
  requestGlobalConfig: RequestGlobalConfigType,
  requestDataPondItem: ComputedRef<RequestDataPondItemType>,
  componentList: Array<CreateComponentType | CreateComponentGroupType>,
  dataPondMapItem?: DataPondMapType[],
  pageContext?: DynamicPageContext
) => {
  if (!dataPondMapItem || !requestDataPondItem.value) return
  const componentIds = collectComponentIds(componentList)
  const currentDataPondMapItem = dataPondMapItem.filter(item => componentIds.has(item.componentId))
  if (!currentDataPondMapItem.length) return
  let fetchInterval: any = 0

  clearInterval(fetchInterval)

  // 请求
  const fetchFn = async () => {
    try {
      const res = await customizeHttp(
        toRaw(requestDataPondItem.value.dataPondRequestConfig),
        toRaw(requestGlobalConfig),
        toRaw(componentList),
        pageContext
      )
      if (res) {
        try {
          // 遍历更新回调函数
          currentDataPondMapItem.forEach(item => {
            const nextDataset = normalizeDatasetForChart(newFunctionHandle(res?.data, res, item.filter), item.chartFrame)
            item.updateCallback(nextDataset)
          })
        } catch (error) {
          console.error(error)
          return error
        }
      }
    } catch (error) {
      return error
    }
  }

  const stopWatch = watch(
    () => [
      requestDataPondItem.value?.dataPondRequestConfig.requestParams.Params,
      requestDataPondItem.value?.dataPondRequestConfig.dynamicRequestParams,
      getDynamicRequestParamDependencySnapshot(
        requestDataPondItem.value?.dataPondRequestConfig.dynamicRequestParams,
        componentList,
        pageContext
      )
    ],
    () => {
      fetchFn()
    },
    {
      immediate: false,
      deep: true
    }
  )
  dataPondRuntimeStops.push(stopWatch)


  // 立即调用
  fetchFn()


  const targetInterval = requestDataPondItem.value.dataPondRequestConfig.requestInterval
  const targetUnit = requestDataPondItem.value.dataPondRequestConfig.requestIntervalUnit

  const globalRequestInterval = requestGlobalConfig.requestInterval
  const globalUnit = requestGlobalConfig.requestIntervalUnit

  // 定时时间
  const time = targetInterval ? targetInterval : globalRequestInterval
  // 单位
  const unit = targetInterval ? targetUnit : globalUnit
  // 开启轮询
  if (time) {
    fetchInterval = setInterval(fetchFn, intervalUnitHandle(time, unit))
    dataPondRuntimeIntervals.push(fetchInterval)
  }

  return () => {
    stopWatch()
    removeRuntimeStop(stopWatch)
    if (fetchInterval) {
      clearInterval(fetchInterval)
      removeRuntimeInterval(fetchInterval)
    }
  }
}

/**
 * 数据池接口处理
 */
export const useChartDataPondFetch = () => {
  // 新增全局接口
  const addGlobalDataInterface = (
    targetComponent: CreateComponentType,
    useChartEditStore: ChartEditStoreType,
    updateCallback: (...args: any) => any
  ) => {
    const chartEditStore = useChartEditStore()
    const { requestDataPond } = chartEditStore.getRequestGlobalConfig

    // 组件对应的数据池 Id
    const requestDataPondId = targetComponent.request.requestDataPondId as string
    // 新增数据项
    const mittPondIdArr = mittDataPondMap.get(requestDataPondId) || []
    mittPondIdArr.push({
      componentId: targetComponent.id,
      updateCallback: updateCallback,
      filter: targetComponent.filter,
      chartFrame: targetComponent.chartConfig?.chartFrame
    })
    mittDataPondMap.set(requestDataPondId, mittPondIdArr)
  }

  // 清除旧数据
  const clearMittDataPondMap = () => {
    clearDataPondRuntime()
    mittDataPondMap.clear()
  }

  const removeDataPondInterfaces = (componentList: Array<CreateComponentType | CreateComponentGroupType>) => {
    const componentIds = collectComponentIds(componentList)
    for (const pondKey of mittDataPondMap.keys()) {
      const nextItems = (mittDataPondMap.get(pondKey) || []).filter(item => !componentIds.has(item.componentId))
      if (nextItems.length) {
        mittDataPondMap.set(pondKey, nextItems)
      } else {
        mittDataPondMap.delete(pondKey)
      }
    }
  }

  // 初始化数据池
  const initDataPond = (
    useChartEditStore: ChartEditStoreType,
    options: {
      requestGlobalConfig?: RequestGlobalConfigType
      componentList?: Array<CreateComponentType | CreateComponentGroupType>
      pageContext?: DynamicPageContext
    } = {}
  ) => {
    const chartEditStore = useChartEditStore()
    const requestGlobalConfig = options.requestGlobalConfig || chartEditStore.requestGlobalConfig
    const componentList = options.componentList || chartEditStore.componentList
    const disposers: Array<() => void> = []
    // 根据 mapId 查找对应的数据池配置
    for (let pondKey of mittDataPondMap.keys()) {
      const requestDataPondItem = computed(() => {
        return requestGlobalConfig.requestDataPond.find(item => item.dataPondId === pondKey)
      }) as ComputedRef<RequestDataPondItemType>
      if (requestDataPondItem.value) {
        const dispose = newPondItemInterval(
          requestGlobalConfig,
          requestDataPondItem,
          componentList,
          mittDataPondMap.get(pondKey),
          options.pageContext
        )
        if (dispose) disposers.push(dispose)
      }
    }
    return () => disposers.splice(0).forEach(dispose => dispose())
  }

  return {
    addGlobalDataInterface,
    clearMittDataPondMap,
    removeDataPondInterfaces,
    initDataPond
  }
}
