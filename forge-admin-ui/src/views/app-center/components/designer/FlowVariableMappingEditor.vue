<template>
  <div class="flow-variable-mapping-editor">
    <div class="mapping-guide">
      <div>
        <strong>{{ translateUiText("让流程认识你的业务字段") }}</strong>
        <span>{{ translateUiText("举例：金额(amount) → 流程条件 ${amount > 10000} 判断走总经理审批。") }}</span>
      </div>
      <n-tag size="small" :type="suggestions.length ? 'info' : 'default'" :bordered="false">
        {{ suggestions.length ? `${suggestions.length} 条推荐` : translateUiText("无推荐") }}
      </n-tag>
    </div>

    <div class="mapping-toolbar">
      <n-space size="small">
        <n-button size="small" secondary :disabled="!suggestions.length" @click="applySuggestions">
          {{ translateUiText("应用推荐映射") }}
        </n-button>
        <n-button size="small" dashed @click="addMapping">
          {{ translateUiText("添加映射") }}
        </n-button>
      </n-space>
    </div>

    <div v-if="mappings.length" class="mapping-list">
      <div v-for="(mapping, index) in mappings" :key="mapping.clientKey" class="mapping-row">
        <n-select
          :value="mapping.formField"
          :options="fieldOptions"
          clearable
          filterable
          :placeholder="translateUiText(&quot;业务字段（来自当前业务单据）&quot;)"
          @update:value="value => updateMapping(index, { formField: value, label: fieldLabel(value) })"
        />
        <span>→</span>
        <n-select
          :value="mapping.flowVariable"
          :options="variableOptions"
          :loading="loading"
          clearable
          filterable
          tag
          :placeholder="translateUiText(&quot;流程使用的变量名&quot;)"
          @update:value="value => updateMapping(index, { flowVariable: value || '' })"
        />
        <n-button quaternary circle size="small" @click="removeMapping(index)">
          <template #icon>
            <n-icon><TrashOutline /></n-icon>
          </template>
        </n-button>
      </div>
    </div>
    <n-empty
      v-else
      :description="translateUiText(&quot;还没配置映射。如果流程有条件分支（如金额 > 10000 走总经理），需要把“金额”映射给变量 amount，否则分支不生效。&quot;)"
    />

    <n-alert v-if="warnings.length" type="warning" :bordered="false" class="mapping-warning">
      {{ warnings.join('；') }}
    </n-alert>
  </div>
</template>

<script setup>
import { formatUiText } from '@/locales'

import { localizeLabel as translateUiText } from '@/locales'

import { TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  variableOptions: {
    type: Array,
    default: () => [],
  },
  suggestions: {
    type: Array,
    default: () => [],
  },
  warnings: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])
const message = useMessage()

const mappings = computed(() => normalizeMappings(props.modelValue))

function addMapping() {
  emit('update:modelValue', [
    ...mappings.value,
    createMapping({ formField: null, flowVariable: '', label: '' }),
  ])
}

function updateMapping(index, patch) {
  const next = mappings.value.map((item, itemIndex) => itemIndex === index
    ? { ...item, ...patch }
    : item)
  emit('update:modelValue', next)
}

function removeMapping(index) {
  emit('update:modelValue', mappings.value.filter((_, itemIndex) => itemIndex !== index))
}

function applySuggestions() {
  if (!props.suggestions.length)
    return
  const next = [...mappings.value]
  let applied = 0
  props.suggestions.forEach((suggestion) => {
    if (!suggestion?.formField || !suggestion?.flowVariable)
      return
    const existing = next.find(item => item.formField === suggestion.formField)
    if (existing) {
      if (!existing.flowVariable) {
        existing.flowVariable = suggestion.flowVariable
        existing.label = suggestion.fieldLabel || fieldLabel(suggestion.formField)
        applied += 1
      }
      return
    }
    next.push(createMapping({
      formField: suggestion.formField,
      flowVariable: suggestion.flowVariable,
      label: suggestion.fieldLabel || fieldLabel(suggestion.formField),
    }))
    applied += 1
  })
  if (!applied) {
    message.info(translateUiText("没有可补充的空映射"))
    return
  }
  emit('update:modelValue', next)
  message.success(formatUiText("已应用 {0} 条推荐映射", [applied]))
}

function normalizeMappings(list = []) {
  return (Array.isArray(list) ? list : []).map(item => createMapping({
    ...item,
    formField: item.formField || item.field || null,
    flowVariable: item.flowVariable || item.variable || '',
    label: item.label || fieldLabel(item.formField || item.field),
    clientKey: item.clientKey,
  }))
}

function createMapping(values = {}) {
  return {
    clientKey: values.clientKey || `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    formField: values.formField || null,
    flowVariable: values.flowVariable || '',
    label: values.label || '',
  }
}

function fieldLabel(code) {
  if (!code)
    return ''
  return props.fieldOptions.find(item => item.value === code)?.label || code
}
</script>

<style scoped>
.flow-variable-mapping-editor {
  display: grid;
  gap: 10px;
  width: 100%;
}

.mapping-guide {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 10px 12px;
}

.mapping-guide strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
  line-height: 18px;
}

.mapping-guide span {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.mapping-toolbar {
  display: flex;
  justify-content: flex-end;
}

.mapping-list {
  display: grid;
  gap: 8px;
}

.mapping-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 24px minmax(180px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.mapping-row span {
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

.mapping-warning {
  margin-top: 2px;
}

@media (max-width: 720px) {
  .mapping-row {
    grid-template-columns: 1fr;
  }
}
</style>
