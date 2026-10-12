<template>
  <CollapseItem name="业务详情" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="副标题">
        <n-input v-model:value="optionData.subtitle" size="small" />
      </SettingItem>
      <SettingItem name="列数">
        <n-input-number v-model:value="optionData.columns" size="small" :min="1" :max="4" />
      </SettingItem>
    </SettingItemBox>

    <SettingItemBox name="数据">
      <SettingItem name="详情接口">
        <n-input v-model:value="optionData.api.detailUrl" size="small" placeholder="/forge-report-api/xxx/:id" />
      </SettingItem>
      <SettingItem name="请求方式">
        <n-select v-model:value="optionData.api.method" size="small" :options="methodOptions" />
      </SettingItem>
      <SettingItem name="数据路径">
        <n-input v-model:value="optionData.api.dataPath" size="small" placeholder="data" />
      </SettingItem>
      <SettingItem name="参数映射">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="openEditor('params')">{{ $ui("配置") }}</n-button>
          <span class="setting-count">{{ recordCount(optionData.api.paramMap) }} {{ $ui("个") }}</span>
        </div>
      </SettingItem>
      <SettingItem name="接口预览">
        <n-button size="tiny" secondary type="primary" @click="openEditor('apiTest')">{{ $ui("测试接口") }}</n-button>
      </SettingItem>
    </SettingItemBox>

    <SettingItemBox name="字段">
      <SettingItem name="详情字段">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="openEditor('fields')">{{ $ui("配置") }}</n-button>
          <span class="setting-count">{{ optionData.fields?.length || 0 }} {{ $ui("项") }}</span>
        </div>
      </SettingItem>
      <SettingItem name="配置源码">
        <n-button size="tiny" secondary @click="openEditor('json')">{{ $ui("查看 JSON") }}</n-button>
      </SettingItem>
    </SettingItemBox>

    <SettingItemBox name="样式">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.style.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="面板">
        <n-color-picker v-model:value="optionData.style.panelColor" size="small" :modes="['hex', 'rgb']" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>

  <n-modal
    v-model:show="editorVisible"
    preset="card"
    :title="editorTitle"
    :bordered="false"
    :segmented="{ content: true, footer: true }"
    class="detail-config-modal"
  >
    <template v-if="editorMode === 'fields'">
      <div class="modal-toolbar">
        <span>{{ $ui("配置详情字段，字段名支持点路径。") }}</span>
        <n-button size="small" type="primary" @click="addField">{{ $ui("新增字段") }}</n-button>
      </div>
      <div class="config-list">
        <div v-for="(field, index) in optionData.fields" :key="index" class="config-card">
          <div class="config-card__head">
            <strong>{{ $ui("字段") }} {{ index + 1 }}</strong>
            <div class="config-actions">
              <n-button size="tiny" quaternary :disabled="index === 0" @click="moveItem(optionData.fields, index, -1)">{{ $ui("上移") }}</n-button>
              <n-button
                size="tiny"
                quaternary
                :disabled="index === optionData.fields.length - 1"
                @click="moveItem(optionData.fields, index, 1)"
              >
                {{ $ui("下移") }}
              </n-button>
              <n-button size="tiny" quaternary type="error" @click="removeItem(optionData.fields, index)">{{ $ui("删除") }}</n-button>
            </div>
          </div>
          <n-grid :cols="2" :x-gap="12" :y-gap="10" responsive="screen">
            <n-grid-item>
              <div class="field-label">{{ $ui("显示名称") }}</div>
              <n-input v-model:value="field.label" size="small" :placeholder="$ui(&quot;如：项目名称&quot;)" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">{{ $ui("字段名") }}</div>
              <n-input v-model:value="field.key" size="small" :placeholder="$ui(&quot;如：project.name&quot;)" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">{{ $ui("字段类型") }}</div>
              <n-select v-model:value="field.type" size="small" :options="fieldTypeOptions" />
            </n-grid-item>
            <n-grid-item>
              <div class="field-label">{{ $ui("占列") }}</div>
              <n-input-number v-model:value="field.span" size="small" :min="1" :max="optionData.columns || 4" />
            </n-grid-item>
            <n-grid-item v-if="field.type === 'tag'">
              <div class="field-label">{{ $ui("标签颜色") }}</div>
              <n-color-picker v-model:value="field.color" size="small" :modes="['hex']" />
            </n-grid-item>
            <n-grid-item v-if="field.type === 'money' || field.type === 'progress'">
              <div class="field-label">{{ $ui("单位") }}</div>
              <n-input v-model:value="field.unit" size="small" :placeholder="$ui(&quot;如：元 / %&quot;)" />
            </n-grid-item>
            <n-grid-item v-if="field.type === 'link'">
              <div class="field-label">{{ $ui("链接模板") }}</div>
              <n-input v-model:value="field.urlTemplate" size="small" placeholder="/detail?id=${id}" />
            </n-grid-item>
            <n-grid-item v-if="field.type === 'link'">
              <div class="field-label">{{ $ui("打开方式") }}</div>
              <n-select v-model:value="field.openTarget" size="small" :options="openTargetOptions" />
            </n-grid-item>
          </n-grid>
        </div>
      </div>
    </template>

    <template v-else-if="editorMode === 'params'">
      <div class="modal-toolbar">
        <span>{{ $ui("把弹窗/下钻上下文参数映射到详情接口。") }}</span>
        <n-button size="small" type="primary" @click="addRecordEntry(optionData.api, 'paramMap')">{{ $ui("新增参数") }}</n-button>
      </div>
      <div class="config-card">
        <div class="option-row option-row--header">
          <span>{{ $ui("接口参数名") }}</span>
          <span>{{ $ui("上下文字段名") }}</span>
          <span></span>
        </div>
        <div v-for="(entry, entryIndex) in recordEntries(optionData.api.paramMap)" :key="entryIndex" class="option-row">
          <n-input
            :value="entry[0]"
            size="small"
            :placeholder="$ui(&quot;接口参数，如 id&quot;)"
            @update:value="setRecordEntry(optionData.api, 'paramMap', entryIndex, 'key', $event)"
          />
          <n-input
            :value="entry[1]"
            size="small"
            :placeholder="$ui(&quot;上下文字段，如 id&quot;)"
            @update:value="setRecordEntry(optionData.api, 'paramMap', entryIndex, 'value', $event)"
          />
          <n-button size="tiny" quaternary type="error" @click="removeRecordEntry(optionData.api, 'paramMap', entryIndex)">{{ $ui("删除") }}</n-button>
        </div>
      </div>
    </template>

    <template v-else-if="editorMode === 'apiTest'">
      <div class="modal-toolbar">
        <span>{{ $ui("按当前接口配置拉取详情数据。") }}</span>
        <div class="toolbar-actions">
          <n-button size="small" secondary :disabled="!Object.keys(apiTestData).length" @click="generateFieldsFromApi">{{ $ui("生成字段") }}</n-button>
          <n-button size="small" type="primary" :loading="apiTesting" @click="testApi">{{ $ui("重新测试") }}</n-button>
        </div>
      </div>
      <div v-if="apiTestError" class="api-test-error">{{ apiTestError }}</div>
      <n-input v-else :value="apiTestPreview" type="textarea" readonly :autosize="{ minRows: 14, maxRows: 24 }" />
    </template>

    <template v-else>
      <div class="modal-toolbar">
        <span>{{ $ui("当前详情配置的只读 JSON。") }}</span>
        <n-button size="small" secondary @click="copyJson">{{ $ui("复制 JSON") }}</n-button>
      </div>
      <n-input :value="jsonPreview" type="textarea" readonly :autosize="{ minRows: 18, maxRows: 28 }" />
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, PropType, ref } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { get, post } from '@/api/http'
import { option } from './config'

type EditorMode = 'fields' | 'params' | 'apiTest' | 'json'
type RecordSide = 'key' | 'value'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const ensureOptionData = () => {
  const data = props.optionData as any
  data.api = {
    detailUrl: '',
    method: 'get',
    dataPath: 'data',
    paramMap: { id: 'id' },
    ...(data.api || {})
  }
  data.fields = Array.isArray(data.fields) ? data.fields : []
  data.data = data.data && typeof data.data === 'object' ? data.data : {}
  data.style = {
    ...option.style,
    ...(data.style || {})
  }
}

ensureOptionData()

const editorVisible = ref(false)
const editorMode = ref<EditorMode>('fields')
const apiTesting = ref(false)
const apiTestData = ref<any>({})
const apiTestError = ref('')

const methodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' }
]

const fieldTypeOptions = [
  { get label() { return uiText("文本") }, value: 'text' },
  { get label() { return uiText("标签") }, value: 'tag' },
  { get label() { return uiText("金额") }, value: 'money' },
  { get label() { return uiText("日期") }, value: 'date' },
  { get label() { return uiText("图片") }, value: 'image' },
  { get label() { return uiText("链接") }, value: 'link' },
  { get label() { return uiText("进度") }, value: 'progress' }
]

const openTargetOptions = [
  { get label() { return uiText("当前页") }, value: '_self' },
  { get label() { return uiText("新窗口") }, value: '_blank' }
]

const editorTitle = computed(() => {
  const titleMap: Record<EditorMode, string> = {
    fields: '配置详情字段',
    params: '配置接口参数',
    apiTest: '接口测试预览',
    json: '查看业务详情 JSON'
  }
  return titleMap[editorMode.value]
})

const jsonPreview = computed(() =>
  JSON.stringify(
    {
      api: props.optionData.api,
      fields: props.optionData.fields,
      data: props.optionData.data
    },
    null,
    2
  )
)

const apiTestPreview = computed(() => JSON.stringify(apiTestData.value, null, 2))

const guessFieldType = (value: any) => {
  if (typeof value === 'number') return value >= 0 && value <= 100 ? 'progress' : 'money'
  if (typeof value === 'string' && /^https?:\/\/.+\.(png|jpg|jpeg|webp|gif)$/i.test(value)) return 'image'
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) return 'date'
  return 'text'
}

const generateFieldsFromApi = () => {
  props.optionData.fields = Object.keys(apiTestData.value || {}).slice(0, 12).map(key => ({
    label: key,
    key,
    type: guessFieldType(apiTestData.value[key]) as any,
    span: 1
  }))
  window['$message']?.success('已按接口响应生成详情字段')
}

const openEditor = (mode: EditorMode) => {
  editorMode.value = mode
  editorVisible.value = true
  if (mode === 'apiTest') testApi()
}

const getByPath = (target: any, path?: string) => {
  if (!path) return target
  return path.split('.').reduce((current, key) => current?.[key], target)
}

const normalizeDetail = (payload: any) => {
  const data = props.optionData.api.dataPath ? getByPath(payload, props.optionData.api.dataPath) : payload?.data
  return data && typeof data === 'object' ? data : {}
}

const testApi = async () => {
  apiTestError.value = ''
  apiTestData.value = {}
  if (!props.optionData.api.detailUrl) {
    apiTestError.value = '请先配置详情接口'
    return
  }
  apiTesting.value = true
  try {
    const method = props.optionData.api.method === 'post' ? post : get
    const res = await method(props.optionData.api.detailUrl, {})
    apiTestData.value = normalizeDetail(res)
  } catch (error: any) {
    apiTestError.value = error?.message || '接口测试失败'
  } finally {
    apiTesting.value = false
  }
}

const addField = () => {
  if (!props.optionData.fields) props.optionData.fields = []
  props.optionData.fields.push({
    label: '新字段',
    key: `field${props.optionData.fields.length + 1}`,
    type: 'text',
    span: 1
  })
}

const removeItem = (list: any[] | undefined, index: number) => {
  list?.splice(index, 1)
}

const moveItem = (list: any[], index: number, direction: -1 | 1) => {
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(nextIndex, 0, item)
}

const recordEntries = (record?: Record<string, string>) => Object.entries(record || {})
const recordCount = (record?: Record<string, string>) => recordEntries(record).length

const addRecordEntry = (target: any, key: 'paramMap') => {
  const record = { ...(target[key] || {}) }
  let index = Object.keys(record).length + 1
  let nextKey = `param${index}`
  while (record[nextKey] !== undefined) {
    index += 1
    nextKey = `param${index}`
  }
  record[nextKey] = ''
  target[key] = record
}

const setRecordEntry = (target: any, key: 'paramMap', index: number, side: RecordSide, value: string) => {
  const entries = recordEntries(target[key])
  const current = entries[index] || ['', '']
  entries[index] = side === 'key' ? [value, current[1]] : [current[0], value]
  target[key] = Object.fromEntries(entries.filter(([entryKey]) => entryKey))
}

const removeRecordEntry = (target: any, key: 'paramMap', index: number) => {
  const entries = recordEntries(target[key])
  entries.splice(index, 1)
  target[key] = Object.fromEntries(entries)
}

const copyJson = async () => {
  await navigator.clipboard?.writeText(jsonPreview.value)
  window['$message']?.success('已复制 JSON')
}
</script>

<style scoped lang="scss">
.setting-line,
.modal-toolbar,
.config-card__head,
.config-actions {
  display: flex;
  align-items: center;
}

.setting-line {
  gap: 8px;
}

.setting-count {
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
}

:deep(.detail-config-modal) {
  width: min(900px, 92vw);
}

.modal-toolbar {
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  margin-bottom: 14px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: min(64vh, 680px);
  padding-right: 4px;
  overflow: auto;
}

.config-card {
  padding: 14px;
  border: 1px solid rgba(120, 172, 255, 0.18);
  border-radius: 8px;
  background: rgba(13, 20, 38, 0.72);
}

.config-card__head {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  strong {
    color: rgba(255, 255, 255, 0.88);
    font-size: 13px;
  }
}

.config-actions {
  gap: 4px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.field-label {
  margin-bottom: 5px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}

.option-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 58px;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.option-row--header {
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.52);
  font-size: 12px;
}

.api-test-error {
  padding: 16px;
  color: #ff8a8a;
  border: 1px solid rgba(255, 120, 120, 0.24);
  border-radius: 8px;
  background: rgba(80, 18, 26, 0.32);
}
</style>
