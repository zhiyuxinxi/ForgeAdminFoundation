<template>
  <n-modal
    :show="show"
    preset="card"
    :title="localizeLabel(title)"
    class="ai-record-selector-modal"
    :style="{ width: 'min(920px, calc(100vw - 32px))' }"
    :mask-closable="false"
    @update:show="emit('update:show', $event)"
  >
    <div class="record-selector-toolbar">
      <n-input
        v-model:value="keyword"
        clearable
        :placeholder="localizeLabel(placeholder)"
        @keyup.enter="reload"
      />
      <n-button type="primary" :loading="loading" @click="reload">
        {{ localizeLabel('查询') }}
      </n-button>
    </div>

    <n-data-table
      :columns="translateUiMetadata(tableColumns)"
      :data="records"
      :loading="loading"
      :row-key="row => row.id"
      :checked-row-keys="checkedRowKeys"
      :pagination="pagination"
      :single-line="false"
      remote
      @update:checked-row-keys="handleCheckedKeys"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <template #footer>
      <n-space justify="space-between" align="center">
        <span class="record-selector-count">{{ localizeLabel('已选') }} {{ checkedRows.length }} {{ localizeLabel('条') }}</span>
        <n-space>
          <n-button @click="emit('update:show', false)">
            {{ localizeLabel('取消') }}
          </n-button>
          <n-button type="primary" :disabled="!checkedRows.length" @click="confirmSelection">
            {{ localizeLabel('确定') }}
          </n-button>
        </n-space>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { computed, ref, watch } from 'vue'

import { queryBusinessRecordSelector } from '@/api/business-app'
import { localizeLabel } from '@/locales'
import { normalizeSelectorMappings, resolveSelectorSearchParams } from './record-selector-utils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '选择记录',
  },
  objectCode: {
    type: String,
    default: '',
  },
  businessObjectCode: {
    type: String,
    default: '',
  },
  targetObjectCode: {
    type: String,
    default: '',
  },
  targetEntityCode: {
    type: String,
    default: '',
  },
  candidateObjectCode: {
    type: String,
    default: '',
  },
  referenceObjectCode: {
    type: String,
    default: '',
  },
  refObjectCode: {
    type: String,
    default: '',
  },
  sourceObjectCode: {
    type: String,
    default: '',
  },
  targetCode: {
    type: String,
    default: '',
  },
  suiteCode: {
    type: String,
    default: '',
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  displayFields: {
    type: Array,
    default: () => [],
  },
  keywordFields: {
    type: Array,
    default: () => [],
  },
  fieldMappings: {
    type: [Array, Object],
    default: () => [],
  },
  searchParams: {
    type: Object,
    default: () => ({}),
  },
  runtimeContext: {
    type: Object,
    default: () => ({}),
  },
  pageSize: {
    type: Number,
    default: 10,
  },
})

const emit = defineEmits(['update:show', 'confirm'])

const loading = ref(false)
const keyword = ref('')
const records = ref([])
const columns = ref([])
const checkedRowKeys = ref([])
const checkedRows = ref([])
const pagination = ref({
  page: 1,
  pageSize: props.pageSize,
  itemCount: 0,
  pageSizes: [10, 20, 50],
  showSizePicker: true,
})

const placeholder = computed(() => props.keywordFields?.length ? localizeLabel('输入关键词查询') : localizeLabel('输入关键词'))
const resolvedSearchParams = computed(() => resolveSelectorSearchParams(props.searchParams, props.runtimeContext))
const effectiveObjectCode = computed(() => firstText(
  props.objectCode,
  props.businessObjectCode,
  props.targetObjectCode,
  props.targetEntityCode,
  props.candidateObjectCode,
  props.referenceObjectCode,
  props.refObjectCode,
  props.sourceObjectCode,
  props.targetCode,
))
const tableColumns = computed(() => [
  {
    type: 'selection',
    multiple: props.multiple,
    width: 48,
  },
  ...columns.value.map(column => ({
    title: column.label || column.field,
    key: column.field,
    width: column.width || 140,
    ellipsis: { tooltip: true },
  })),
])

watch(() => props.show, (visible) => {
  if (visible)
    reload()
  else resetSelection()
})

async function reload() {
  if (!effectiveObjectCode.value) {
    records.value = []
    columns.value = []
    pagination.value.itemCount = 0
    return
  }
  loading.value = true
  try {
    const res = await queryBusinessRecordSelector({
      suiteCode: props.suiteCode,
      objectCode: effectiveObjectCode.value,
      businessObjectCode: props.businessObjectCode || effectiveObjectCode.value,
      targetObjectCode: props.targetObjectCode || effectiveObjectCode.value,
      targetEntityCode: props.targetEntityCode || effectiveObjectCode.value,
      candidateObjectCode: props.candidateObjectCode,
      referenceObjectCode: props.referenceObjectCode,
      refObjectCode: props.refObjectCode,
      sourceObjectCode: props.sourceObjectCode,
      targetCode: props.targetCode,
      keyword: keyword.value,
      keywordFields: props.keywordFields,
      searchParams: resolvedSearchParams.value,
      displayFields: props.displayFields,
      fieldMappings: toMappingArray(props.fieldMappings),
    }, {
      pageNum: pagination.value.page,
      pageSize: pagination.value.pageSize,
    })
    const data = res.data || {}
    records.value = Array.isArray(data.records) ? data.records : []
    columns.value = Array.isArray(data.columns) ? data.columns : []
    pagination.value.itemCount = Number(data.total || 0)
  }
  finally {
    loading.value = false
  }
}

function handleCheckedKeys(keys = [], rows = []) {
  checkedRowKeys.value = props.multiple ? keys : keys.slice(-1)
  checkedRows.value = props.multiple ? rows : rows.slice(-1)
}

function handlePageChange(page) {
  pagination.value.page = page
  reload()
}

function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  reload()
}

function confirmSelection() {
  emit('confirm', {
    rows: checkedRows.value,
    mappings: normalizeSelectorMappings(props.fieldMappings),
  })
  emit('update:show', false)
}

function resetSelection() {
  checkedRowKeys.value = []
  checkedRows.value = []
  keyword.value = ''
}

function toMappingArray(mappings = []) {
  if (Array.isArray(mappings))
    return mappings
  return Object.entries(mappings || {}).map(([sourceField, targetField]) => ({ sourceField, targetField }))
}

function firstText(...values) {
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (text)
      return text
  }
  return ''
}
</script>

<style scoped>
.record-selector-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-bottom: 12px;
}

.record-selector-count {
  color: #64748b;
  font-size: 13px;
}
</style>
