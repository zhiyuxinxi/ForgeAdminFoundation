<template>
  <div class="component-palette">
    <div class="panel-title">
      {{ translateUiText("业务组件库") }}
    </div>
    <div class="zone-tabs">
      <button
        v-for="zone in pageZoneCatalog"
        :key="zone.zoneKey"
        class="zone-tab"
        :class="{ active: selectedZoneKey === zone.zoneKey }"
        type="button"
        @click="$emit('select', zone.zoneKey)"
      >
        {{ zone.title }}
      </button>
    </div>

    <div class="palette-scroll">
      <section class="palette-section">
        <div class="section-title">
          {{ translateUiText("业务组件") }}
        </div>
        <div class="palette-list">
          <button
            v-for="item in businessComponents"
            :key="item.componentKey"
            class="palette-item"
            type="button"
            draggable="true"
            @dragstart="handleComponentDragStart($event, item)"
          >
            <span class="palette-name">{{ translateUiText(item.title, item?.titleI18n) }}</span>
            <span class="palette-desc">{{ item.desc }}</span>
            <span class="palette-tip">{{ translateUiText("拖到右侧 Canvas") }}</span>
          </button>
        </div>
      </section>

      <section v-if="baseControls.length" class="palette-section">
        <div class="section-title">
          {{ translateUiText("基础控件") }}
        </div>
        <div class="compact-list">
          <button
            v-for="item in baseControls"
            :key="item.componentKey"
            class="compact-item"
            type="button"
            draggable="true"
            @dragstart="handleComponentDragStart($event, item)"
          >
            {{ translateUiText(item.title, item?.titleI18n) }}
          </button>
        </div>
      </section>

      <section class="palette-section">
        <div class="section-title field-section-title">
          <span>{{ translateUiText("数据字段") }}</span>
          <span>{{ availableFields.length }} / {{ pageFields.length }}</span>
        </div>
        <div class="field-tools">
          <n-input
            v-model:value="fieldKeyword"
            clearable
            size="small"
            :placeholder="translateUiText(&quot;搜索字段名称或编码&quot;)"
          />
          <n-checkbox v-model:checked="hideUsedFields" size="small">
            {{ translateUiText("隐藏已放置") }}
          </n-checkbox>
        </div>
        <div class="field-list">
          <button
            v-for="field in availableFields"
            :key="field.field"
            class="field-item"
            type="button"
            draggable="true"
            @dragstart="handleFieldDragStart($event, field)"
          >
            <span class="field-name">{{ field.label || field.field }}</span>
            <span class="field-model-tag" :class="{ primary: isPrimaryModelField(field) }">
              {{ resolveModelTag(field) }}
            </span>
            <span class="field-meta">{{ field.field }} · {{ resolveFieldType(field) }}</span>
          </button>
          <n-empty v-if="!availableFields.length" size="small" :description="translateUiText(&quot;没有可添加字段&quot;)" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { computed, ref } from 'vue'
import { canvasComponentCatalog, isPageFieldVisible, pageZoneCatalog, resolveDefaultFieldComponentKey } from './page-schema'

const props = defineProps({
  selectedZoneKey: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  usedFieldRefs: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['select'])

const businessComponents = computed(() => canvasComponentCatalog.filter((item) => {
  return item.group !== 'field' && item.zones?.includes(props.selectedZoneKey)
}))

const baseControls = computed(() => canvasComponentCatalog.filter((item) => {
  return item.group === 'field' && item.zones?.includes(props.selectedZoneKey)
}))

const fieldKeyword = ref('')
const hideUsedFields = ref(true)
const usedFieldSet = computed(() => new Set(props.usedFieldRefs || []))
const pageFields = computed(() => {
  return props.fields.filter(field => isPageFieldVisible(field, props.selectedZoneKey))
})

const availableFields = computed(() => {
  const keyword = fieldKeyword.value.trim().toLowerCase()
  return pageFields.value.filter((field) => {
    if (hideUsedFields.value && usedFieldSet.value.has(field.field))
      return false
    if (!keyword)
      return true
    return [
      field.label,
      field.field,
      field.columnName,
      field.sourceField,
      field.comment,
    ].some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

function handleComponentDragStart(event, item) {
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-lowcode-component', JSON.stringify({
    componentKey: item.componentKey,
    label: item.title,
  }))
}

function handleFieldDragStart(event, field) {
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-lowcode-component', JSON.stringify({
    componentKey: resolveDefaultFieldComponentKey(field, props.selectedZoneKey),
    fieldRef: field.field,
    label: field.label || field.field,
    modelCode: field.modelCode || '',
    modelName: field.modelName || '',
  }))
}

function resolveFieldType(field) {
  if (field.dictType)
    return '字典'
  if (field.componentType)
    return field.componentType
  return field.dataType || '字段'
}

function isPrimaryModelField(field = {}) {
  return !field.modelCode || field.field === (field.sourceField || field.field)
}

function resolveModelTag(field = {}) {
  if (isPrimaryModelField(field))
    return '主模型'
  return field.modelName || field.modelCode || '关联模型'
}
</script>

<style scoped>
.component-palette {
  height: 100%;
  display: grid;
  grid-template-rows: 42px auto minmax(0, 1fr);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.panel-title {
  height: 42px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.zone-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f7;
}

.zone-tab {
  min-height: 30px;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
}

.zone-tab:hover,
.zone-tab.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.palette-scroll {
  overflow: auto;
  padding: 12px;
}

.palette-section + .palette-section {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.field-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-section-title span:last-child {
  color: #94a3b8;
  font-weight: 500;
}

.field-tools {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.palette-list {
  display: grid;
  gap: 8px;
}

.palette-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
}

.palette-item:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.palette-item.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.08);
}

.compact-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.compact-item {
  min-height: 34px;
  padding: 0 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
  color: #475569;
  font-size: 12px;
  cursor: grab;
}

.compact-item:hover {
  border-color: #2563eb;
  color: #1d4ed8;
}

.field-list {
  display: grid;
  gap: 6px;
}

.field-item {
  display: grid;
  gap: 3px;
  min-height: 46px;
  padding: 7px 9px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: grab;
}

.field-item:hover {
  border-color: #2563eb;
  background: #f8fbff;
}

.field-name {
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-model-tag {
  justify-self: start;
  padding: 1px 6px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.5;
}

.field-model-tag.primary {
  background: #eff6ff;
  color: #1d4ed8;
}

.field-meta {
  color: #94a3b8;
  font-size: 11px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.palette-name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.palette-desc {
  font-size: 12px;
  color: #64748b;
}

.palette-tip {
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}
</style>
