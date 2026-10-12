<template>
  <ElConfigProvider :locale="elementLocale">
    <div class="flow-form-create-renderer">
      <component
        :is="FormCreateComponent"
        v-if="rules.length"
        v-model:api="formApi"
        v-model="innerValue"
        :rule="rules"
        :option="mergedOptions"
        :disabled="readOnly"
        @change="handleChange"
      />
      <n-empty v-else size="small" :description="localizeLabel('暂无动态表单配置')" />
    </div>
  </ElConfigProvider>
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import elementEn from 'element-plus/es/locale/lang/en'
import elementZh from 'element-plus/es/locale/lang/zh-cn'
import { computed, getCurrentInstance, ref, watch } from 'vue'
import { getLocale, localizeLabel, t } from '@/locales'

import { localizeMetadata } from '@/locales/metadata'
import { normalizeFieldPermissions } from '@/utils/field-permissions'
import { hydrateForgeBusinessPreviewRules } from '@/views/app-center/components/designer/form-first/forgeBusinessComponents'
import {
  buildDefaultFormOptions,
  cloneValue,
  getFormCreateComponent,
  installFormCreate,
  normalizeFormCreateOptions,
  normalizeFormCreateRules,
} from './formCreateBridge'

const props = defineProps({
  schema: {
    type: [Array, String],
    default: () => [],
  },
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  options: {
    type: [Object, String],
    default: () => ({}),
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
  fieldPermissions: {
    type: [Array, String, Object],
    default: () => [],
  },
  gridCols: {
    type: Number,
    default: 2,
  },
  labelPlacement: {
    type: String,
    default: 'top',
  },
  labelWidth: {
    type: [String, Number],
    default: '110px',
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

installFormCreate(getCurrentInstance()?.appContext?.app)

const FormCreateComponent = getFormCreateComponent()
const formApi = ref(null)
const innerValue = ref(cloneValue(props.modelValue) || {})
const hydratedRules = ref([])
let hydrateSeq = 0

const fieldPermissionMap = computed(() => {
  const map = new Map()
  for (const item of normalizeFieldPermissions(props.fieldPermissions, { readOnly: props.readOnly })) {
    if (item.field)
      map.set(item.field, item)
  }
  return map
})
const rawRules = computed(() => applyFieldPermissions(
  applyLayoutToRules(normalizeFormCreateRules(props.schema)),
  fieldPermissionMap.value,
))
const elementLocale = computed(() => getLocale() === 'en-US' ? elementEn : elementZh)
const rules = computed(() => localizeMetadata(hydratedRules.value).map(localizeRequiredMessages))
const mergedOptions = computed(() => {
  const baseOptions = normalizeFormCreateOptions(props.options)
  return {
    ...buildDefaultFormOptions(),
    ...baseOptions,
    form: {
      ...(baseOptions.form || {}),
      labelPosition: props.labelPlacement === 'top' ? 'top' : 'right',
      labelWidth: props.labelPlacement === 'top' ? undefined : normalizeLabelWidth(props.labelWidth),
    },
  }
})

watch(
  () => props.modelValue,
  (value) => {
    innerValue.value = cloneValue(value) || {}
  },
  { deep: true },
)

watch(
  rawRules,
  () => hydrateRules(),
  { immediate: true, deep: true },
)

function handleChange() {
  emit('update:modelValue', cloneValue(innerValue.value) || {})
  emit('change', cloneValue(innerValue.value) || {})
}

function applyFieldPermissions(rules, permissionMap) {
  if (!permissionMap.size)
    return rules
  return applyFieldPermissionsToList(rules, permissionMap)
}

function applyFieldPermissionsToList(rules, permissionMap) {
  return (rules || [])
    .map(rule => applyFieldPermissionToRule(rule, permissionMap))
    .filter(Boolean)
}

function applyFieldPermissionToRule(rule, permissionMap) {
  if (!rule || typeof rule !== 'object')
    return rule

  const next = cloneValue(rule)
  const field = String(next.field || next.props?.field || '').trim()
  const permission = field ? permissionMap.get(field) : null

  if (props.readOnly) {
    next.props = { ...(next.props || {}), disabled: true }
    next.disabled = true
  }

  if (permission?.readable === false)
    return null

  if (permission) {
    if (permission.writable === false) {
      next.props = { ...(next.props || {}), disabled: true }
      next.disabled = true
    }
    if (permission.required === true)
      next.validate = ensureRequiredRule(next.validate, next.title || field)
  }

  if (Array.isArray(next.children))
    next.children = applyFieldPermissionsToList(next.children, permissionMap)

  return next
}

function ensureRequiredRule(validate, title) {
  const list = Array.isArray(validate) ? [...validate] : []
  if (!list.some(item => item?.required))
    list.unshift({ required: true, message: `请输入${title || '该字段'}`, __forgeRequiredMessage: true, trigger: 'blur' })
  return list
}

function localizeRequiredMessages(rule) {
  const next = { ...rule }
  if (Array.isArray(next.validate))
    next.validate = next.validate.map(item => item.__forgeRequiredMessage ? { ...item, message: t('customer.enterValue', { value0: next.title || localizeLabel('该字段') }) } : item)
  if (Array.isArray(next.children))
    next.children = next.children.map(localizeRequiredMessages)
  return next
}

function applyLayoutToRules(rules) {
  return applyLayoutToList(rules || [], normalizeGridSpan(props.gridCols))
}

function applyLayoutToList(rules, span) {
  return (rules || []).map((rule) => {
    if (!rule || typeof rule !== 'object')
      return rule
    const next = cloneValue(rule)
    if (next.field)
      next.col = { ...(next.col || {}), span }
    if (Array.isArray(next.children))
      next.children = applyLayoutToList(next.children, span)
    return next
  })
}

function normalizeGridSpan(cols) {
  const count = Number(cols)
  if (!Number.isFinite(count) || count <= 1)
    return 24
  if (count >= 4)
    return 6
  if (count === 3)
    return 8
  return 12
}

function normalizeLabelWidth(value) {
  if (typeof value === 'number')
    return `${value}px`
  return value || '110px'
}

async function hydrateRules() {
  const seq = ++hydrateSeq
  const normalizedRules = cloneValue(rawRules.value) || []
  hydratedRules.value = normalizedRules
  const nextRules = await hydrateForgeBusinessPreviewRules(normalizedRules)
  if (seq !== hydrateSeq)
    return
  hydratedRules.value = nextRules
}

async function validate() {
  if (!rules.value.length)
    return true
  await formApi.value?.validate?.()
  return true
}

function getData() {
  return cloneValue(formApi.value?.formData?.() || innerValue.value) || {}
}

async function submit() {
  await validate()
  return getData()
}

defineExpose({
  validate,
  getData,
  submit,
})
</script>

<style scoped>
.flow-form-create-renderer {
  width: 100%;
}

:deep(.form-create) {
  margin: 0;
}

:deep(.el-form) {
  width: 100%;
}

:deep(.el-row) {
  row-gap: 4px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-form-item__label) {
  line-height: 1.4;
  color: var(--text-color-2, #4b5563);
  font-weight: 500;
}

:deep(.el-input),
:deep(.el-select),
:deep(.el-date-editor),
:deep(.el-textarea),
:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
</style>
