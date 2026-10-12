<template>
  <section class="operation-toolbar" :style="rootStyle">
    <strong v-if="option.title">{{ option.title }}</strong>
    <div class="toolbar-actions" :class="`align-${option.align}`">
      <button
        v-for="action in option.actions"
        :key="action.label"
        :class="`is-${action.style || 'primary'}`"
        :disabled="loadingMap[action.label]"
        @click="runAction(action)"
      >
        {{ loadingMap[action.label] ? $ui("处理中") : action.label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, PropType, reactive } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { get, post, put, del } from '@/api/http'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { switchPreviewPage } from '@/views/preview/utils/storage'
import { option as defaultOption } from './config'
import type { ToolbarAction } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const chartEditStore = useChartEditStore()
const loadingMap = reactive<Record<string, boolean>>({})
const option = computed(() => ({
  ...defaultOption,
  ...(props.chartConfig.option || {}),
  style: { ...defaultOption.style, ...(props.chartConfig.option?.style || {}) },
  actions: props.chartConfig.option?.actions || []
}))
const rootStyle = computed(() => ({
  '--tool-accent': option.value.style.accentColor,
  '--tool-text': option.value.style.textColor,
  '--tool-muted': option.value.style.mutedColor,
  '--tool-panel': option.value.style.panelColor,
  '--tool-border': option.value.style.borderColor
}))

const refreshPage = () => {
  window.dispatchEvent(new CustomEvent('forge-report-refresh', { detail: { time: Date.now() } }))
  window['$message']?.success('已触发刷新')
}

const runAction = async (action: ToolbarAction) => {
  if (action.confirm && !window.confirm(action.confirmText || `确认执行“${action.label}”？`)) return
  loadingMap[action.label] = true
  try {
    if (action.type === 'refresh') {
      refreshPage()
    } else if (action.type === 'goPage' && action.targetPageId) {
      switchPreviewPage(action.targetPageId, chartEditStore.getRuntimePageContext || {})
    } else if (action.type === 'openModal' && action.targetPageId) {
      chartEditStore.openModal(action.targetPageId, chartEditStore.getRuntimePageContext || {})
    } else if (action.type === 'closeModal') {
      chartEditStore.closeModal()
    } else if (action.type === 'link' && action.url) {
      window.open(action.url, action.openTarget || '_self')
    } else if (action.type === 'request' && action.url) {
      const methodMap = { get, post, put, delete: del }
      const method = methodMap[action.method || 'post']
      await method(action.url, chartEditStore.getRuntimePageContext || {})
      window['$message']?.success('操作成功')
    }
  } finally {
    loadingMap[action.label] = false
  }
}
</script>

<style scoped lang="scss">
.operation-toolbar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  width: 100%;
  height: 100%;
  padding: 12px;
  color: var(--tool-text);
  border: 1px solid var(--tool-border);
  border-radius: 8px;
  background: var(--tool-panel);
}

strong {
  color: var(--tool-muted);
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  min-width: 0;
}

.align-left {
  justify-content: flex-start;
}

.align-center {
  justify-content: center;
}

.align-right {
  justify-content: flex-end;
}

button {
  height: 32px;
  padding: 0 14px;
  color: var(--tool-text);
  border: 1px solid var(--tool-border);
  border-radius: 6px;
  cursor: pointer;
  background: rgba(8, 30, 58, 0.7);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.is-primary {
  color: #00131f;
  border-color: transparent;
  background: var(--tool-accent);
}

.is-success {
  color: #34d399;
}

.is-warning {
  color: #fbbf24;
}

.is-error {
  color: #fb7185;
}
</style>
