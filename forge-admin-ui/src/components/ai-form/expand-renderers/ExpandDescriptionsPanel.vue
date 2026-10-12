<template>
  <n-descriptions
    class="expand-descriptions-panel"
    size="small"
    bordered
    :column="descriptionConfig.columns || 3"
    :label-placement="descriptionConfig.labelPlacement || 'left'"
  >
    <n-descriptions-item
      v-for="field in fields"
      :key="field.field || field.key || field.label"
      :label="field.label || field.title || field.field"
      :span="field.span || 1"
    >
      <RenderValue :render="() => renderField(field)" />
    </n-descriptions-item>
  </n-descriptions>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h } from 'vue'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import FileUpload from '@/components/file-upload/index.vue'
import { splitFileValues } from '../file-render-utils'

const props = defineProps({
  panel: { type: Object, required: true },
  data: { type: Object, default: () => ({}) },
  row: { type: Object, default: () => ({}) },
})

const RenderValue = props => props.render?.() ?? null

const descriptionConfig = computed(() => props.panel.descriptions || {})
const fields = computed(() => descriptionConfig.value.fields || [])
const record = computed(() => props.data && !Array.isArray(props.data) ? props.data : props.row)

function renderField(field) {
  const key = field.field || field.key
  const value = record.value?.[key]
  if (field.render?.type === 'dictTag') {
    return h(DictTag, {
      dictType: field.render.dictType,
      value,
      size: 'small',
    })
  }
  if (field.render?.type === 'tag') {
    return h(NTag, { size: 'small', type: field.render.tagType || 'default' }, { default: () => value ?? '-' })
  }
  if (field.render?.type === 'imageUpload') {
    if (!value)
      return '-'
    const fileIds = String(value).split(',').filter(Boolean)
    return h('div', { class: 'expand-image-list' }, fileIds.map(fileId => h(AuthImage, {
      fileId,
      style: 'width: 36px; height: 36px; border-radius: 4px; object-fit: cover;',
    })))
  }
  if (field.render?.type === 'fileUpload') {
    const files = Array.isArray(value) ? value.filter(Boolean) : splitFileValues(value)
    if (!files.length)
      return field.emptyText ?? '-'
    return h(FileUpload, { modelValue: files, valueType: 'array', disabled: true, showTip: false })
  }
  return value ?? field.emptyText ?? '-'
}
</script>

<style scoped>
.expand-descriptions-panel {
  width: 100%;
}

.expand-image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
