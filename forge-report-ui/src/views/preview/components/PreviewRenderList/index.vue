<template>
  <div
    class="chart-item"
    :id="item.id"
    v-for="(item, index) in renderComponentList"
    :class="animationsClass(item.styles.animations)"
    :key="item.id"
    :style="{
      ...getComponentAttrStyle(item.attr, index),
      ...getTransformStyle(item.styles),
      ...getStatusStyle(item.status),
      ...getPreviewConfigStyle(item.preview),
      ...getBlendModeStyle(item.styles) as any,
      ...getSizeStyle(item.attr)
    }"
  >
    <!-- 分组 -->
    <preview-render-group
      v-if="item.isGroup"
      :groupData="(item as CreateComponentGroupType)"
      :groupIndex="index"
      :themeSetting="themeSetting"
      :themeColor="themeColor"
      :pageContext="renderPageContext"
    ></preview-render-group>

    <!-- 单组件 -->
    <component
      v-else
      :is="item.chartConfig.chartKey"
      :id="item.id"
      :chartConfig="item"
      :themeSetting="themeSetting"
      :themeColor="themeColor"
      :style="{ 
        ...getSizeStyle(item.attr),
        ...getFilterStyle(item.styles)
      }"
      v-on="useLifeHandler(item, renderPageContext)"
    ></component>
  </div>
</template>

<script setup lang="ts">
import { PropType, computed, onMounted, onUnmounted, provide } from 'vue'
import { useChartDataPondFetch } from '@/hooks'
import { PreviewRenderGroup } from '../PreviewRenderGroup/index'
import { CreateComponentGroupType, CreateComponentType } from '@/packages/index.d'
import { chartColors } from '@/settings/chartThemes/index'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { animationsClass, getFilterStyle, getTransformStyle, getBlendModeStyle, colorCustomMerge } from '@/utils'
import { getSizeStyle, getComponentAttrStyle, getStatusStyle, getPreviewConfigStyle } from '../../utils'
import { useLifeHandler } from '@/hooks'
import type { EditCanvasConfigType, RequestGlobalConfigType } from '@/store/modules/chartEditStore/chartEditStore.d'
import { PREVIEW_COMPONENT_LIST_KEY, PREVIEW_PAGE_CONTEXT_KEY } from '@/utils/requestDynamicParams'

// 初始化数据池
const { initDataPond, clearMittDataPondMap, removeDataPondInterfaces } = useChartDataPondFetch()
const chartEditStore = useChartEditStore()
let disposeDataPond: (() => void) | undefined

const props = defineProps({
  componentList: {
    type: Array as PropType<Array<CreateComponentType | CreateComponentGroupType>>,
    default: undefined
  },
  canvasConfig: {
    type: Object as PropType<EditCanvasConfigType>,
    default: undefined
  },
  requestGlobalConfig: {
    type: Object as PropType<RequestGlobalConfigType>,
    default: undefined
  },
  pageContext: {
    type: Object as PropType<Record<string, any>>,
    default: () => ({})
  }
})

const renderComponentList = computed(() => props.componentList || chartEditStore.componentList)
const renderCanvasConfig = computed(() => props.canvasConfig || chartEditStore.editCanvasConfig)
const renderRequestGlobalConfig = computed(() => props.requestGlobalConfig || chartEditStore.requestGlobalConfig)
const isMainRender = computed(() => !props.componentList)
const renderPageContext = computed(() => {
  if (props.componentList) return props.pageContext || {}
  return chartEditStore.getRuntimePageContext || {}
})
provide(PREVIEW_PAGE_CONTEXT_KEY, renderPageContext)
provide(PREVIEW_COMPONENT_LIST_KEY, renderComponentList)

// 主题色
const themeSetting = computed(() => {
  const chartThemeSetting = renderCanvasConfig.value.chartThemeSetting
  return chartThemeSetting
})

// 配置项
const themeColor = computed(() => {
  const colorCustomMergeData = colorCustomMerge(renderCanvasConfig.value.chartCustomThemeColorInfo)
  return colorCustomMergeData[renderCanvasConfig.value.chartThemeColor]
})

// 组件渲染结束初始化数据池
if (isMainRender.value) {
  clearMittDataPondMap()
}
onMounted(() => {
  disposeDataPond = initDataPond(useChartEditStore, {
    componentList: renderComponentList.value,
    requestGlobalConfig: renderRequestGlobalConfig.value,
    pageContext: renderPageContext
  })
})

onUnmounted(() => {
  disposeDataPond?.()
  if (isMainRender.value) {
    clearMittDataPondMap()
  } else {
    removeDataPondInterfaces(renderComponentList.value)
  }
})
</script>

<style lang="scss" scoped>
.chart-item {
  position: absolute;
  box-sizing: border-box;

  :deep(*),
  :deep(*::before),
  :deep(*::after) {
    box-sizing: border-box;
  }
}
</style>
