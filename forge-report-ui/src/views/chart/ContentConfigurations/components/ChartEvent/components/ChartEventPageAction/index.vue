<template>
  <n-collapse-item :title="$ui(&quot;页面动作&quot;)" name="3">
    <template #header-extra>
      <n-space :size="6">
        <n-button type="primary" tertiary size="small" @click.stop="addAction(ComponentActionType.GO_PAGE)">
          <template #icon>
            <n-icon>
              <add-icon />
            </n-icon>
          </template>
          {{ $ui("跳转") }}
        </n-button>
        <n-button type="info" tertiary size="small" @click.stop="addAction(ComponentActionType.OPEN_MODAL)">
          {{ $ui("弹窗") }}
        </n-button>
      </n-space>
    </template>

    <div v-if="!actionItems.length" class="no-data go-flex-center">
      <img :src="noData" alt="暂无数据" />
      <n-text :depth="3">{{ $ui("暂无页面动作") }}</n-text>
    </div>

    <n-card
      v-for="(item, index) in actionItems"
      :key="item.id"
      class="n-card-shallow page-action-card"
      size="small"
    >
      <n-space justify="space-between" align="center">
        <n-space align="center" :size="6">
          <n-tag :bordered="false" :type="item.type === ComponentActionType.OPEN_MODAL ? 'info' : 'primary'" size="small">
            {{ actionTypeLabel(item.type) }} {{ index + 1 }}
          </n-tag>
          <n-tag v-if="item.type !== ComponentActionType.CLOSE_MODAL && !isValidTarget(item.targetPageId)" :bordered="false" type="error" size="small">
            {{ $ui("目标页面已失效") }}
          </n-tag>
        </n-space>
        <n-button type="error" text size="small" @click="deleteAction(item.id)">
          <template #icon>
            <n-icon>
              <close-icon />
            </n-icon>
          </template>
        </n-button>
      </n-space>

      <n-divider />

      <setting-item-box name="动作类型" :alone="true">
        <n-select
          v-model:value="item.type"
          size="tiny"
          :options="actionTypeOptions"
          @update:value="() => handleActionTypeChange(item)"
        />
      </setting-item-box>

      <setting-item-box name="触发事件" :alone="true">
        <n-select v-model:value="item.trigger" size="tiny" :options="triggerOptions" />
      </setting-item-box>

      <setting-item-box v-if="item.type !== ComponentActionType.CLOSE_MODAL" :name="item.type === ComponentActionType.OPEN_MODAL ? $ui(&quot;目标弹窗&quot;) : $ui(&quot;目标页面&quot;)" :alone="true">
        <n-select
          v-model:value="item.targetPageId"
          size="tiny"
          filterable
          clearable
          :placeholder="$ui(&quot;选择项目内页面&quot;)"
          :options="targetOptions(item)"
        />
      </setting-item-box>

      <setting-item-box v-if="item.type === ComponentActionType.GO_PAGE" name="切换动画" :alone="true">
        <n-select
          v-model:value="item.transition"
          size="tiny"
          clearable
          :placeholder="$ui(&quot;跟随项目默认&quot;)"
          :options="transitionOptions"
        />
      </setting-item-box>

      <template v-if="item.type !== ComponentActionType.CLOSE_MODAL">
        <n-divider>{{ $ui("下钻参数") }}</n-divider>

        <div v-if="!item.params?.length" class="param-empty">
          <n-text depth="3">{{ $ui("未配置参数") }}</n-text>
        </div>

        <div v-for="(param, paramIndex) in item.params" :key="param.id" class="param-row">
          <n-input
            v-model:value.trim="param.targetKey"
            class="param-target"
            size="tiny"
            :placeholder="$ui(&quot;目标参数名&quot;)"
          />
          <n-select
            v-model:value="param.source"
            class="param-source"
            size="tiny"
            :options="paramSourceOptions"
            @update:value="() => handleParamSourceChange(param)"
          />
          <n-tooltip :show-arrow="false" trigger="hover">
            <template #trigger>
              <n-button class="param-delete" size="tiny" type="error" tertiary circle @click="deleteParam(item, paramIndex)">
                <template #icon>
                  <n-icon>
                    <trash-icon />
                  </n-icon>
                </template>
              </n-button>
            </template>
            {{ $ui("删除参数") }}
          </n-tooltip>
          <n-input
            v-if="param.source === 'static'"
            v-model:value="param.value"
            class="param-value"
            size="tiny"
            :placeholder="$ui(&quot;固定值&quot;)"
          />
          <n-select
            v-else-if="param.source === 'pageContext'"
            v-model:value="param.sourceKey"
            class="param-value"
            size="tiny"
            filterable
            tag
            :options="pageContextOptions"
            :placeholder="$ui(&quot;上下文字段&quot;)"
          />
          <n-input
            v-else
            v-model:value="param.sourceKey"
            class="param-value"
            size="tiny"
            :placeholder="$ui(&quot;组件字段，如 option.dataset&quot;)"
          />
          <n-input
            v-model:value="param.fallbackValue"
            class="param-fallback"
            size="tiny"
            :placeholder="$ui(&quot;默认值&quot;)"
          />
        </div>

        <n-button size="tiny" type="primary" dashed class="param-add" @click="addParam(item)">
          {{ $ui("新增参数") }}
        </n-button>
      </template>
    </n-card>
  </n-collapse-item>
</template>

<script lang="ts" setup>
import { uiText } from '@/i18n/display'
import { computed } from 'vue'
import { SettingItemBox } from '@/components/Pages/ChartItemSetting'
import { BaseEvent, ComponentActionType } from '@/enums/eventEnum'
import { getUUID } from '@/utils'
import { pageContextParamOptions } from '@/utils/requestDynamicParams'
import { icon } from '@/plugins'
import noData from '@/assets/images/canvas/noData.png'
import { useTargetData } from '../../../hooks/useTargetData.hook'
import type {
  ComponentAction,
  DrillParamBinding,
  DrillParamSource,
  ReportPageTransition
} from '@/store/modules/chartEditStore/chartEditStore.d'

const { CloseIcon, AddIcon, TrashIcon } = icon.ionicons5
const { targetData, chartEditStore } = useTargetData()

const triggerOptions = [
  { get label() { return uiText("单击") }, value: BaseEvent.ON_CLICK },
  { get label() { return uiText("双击") }, value: BaseEvent.ON_DBL_CLICK }
]

const actionTypeOptions = [
  { get label() { return uiText("页面跳转") }, value: ComponentActionType.GO_PAGE },
  { get label() { return uiText("打开弹窗") }, value: ComponentActionType.OPEN_MODAL },
  { get label() { return uiText("关闭弹窗") }, value: ComponentActionType.CLOSE_MODAL }
]

const transitionOptions: Array<{ label: string; value: ReportPageTransition }> = [
  { get label() { return uiText("无动画") }, value: 'none' },
  { get label() { return uiText("淡入淡出") }, value: 'fade' },
  { get label() { return uiText("左滑") }, value: 'slide-left' },
  { get label() { return uiText("右滑") }, value: 'slide-right' },
  { get label() { return uiText("缩放") }, value: 'zoom' }
]

const paramSourceOptions: Array<{ label: string; value: DrillParamSource }> = [
  { get label() { return uiText("固定值") }, value: 'static' },
  { get label() { return uiText("组件字段") }, value: 'componentField' },
  { get label() { return uiText("页面上下文") }, value: 'pageContext' }
]

const pageContextOptions = pageContextParamOptions

const ensureActions = () => {
  targetData.value.events.actions = targetData.value.events.actions || []
  return targetData.value.events.actions
}

const actionItems = computed(() => {
  return ensureActions().filter(action => [
    ComponentActionType.GO_PAGE,
    ComponentActionType.OPEN_MODAL,
    ComponentActionType.CLOSE_MODAL
  ].includes(action.type as ComponentActionType))
})

const pageOptions = computed(() => {
  return chartEditStore.getProjectPages
    .filter(page => page.pageType !== 'modal')
    .map(page => ({
    label: `${page.name}${page.id === chartEditStore.getHomePageId ? '（首页）' : ''}`,
    value: page.id
  }))
})

const modalOptions = computed(() => {
  return chartEditStore.getProjectPages
    .filter(page => page.pageType === 'modal')
    .map(page => ({
      label: page.name,
      value: page.id
    }))
})

const targetOptions = (action: ComponentAction) => {
  return action.type === ComponentActionType.OPEN_MODAL ? modalOptions.value : pageOptions.value
}

const isValidTarget = (pageId?: string) => {
  return !!pageId && chartEditStore.getProjectPages.some(page => page.id === pageId)
}

const getDefaultTargetPageId = (type: ComponentAction['type']) => {
  const candidates = chartEditStore.getProjectPages.filter(page => {
    if (type === ComponentActionType.OPEN_MODAL) return page.pageType === 'modal'
    return page.pageType !== 'modal'
  })
  if (type === ComponentActionType.OPEN_MODAL) {
    return candidates[0]?.id || ''
  }
  return candidates.find(page => page.id !== chartEditStore.getActivePageId)?.id
    || chartEditStore.getHomePageId
    || candidates[0]?.id
    || ''
}

const actionTypeLabel = (type: ComponentAction['type']) => {
  if (type === ComponentActionType.OPEN_MODAL) return '弹窗'
  if (type === ComponentActionType.CLOSE_MODAL) return '关闭'
  return '跳转'
}

const addAction = (type: ComponentAction['type'] = ComponentActionType.GO_PAGE) => {
  const action: ComponentAction = {
    id: getUUID(),
    trigger: BaseEvent.ON_CLICK,
    type,
    targetPageId: type === ComponentActionType.CLOSE_MODAL ? undefined : getDefaultTargetPageId(type),
    transition: undefined,
    params: []
  }
  ensureActions().push(action)
}

const handleActionTypeChange = (action: ComponentAction) => {
  if (action.type === ComponentActionType.CLOSE_MODAL) {
    action.targetPageId = undefined
    action.params = []
    action.transition = undefined
    return
  }
  action.targetPageId = getDefaultTargetPageId(action.type)
  if (action.type === ComponentActionType.OPEN_MODAL) {
    action.transition = undefined
  }
}

const deleteAction = (id: string) => {
  targetData.value.events.actions = ensureActions().filter(action => action.id !== id)
}

const addParam = (action: ComponentAction) => {
  action.params = action.params || []
  action.params.push({
    id: getUUID(),
    targetKey: '',
    source: 'static',
    value: '',
    sourceKey: '',
    fallbackValue: ''
  })
}

const deleteParam = (action: ComponentAction, index: number) => {
  action.params?.splice(index, 1)
}

const handleParamSourceChange = (param: DrillParamBinding) => {
  if (param.source === 'static') {
    param.sourceKey = ''
    param.value = param.value ?? ''
  } else if (param.source === 'pageContext') {
    param.sourceKey = param.sourceKey || 'regionCode'
    param.value = ''
  } else {
    param.sourceKey = param.sourceKey || 'option.dataset'
    param.value = ''
  }
}
</script>

<style lang="scss" scoped>
.page-action-card {
  :deep(.n-divider) {
    margin: 10px 0;
  }
}

.no-data {
  flex-direction: column;
  width: 100%;
  img {
    width: 120px;
  }
}

.param-empty {
  padding: 2px 0 10px;
}

.param-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px 28px;
  gap: 6px;
  align-items: center;
  min-width: 0;
  padding: 8px;
  margin-bottom: 8px;
  border: 1px solid rgba(var(--app-theme-rgb), 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.025);

  :deep(.n-input),
  :deep(.n-select) {
    min-width: 0;
  }

  .param-target,
  .param-source,
  .param-value,
  .param-fallback {
    min-width: 0;
  }

  .param-delete {
    width: 28px;
    height: 28px;
  }

  .param-value {
    grid-column: 1 / -1;
  }

  .param-fallback {
    grid-column: 1 / -1;
  }
}

.param-add {
  width: 100%;
}
</style>
