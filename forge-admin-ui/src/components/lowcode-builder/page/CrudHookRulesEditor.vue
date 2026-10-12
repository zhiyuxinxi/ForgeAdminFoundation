<template>
  <div class="crud-hook-rules-editor">
    <div class="hook-rule-head">
      <div>
        <strong>{{ translateUiText("回调参数处理") }}</strong>
        <span>{{ translateUiText("用于常见参数改写；复杂逻辑仍交给代码扩展。") }}</span>
      </div>
      <NSelect
        :value="activeHook"
        :options="hookOptions"
        size="small"
        class="hook-select"
        @update:value="activeHook = $event || 'beforeSubmit'"
      />
    </div>

    <div class="hook-rule-desc">
      {{ activeHookMeta?.description || translateUiText("选择回调阶段后配置参数处理规则。") }}
    </div>

    <div v-if="activeRules.length" class="hook-rule-list">
      <div v-for="(rule, idx) in activeRules" :key="rule.id || idx" class="hook-rule-row">
        <NSelect
          :value="rule.action || 'set'"
          :options="actionOptions"
          size="tiny"
          @update:value="updateRule(idx, { action: $event || 'set' })"
        />
        <NSelect
          :value="rule.field || ''"
          :options="fieldOptions"
          size="tiny"
          filterable
          tag
          :placeholder="translateUiText(&quot;目标字段&quot;)"
          @update:value="updateRule(idx, { field: $event || '' })"
        />
        <NSelect
          v-if="rule.action === 'copyFrom'"
          :value="rule.sourceField || ''"
          :options="fieldOptions"
          size="tiny"
          filterable
          tag
          :placeholder="translateUiText(&quot;来源字段&quot;)"
          @update:value="updateRule(idx, { sourceField: $event || '' })"
        />
        <NInput
          v-else-if="rule.action !== 'clear'"
          :value="rule.value || ''"
          size="tiny"
          :placeholder="translateUiText(&quot;值&quot;)"
          @update:value="updateRule(idx, { value: $event })"
        />
        <span v-else class="hook-rule-note">{{ translateUiText("清空") }}</span>
        <NButton size="tiny" quaternary type="error" attr-type="button" @click.stop.prevent="removeRule(idx)">
          {{ translateUiText("删") }}
        </NButton>
      </div>
    </div>
    <div v-else class="hook-rule-empty">
      {{ translateUiText("当前回调没有规则。") }}
    </div>

    <NButton size="small" type="primary" secondary block attr-type="button" @click.stop.prevent="addRule">
      {{ translateUiText("新增") }}{{ activeHookMeta?.label || translateUiText("回调") }}{{ translateUiText("规则") }}
    </NButton>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { NButton, NInput, NSelect } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import {
  createCrudHookRule,
  CRUD_HOOK_RULE_ACTIONS,
  CRUD_HOOK_RULE_TARGETS,
  normalizeCrudHookRules,
} from './crud-hook-rules'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  legacyBeforeSubmitRules: {
    type: Array,
    default: () => [],
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const activeHook = ref('beforeSubmit')
const localRules = ref({})
const hookOptions = CRUD_HOOK_RULE_TARGETS.map(item => ({
  label: item.label,
  value: item.value,
}))
const actionOptions = CRUD_HOOK_RULE_ACTIONS
const activeHookMeta = computed(() => CRUD_HOOK_RULE_TARGETS.find(item => item.value === activeHook.value))
const activeRules = computed(() => localRules.value?.[activeHook.value] || [])

watch(
  () => [props.modelValue, props.legacyBeforeSubmitRules],
  () => {
    localRules.value = normalizeCrudHookRules(props.modelValue || {}, props.legacyBeforeSubmitRules || [], { keepEmpty: true })
    if (!CRUD_HOOK_RULE_TARGETS.some(item => item.value === activeHook.value))
      activeHook.value = 'beforeSubmit'
  },
  { immediate: true, deep: true },
)

function emitRules(nextRules) {
  const normalized = normalizeCrudHookRules(nextRules || {}, [], { keepEmpty: true })
  localRules.value = normalized
  emit('update:modelValue', normalized)
}

function addRule() {
  emitRules({
    ...localRules.value,
    [activeHook.value]: [
      ...activeRules.value,
      createCrudHookRule(activeHook.value),
    ],
  })
}

function updateRule(idx, patch) {
  const list = [...activeRules.value]
  list[idx] = { ...(list[idx] || {}), hookName: activeHook.value, ...patch }
  emitRules({ ...localRules.value, [activeHook.value]: list })
}

function removeRule(idx) {
  const list = [...activeRules.value]
  list.splice(idx, 1)
  emitRules({ ...localRules.value, [activeHook.value]: list })
}
</script>

<style scoped>
.crud-hook-rules-editor {
  display: grid;
  gap: 9px;
  width: 100%;
  min-width: 0;
}

.hook-rule-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.hook-rule-head strong {
  display: block;
  color: #27272a;
  font-size: 12px;
  font-weight: 650;
  line-height: 18px;
}

.hook-rule-head span,
.hook-rule-desc,
.hook-rule-empty,
.hook-rule-note {
  color: #71717a;
  font-size: 11px;
  line-height: 16px;
}

.hook-select {
  min-width: 0;
}

.hook-rule-desc,
.hook-rule-empty {
  padding: 10px 9px;
  border: 1px dashed #d4d4d8;
  border-radius: 7px;
  background: #fafafa;
}

.hook-rule-list {
  display: grid;
  gap: 7px;
}

.hook-rule-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
  align-items: center;
  min-width: 0;
  padding: 8px;
  border: 1px solid #e4e4e7;
  border-radius: 6px;
  background: #fafafa;
}

.hook-rule-row :deep(.n-select),
.hook-rule-row :deep(.n-input) {
  width: 100%;
  min-width: 0;
}

.crud-hook-rules-editor :deep(.n-button) {
  --n-border-radius: 6px !important;
  font-size: 12px;
}

.crud-hook-rules-editor :deep(.n-input),
.crud-hook-rules-editor :deep(.n-base-selection) {
  --n-color: #fff !important;
  --n-color-hover: #fff !important;
  --n-color-focus: #fff !important;
  --n-border: 1px solid #e4e4e7 !important;
  --n-border-hover: 1px solid #a5b4fc !important;
  --n-border-focus: 1px solid #6366f1 !important;
  --n-box-shadow-focus: 0 0 0 2px rgba(99, 102, 241, 0.1) !important;
  border-radius: 6px;
}

.crud-hook-rules-editor :deep(.n-input__input-el),
.crud-hook-rules-editor :deep(.n-base-selection-label) {
  font-size: 12px !important;
}
</style>
