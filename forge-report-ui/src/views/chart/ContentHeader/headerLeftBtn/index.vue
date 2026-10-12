<template>
  <div class="header-left-btn">
    <n-button class="home-command" size="small" quaternary @click="goHomeHandle()">
      <template #icon>
        <n-icon :depth="3">
          <home-icon></home-icon>
        </n-icon>
      </template>
    </n-button>

    <div class="command-group">
      <span class="command-label">{{ $ui("面板") }}</span>
      <n-tooltip v-for="item in btnList" :key="item.key" placement="bottom" trigger="hover">
        <template #trigger>
          <n-button class="command-btn" size="small" ghost :type="styleHandle(item)" :focusable="false" @click="clickHandle(item)">
            <component :is="item.icon"></component>
            <span class="command-text">{{ $ui(item.title) }}</span>
          </n-button>
        </template>
        <span>{{ $ui(item.title) }}</span>
      </n-tooltip>
    </div>

    <n-button class="ai-command" size="small" ghost :type="isAIActive ? 'primary' : 'default'" @click="toggleAIHandle">
      <template #icon>
        <n-icon size="16"><sparkles-icon /></n-icon>
      </template>
      <span class="command-text">{{ $ui("AI 助手") }}</span>
    </n-button>

    <div class="command-group history-group">
      <span class="command-label">{{ $ui("历史") }}</span>
      <n-tooltip v-for="item in historyList" :key="item.key" placement="bottom" trigger="hover">
        <template #trigger>
          <n-button class="command-btn icon-only" size="small" ghost type="primary" :disabled="!item.select" @click="clickHistoryHandle(item)">
            <component :is="item.icon"></component>
          </n-button>
        </template>
        <span>{{ $ui(item.title) }}</span>
      </n-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, reactive, Ref, toRefs, unref } from 'vue'
import { renderIcon, goDialog, goHome } from '@/utils'
import { icon } from '@/plugins'
import { useRemoveKeyboard } from '../../hooks/useKeyboard.hook'

import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'

import { useChartHistoryStore } from '@/store/modules/chartHistoryStore/chartHistoryStore'
import { HistoryStackEnum } from '@/store/modules/chartHistoryStore/chartHistoryStore.d'

import { useChartLayoutStore } from '@/store/modules/chartLayoutStore/chartLayoutStore'
import { ChartLayoutStoreEnum } from '@/store/modules/chartLayoutStore/chartLayoutStore.d'

import { useAIStore } from '@/store/modules/aiStore/aiStore'

const { AlbumsIcon, LayersIcon, BarChartIcon, PrismIcon, HomeIcon, ArrowUndoIcon, ArrowRedoIcon, SparklesIcon } = icon.ionicons5
const chartLayoutStore = useChartLayoutStore()
const { getPages, getLayers, getCharts, getDetails } = toRefs(chartLayoutStore)
const chartEditStore = useChartEditStore()
const chartHistoryStore = useChartHistoryStore()
const aiStore = useAIStore()

type LeftPanelKey = ChartLayoutStoreEnum.PAGES | ChartLayoutStoreEnum.CHARTS | ChartLayoutStoreEnum.LAYERS

const activeLeftPanel = computed<LeftPanelKey | 'ai' | ''>(() => {
  if (aiStore.getAIPanelVisible) return 'ai'
  if (getCharts.value) return ChartLayoutStoreEnum.CHARTS
  if (getPages.value) return ChartLayoutStoreEnum.PAGES
  if (getLayers.value) return ChartLayoutStoreEnum.LAYERS
  return ''
})
const isAIActive = computed(() => activeLeftPanel.value === 'ai')

interface ItemType<T> {
  key: T
  select: Ref<boolean> | boolean
  title: string
  icon: any
}

const btnList = reactive<ItemType<ChartLayoutStoreEnum>[]>([
  {
    key: ChartLayoutStoreEnum.PAGES,
    select: getPages,
    title: '画布页面',
    icon: renderIcon(AlbumsIcon)
  },
  {
    key: ChartLayoutStoreEnum.CHARTS,
    select: getCharts,
    title: '图表组件',
    icon: renderIcon(BarChartIcon)
  },
  {
    key: ChartLayoutStoreEnum.LAYERS,
    select: getLayers,
    title: '图层控制',
    icon: renderIcon(LayersIcon)
  },
  {
    key: ChartLayoutStoreEnum.DETAILS,
    select: getDetails,
    title: '详情设置',
    icon: renderIcon(PrismIcon)
  }
])

const isBackStack = computed(()=> chartHistoryStore.getBackStack.length> 1)

const isForwardStack = computed(()=> chartHistoryStore.getForwardStack.length> 0)

const historyList = reactive<ItemType<HistoryStackEnum>[]>([
  {
    key: HistoryStackEnum.BACK_STACK,
    select: isBackStack,
    title: '后退',
    icon: renderIcon(ArrowUndoIcon)
  },
  {
    key: HistoryStackEnum.FORWARD_STACK,
    select: isForwardStack,
    title: '前进',
    icon: renderIcon(ArrowRedoIcon)
  }
])

const getSelectedValue = (item: ItemType<any>) => Boolean(unref(item.select))

// store 描述的是展示的值，所以和 ContentConfigurations 的 collapsed 是相反的
const styleHandle = (item: ItemType<ChartLayoutStoreEnum>) => {
  if (item.key === ChartLayoutStoreEnum.DETAILS) {
    return getSelectedValue(item) ? '' : 'primary'
  }
  return activeLeftPanel.value === item.key ? 'primary' : ''
}

const setExclusiveLeftPanel = (key: LeftPanelKey | 'ai' | '') => {
  chartLayoutStore.setItem(ChartLayoutStoreEnum.PAGES, key === ChartLayoutStoreEnum.PAGES, false)
  chartLayoutStore.setItem(ChartLayoutStoreEnum.CHARTS, key === ChartLayoutStoreEnum.CHARTS, false)
  chartLayoutStore.setItem(ChartLayoutStoreEnum.LAYERS, key === ChartLayoutStoreEnum.LAYERS, false)
  aiStore.setAIPanelVisible(key === 'ai')
  setTimeout(() => {
    chartEditStore.computedScale()
  }, 260)
}

// 布局处理
const clickHandle = (item: ItemType<ChartLayoutStoreEnum>) => {
  if (item.key === ChartLayoutStoreEnum.DETAILS) {
    chartLayoutStore.setItem(item.key, !getSelectedValue(item))
    return
  }
  const key = item.key as LeftPanelKey
  setExclusiveLeftPanel(activeLeftPanel.value === key ? '' : key)
}

// 历史记录处理
const clickHistoryHandle = (item: ItemType<HistoryStackEnum>) => {
  switch (item.key) {
    case HistoryStackEnum.BACK_STACK:
      chartEditStore.setBack()
      break;
    case HistoryStackEnum.FORWARD_STACK:
      chartEditStore.setForward()
      break;
  }
}

const toggleAIHandle = () => {
  setExclusiveLeftPanel(isAIActive.value ? '' : 'ai')
}

// 返回首页
const goHomeHandle = () => {
  goDialog({
    message: uiText("返回将不会保存任何操作"),
    isMaskClosable: true,
    onPositiveCallback: () => {
      goHome()
      useRemoveKeyboard()
    }
  })
}
</script>
<style lang="scss" scoped>
.header-left-btn {
  margin-left: -12px;
  display: flex;
  align-items: center;
  gap: 10px;

  .command-group {
    height: 34px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 2px 6px 2px 10px;
    border-radius: 12px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.1);
    background: rgba(2, 6, 23, 0.26);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.025);
  }

  .history-group {
    padding-left: 8px;
    gap: 4px;
    background:
      linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.11), transparent 70%),
      rgba(2, 6, 23, 0.24);
  }

  .command-label {
    font-size: 10px;
    letter-spacing: 1px;
    @include fetch-color(4);
    padding-right: 3px;
    white-space: nowrap;
  }

  :deep(.n-button) {
    min-width: 34px;
    height: 30px;
    border-color: rgba(var(--app-theme-rgb), 0.12);
    background: rgba(15, 23, 42, 0.36);

    &:hover {
      border-color: rgba(var(--app-theme-rgb), 0.28);
      background: rgba(var(--app-theme-rgb), 0.1);
      box-shadow: 0 0 14px rgba(var(--app-theme-rgb), 0.16);
      transform: translateY(-1px);
    }
  }

  .home-command {
    width: 36px;
    height: 36px;
    border-radius: 12px;
  }

  .ai-command {
    height: 34px;
    border-radius: 12px;
    padding: 0 12px 0 8px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.1);
    background: rgba(2, 6, 23, 0.26);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.025);
    background:
      linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.09), transparent 70%),
      rgba(2, 6, 23, 0.24);

    :deep(.n-button__icon) {
      margin-right: 2px;
    }
  }

  .command-text {
    font-size: 11px;
    margin-left: 4px;
    letter-spacing: 0.2px;
  }

  .icon-only {
    width: 30px;
    border-radius: 999px;

    :deep(.n-button__icon) {
      margin: 0;
    }
  }
}
</style>
