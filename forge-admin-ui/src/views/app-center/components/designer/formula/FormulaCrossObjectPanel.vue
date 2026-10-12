<template>
  <section class="cross-object-panel" :class="{ active: formModel.formulaCrossObjectEnabled }">
    <div class="cross-object-head">
      <div>
        <strong>{{ translateUiText("跨对象引用") }}</strong>
        <span>{{ pathPreview || translateUiText("选择关系和返回字段后生成一跳路径") }}</span>
      </div>
      <n-switch
        :value="formModel.formulaCrossObjectEnabled"
        :disabled="disabled"
        size="small"
        @update:value="updateEnabled"
      />
    </div>

    <div v-if="formModel.formulaCrossObjectEnabled" class="cross-object-grid">
      <div v-if="pathPreview" class="cross-object-path">
        <span>{{ translateUiText("生成表达式") }}</span>
        <code>{{ pathPreview }}</code>
      </div>

      <n-grid :cols="2" :x-gap="10">
        <n-form-item-gi :label="translateUiText(&quot;对象关系&quot;)">
          <n-select
            v-model:value="formModel.formulaCrossObjectRelationCode"
            :options="relationOptions"
            :disabled="disabled"
            filterable
            clearable
            :placeholder="translateUiText(&quot;选择一跳关系&quot;)"
            @update:value="handleRelationChange"
          />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;目标对象&quot;)">
          <n-input
            v-model:value="formModel.formulaCrossObjectTargetObjectCode"
            disabled
            :placeholder="translateUiText(&quot;选择关系后自动带出&quot;)"
          />
        </n-form-item-gi>
      </n-grid>

      <n-grid :cols="2" :x-gap="10">
        <n-form-item-gi :label="translateUiText(&quot;返回字段&quot;)">
          <n-select
            v-model:value="formModel.formulaCrossObjectReturnField"
            :options="targetFieldOptions"
            :disabled="disabled"
            filterable
            clearable
            tag
            :placeholder="translateUiText(&quot;例如 level / realName&quot;)"
            @update:value="syncPath"
          />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;重算策略&quot;)">
          <n-select
            v-model:value="formModel.formulaCrossObjectRecomputeMode"
            :options="translateUiMetadata(recomputeModeOptions)"
            :disabled="disabled"
          />
        </n-form-item-gi>
      </n-grid>
    </div>
  </section>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

import { computed, watch } from 'vue'

const props = defineProps({
  form: {
    type: Object,
    required: true,
  },
  relations: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const formModel = computed(() => props.form)
const recomputeModeOptions = [
  { label: '异步队列', value: 'ASYNC' },
  { label: '同步重算', value: 'SYNC' },
  { label: '手动重算', value: 'MANUAL' },
]

const activeRelations = computed(() => {
  return (props.relations || [])
    .filter(item => item && item.status !== 0 && relationCode(item))
})

const relationOptions = computed(() => activeRelations.value.map(relation => ({
  label: `${relation.relationName || relation.targetObjectName || relation.targetObjectCode}（${relation.targetObjectCode || '目标对象'}）`,
  value: relationCode(relation),
})))

const selectedRelation = computed(() => {
  return activeRelations.value.find(item => relationCode(item) === formModel.value.formulaCrossObjectRelationCode) || null
})

const targetFieldOptions = computed(() => {
  const fields = new Set()
  const relation = selectedRelation.value
  append(fields, relation?.targetFieldCode)
  append(fields, relationConfigValue(relation, 'displayField'))
  append(fields, relationConfigValue(relation, 'returnField'))
  append(fields, formModel.value.formulaCrossObjectReturnField)
  return [...fields].map(value => ({ label: value, value }))
})

const pathPreview = computed(() => formModel.value.formulaCrossObjectPath || '')

watch(selectedRelation, (relation) => {
  if (!relation || !formModel.value.formulaCrossObjectEnabled)
    return
  applyRelationDefaults(relation)
}, { immediate: true })

watch(() => formModel.value.formulaCrossObjectReturnField, () => {
  syncPath()
})

function updateEnabled(value) {
  formModel.value.formulaCrossObjectEnabled = value
  if (!value) {
    formModel.value.formulaCrossObjectPath = ''
    formModel.value.formulaCrossObjectRelationCode = ''
    formModel.value.formulaCrossObjectTargetObjectCode = ''
    formModel.value.formulaCrossObjectReturnField = ''
    return
  }
  formModel.value.formulaCrossObjectRecomputeMode = formModel.value.formulaCrossObjectRecomputeMode || 'ASYNC'
  if (selectedRelation.value)
    applyRelationDefaults(selectedRelation.value)
}

function handleRelationChange() {
  const relation = selectedRelation.value
  if (relation)
    applyRelationDefaults(relation, { force: true })
  syncPath()
}

function applyRelationDefaults(relation, { force = false } = {}) {
  formModel.value.formulaCrossObjectTargetObjectCode = relation.targetObjectCode || ''
  if (force || !formModel.value.formulaCrossObjectReturnField) {
    formModel.value.formulaCrossObjectReturnField = relationConfigValue(relation, 'displayField')
      || relation.targetFieldCode
      || ''
  }
  syncPath()
}

function syncPath() {
  if (!formModel.value.formulaCrossObjectEnabled)
    return
  const relation = selectedRelation.value
  const relationCodeValue = formModel.value.formulaCrossObjectRelationCode || relationCode(relation)
  const returnField = formModel.value.formulaCrossObjectReturnField
  if (!relationCodeValue || !returnField) {
    formModel.value.formulaCrossObjectPath = ''
    return
  }
  const path = `${relationCodeValue}.${returnField}`
  formModel.value.formulaCrossObjectPath = path
  formModel.value.formulaExpression = path
}

function relationCode(relation = {}) {
  return relation?.relationName || relation?.targetObjectCode || (relation?.id ? String(relation.id) : '')
}

function relationConfigValue(relation, key) {
  if (!relation?.relationConfig)
    return ''
  try {
    const config = typeof relation.relationConfig === 'string'
      ? JSON.parse(relation.relationConfig)
      : relation.relationConfig
    return config?.[key] || ''
  }
  catch {
    return ''
  }
}

function append(target, value) {
  const text = String(value || '').trim()
  if (text)
    target.add(text)
}
</script>

<style scoped>
.cross-object-panel {
  position: relative;
  z-index: 1;
  isolation: isolate;
  display: grid;
  gap: 8px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  padding: 9px 10px;
}

.cross-object-panel.active {
  border-color: #9cc4ff;
  background: #f7fbff;
}

.cross-object-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.cross-object-head strong,
.cross-object-head span {
  display: block;
}

.cross-object-head strong {
  color: #111827;
  font-size: 13px;
}

.cross-object-head span {
  margin-top: 3px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.cross-object-grid {
  display: grid;
  gap: 6px;
}

.cross-object-path {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #eff6ff;
  padding: 6px 8px;
}

.cross-object-path span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.cross-object-path code {
  overflow: hidden;
  color: #1e3a8a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cross-object-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.cross-object-grid :deep(.n-form-item-label) {
  min-height: auto;
  padding-bottom: 5px;
}

.cross-object-grid :deep(.n-form-item-label__text) {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.cross-object-grid :deep(.n-input),
.cross-object-grid :deep(.n-base-selection) {
  --n-border-radius: 6px;
  --n-height: 32px;
  background: #fff;
}
</style>
