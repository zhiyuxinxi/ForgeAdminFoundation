<template>
  <section class="business-detail" :style="rootStyle">
    <header class="detail-header">
      <span></span>
      <div>
        <h3>{{ option.title }}</h3>
        <p v-if="option.subtitle">{{ option.subtitle }}</p>
      </div>
    </header>
    <div v-if="loading" class="detail-state">{{ $ui("加载中...") }}</div>
    <div v-else-if="errorMessage" class="detail-state is-error">{{ errorMessage }}</div>
    <div v-else class="detail-grid" :style="{ gridTemplateColumns: `repeat(${option.columns || 2}, minmax(0, 1fr))` }">
      <div
        v-for="field in option.fields"
        :key="field.key"
        class="detail-item"
        :style="{ gridColumn: `span ${Math.min(field.span || 1, option.columns || 2)}` }"
      >
        <div class="label">{{ field.label }}</div>
        <img v-if="field.type === 'image'" class="value-image" :src="getValue(field.key)" alt="" />
        <div v-else-if="field.type === 'tag'" class="value-tag" :style="{ color: field.color, borderColor: field.color }">
          {{ formatValue(field) }}
        </div>
        <a v-else-if="field.type === 'link'" class="value-link" :href="formatLink(field)" :target="field.openTarget || '_self'">
          {{ formatValue(field) }}
        </a>
        <div v-else-if="field.type === 'progress'" class="value-progress">
          <span><i :style="{ width: `${clampPercent(getValue(field.key))}%` }"></i></span>
          <em>{{ clampPercent(getValue(field.key)) }}{{ field.unit || '%' }}</em>
        </div>
        <div v-else class="value">{{ formatValue(field) }}</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, PropType, ref, unref, watch } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { get, post } from '@/api/http'
import { PREVIEW_PAGE_CONTEXT_KEY } from '@/utils/requestDynamicParams'
import type { DetailField, option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const pageContext = inject(PREVIEW_PAGE_CONTEXT_KEY, ref({}))
const detailData = ref<Record<string, any>>({})
const loading = ref(false)
const errorMessage = ref('')
const option = computed(() => props.chartConfig.option)
const apiOption = computed(() => ({
  detailUrl: '',
  method: 'get',
  dataPath: 'data',
  paramMap: { id: 'id' },
  ...(option.value.api || {})
}))
const data = computed(() => ({ ...(option.value.data || {}), ...(unref(pageContext) || {}), ...(detailData.value || {}) }))
const rootStyle = computed(() => ({
  '--detail-accent': option.value.style.accentColor,
  '--detail-text': option.value.style.textColor,
  '--detail-muted': option.value.style.mutedColor,
  '--detail-panel': option.value.style.panelColor,
  '--detail-border': option.value.style.borderColor,
  '--detail-radius': `${option.value.style.radius}px`
}))

const getByPath = (target: any, path: string) => path.split('.').reduce((current, key) => current?.[key], target)
const getValue = (key: string) => getByPath(data.value, key)

const templateUrl = (template: string, row: Record<string, any>) =>
  template.replace(/\$\{([^}]+)\}/g, (_, key) => String(getByPath(row, key) ?? ''))

const buildParams = () => {
  const context = unref(pageContext) || {}
  const params: Record<string, any> = {}
  Object.entries(apiOption.value.paramMap || {}).forEach(([targetKey, sourceKey]) => {
    const value = getByPath(context, sourceKey)
    if (value !== undefined && value !== null && value !== '') params[targetKey] = value
  })
  return params
}

const normalizeDetail = (payload: any) => {
  const detail = apiOption.value.dataPath ? getByPath(payload, apiOption.value.dataPath) : payload?.data
  return detail && typeof detail === 'object' ? detail : {}
}

const fetchDetail = async () => {
  if (!apiOption.value.detailUrl) {
    detailData.value = {}
    errorMessage.value = ''
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const params = buildParams()
    const method = apiOption.value.method === 'post' ? post : get
    const res = await method(apiOption.value.detailUrl, params)
    detailData.value = normalizeDetail(res)
  } catch (error) {
    console.error(error)
    errorMessage.value = '详情数据加载失败'
  } finally {
    loading.value = false
  }
}

const formatValue = (field: DetailField) => {
  const value = getValue(field.key)
  if (field.type === 'money') return Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  if (field.type === 'date' && value) return String(value).slice(0, 10)
  if (field.type === 'progress') return `${clampPercent(value)}${field.unit || '%'}`
  return value ?? '-'
}

const formatLink = (field: DetailField) => field.urlTemplate ? templateUrl(field.urlTemplate, data.value) : String(getValue(field.key) || '#')
const clampPercent = (value: any) => Math.min(100, Math.max(0, Number(value) || 0))

watch(
  () => [apiOption.value.detailUrl, apiOption.value.method, apiOption.value.dataPath, apiOption.value.paramMap, unref(pageContext)],
  fetchDetail,
  { deep: true }
)

onMounted(fetchDetail)
</script>

<style scoped lang="scss">
.business-detail {
  width: 100%;
  height: 100%;
  padding: 16px;
  overflow: hidden;
  color: var(--detail-text);
  border: 1px solid var(--detail-border);
  border-radius: var(--detail-radius);
  background: var(--detail-panel);
}

.detail-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;

  span {
    width: 4px;
    height: 30px;
    border-radius: 4px;
    background: var(--detail-accent);
    box-shadow: 0 0 16px var(--detail-accent);
  }

  h3 {
    margin: 0;
    font-size: 18px;
    line-height: 1.2;
  }

  p {
    margin: 4px 0 0;
    color: var(--detail-muted);
    font-size: 12px;
  }
}

.detail-grid {
  display: grid;
  gap: 10px;
  max-height: calc(100% - 48px);
  overflow: auto;
}

.detail-state {
  display: grid;
  height: calc(100% - 48px);
  place-items: center;
  color: var(--detail-muted);
}

.detail-state.is-error {
  color: #fb7185;
}

.detail-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.035);
}

.label {
  margin-bottom: 6px;
  color: var(--detail-muted);
  font-size: 12px;
}

.value {
  overflow: hidden;
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.value-tag {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.value-link {
  color: var(--detail-accent);
  font-weight: 700;
  text-decoration: none;
}

.value-progress {
  display: flex;
  align-items: center;
  gap: 8px;

  span {
    position: relative;
    flex: 1;
    height: 7px;
    overflow: hidden;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.14);
  }

  i {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, var(--detail-accent), #34d399);
  }

  em {
    color: var(--detail-muted);
    font-size: 12px;
    font-style: normal;
  }
}

.value-image {
  width: 100%;
  max-height: 96px;
  object-fit: cover;
  border-radius: 6px;
}
</style>
