<template>
  <section class="alert-list" :style="rootStyle">
    <header>
      <h3>{{ option.title }}</h3>
      <span>{{ items.length }}</span>
    </header>
    <div class="alert-items">
      <article v-for="item in items" :key="`${item.title}-${item.time}`" :class="`is-${item.level}`">
        <i></i>
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
        </div>
        <time>{{ item.time }}</time>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, PropType, ref, unref } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { PREVIEW_PAGE_CONTEXT_KEY } from '@/utils/requestDynamicParams'
import { fetchBusinessData, normalizeArrayData } from '@/packages/components/common/businessDataSource'
import { option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const pageContext = inject(PREVIEW_PAGE_CONTEXT_KEY, ref({}))
const remoteItems = ref<any[]>([])
const option = computed(() => ({
  ...defaultOption,
  ...(props.chartConfig.option || {}),
  style: { ...defaultOption.style, ...(props.chartConfig.option?.style || {}) },
  dataSource: { ...defaultOption.dataSource, ...(props.chartConfig.option?.dataSource || {}) }
}))
const items = computed(() => remoteItems.value.length ? remoteItems.value : option.value.items)
const rootStyle = computed(() => ({
  '--alert-accent': option.value.style.accentColor,
  '--alert-critical': option.value.style.criticalColor,
  '--alert-warning': option.value.style.warningColor,
  '--alert-info': option.value.style.infoColor,
  '--alert-text': option.value.style.textColor,
  '--alert-muted': option.value.style.mutedColor,
  '--alert-panel': option.value.style.panelColor,
  '--alert-border': option.value.style.borderColor
}))

const fetchItems = async () => {
  const data = await fetchBusinessData(option.value.dataSource, unref(pageContext) || {})
  remoteItems.value = normalizeArrayData(data)
}

onMounted(() => {
  fetchItems()
  window.addEventListener('forge-report-refresh', fetchItems)
})

onUnmounted(() => {
  window.removeEventListener('forge-report-refresh', fetchItems)
})
</script>

<style scoped lang="scss">
.alert-list {
  width: 100%;
  height: 100%;
  padding: 14px;
  color: var(--alert-text);
  border: 1px solid var(--alert-border);
  border-radius: 8px;
  background: var(--alert-panel);
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h3 {
    margin: 0;
    font-size: 16px;
  }

  span {
    color: var(--alert-accent);
    font-weight: 800;
  }
}

.alert-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: calc(100% - 34px);
  overflow: auto;
}

article {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.035);

  i {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--alert-info);
  }

  strong,
  p {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  p {
    margin: 4px 0 0;
    color: var(--alert-muted);
    font-size: 12px;
  }

  time {
    color: var(--alert-muted);
    font-size: 12px;
  }
}

article.is-critical i {
  background: var(--alert-critical);
  box-shadow: 0 0 12px var(--alert-critical);
}

article.is-warning i {
  background: var(--alert-warning);
}
</style>
