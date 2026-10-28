<template>
  <n-space vertical>
    <n-tree-select
      :value="orgId"
      :options="organizations"
      :disabled="disabled || !reference?.operation"
      :placeholder="L('请选择所属部门', 'Select a department')"
      :on-load="loadChildren"
      clearable
      @update:value="changeOrganization"
    />
    <n-select
      :value="selectedValue"
      :options="options"
      :multiple="multiple"
      :disabled="disabled || !orgId || !reference?.operation"
      :loading="loading"
      :clearable="clearable"
      :placeholder="placeholder || L('请选择岗位', 'Select a post')"
      filterable
      remote
      @search="search"
      @update:value="select"
    />
    <n-button v-if="hasNext" :disabled="disabled || loading" size="small" @click="loadMore">
      {{ L('加载更多', 'Load more') }}
    </n-button>
    <n-alert v-if="error" type="error">
      {{ error }}
      <n-button :disabled="disabled || loading" size="small" @click="retry">
        {{ L('重试', 'Retry') }}
      </n-button>
    </n-alert>
  </n-space>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { loadOrganizationBranch, loadPostPage, loadPostScope } from '@/components/common/identity-reference'
import { localizeLabel } from '@/locales'
import { request } from '@/utils/http'

const props = defineProps({
  reference: { type: Object, default: null },
  modelValue: { type: [String, Number, Array], default: null },
  labelValue: { type: [String, Number, Array], default: '' },
  multiple: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  clearable: { type: Boolean, default: true },
  placeholder: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'update:labelValue', 'select'])
const organizations = ref([])
const orgId = ref(null)
const rows = ref([])
const keyword = ref('')
const loading = ref(false)
const hasNext = ref(false)
const error = ref('')
let epoch = 0

const L = (zh, en) => localizeLabel(zh, { 'en-US': en })
const selectedValue = computed(() => Array.isArray(props.modelValue)
  ? props.modelValue.map(String)
  : props.modelValue == null ? null : String(props.modelValue))
const options = computed(() => {
  const ids = Array.isArray(props.modelValue) ? props.modelValue : props.modelValue == null ? [] : [props.modelValue]
  const labels = Array.isArray(props.labelValue) ? props.labelValue : [props.labelValue]
  // Keep the previous snapshot visible; it is not a new selectable candidate.
  const combined = new Map(ids.map((id, index) => [String(id), {
    value: String(id),
    label: String(labels[index] || id),
    disabled: true,
  }]))
  rows.value.forEach(post => combined.set(post.value, post))
  return [...combined.values()]
})

async function initialize() {
  const current = ++epoch
  orgId.value = null
  organizations.value = []
  rows.value = []
  hasNext.value = false
  error.value = ''
  loading.value = false
  if (!props.reference?.operation || props.disabled)
    return
  try {
    const [result, scope] = await Promise.all([
      loadOrganizationBranch(request, props.reference),
      loadPostScope(request, props.reference),
    ])
    if (current === epoch) {
      organizations.value = scope.tenantWideAllowed
        ? [{ key: 'tenant-wide', value: 'tenant-wide', label: L('租户级岗位', 'Tenant-wide posts'), isLeaf: true }, ...result]
        : result
    }
  }
  catch (cause) {
    if (current === epoch)
      error.value = cause.message || L('岗位范围加载失败', 'Unable to load post scope')
  }
}

async function loadChildren(option) {
  const current = epoch
  try {
    const result = await loadOrganizationBranch(request, props.reference, option.value)
    if (current === epoch)
      option.children = result
  }
  catch (cause) {
    if (current === epoch)
      error.value = cause.message || L('岗位范围加载失败', 'Unable to load post scope')
    throw cause
  }
}

function changeOrganization(value) {
  orgId.value = value
  keyword.value = ''
  return load(false)
}

function search(value) {
  keyword.value = value
  return load(false)
}

function loadMore() {
  return load(true)
}

async function load(append) {
  const current = ++epoch
  const afterPostId = append ? rows.value.at(-1)?.value : undefined
  if (!append)
    rows.value = []
  hasNext.value = false
  error.value = ''
  loading.value = false
  if (!orgId.value || !props.reference?.operation || props.disabled)
    return
  loading.value = true
  try {
    const result = await loadPostPage(request, props.reference, {
      orgId: orgId.value === 'tenant-wide' ? undefined : orgId.value,
      tenantWide: orgId.value === 'tenant-wide',
      keyword: keyword.value,
      afterPostId,
      pageSize: 20,
    })
    if (current !== epoch)
      return
    rows.value = append ? [...rows.value, ...result.rows] : result.rows
    hasNext.value = result.hasNext
  }
  catch (cause) {
    if (current === epoch)
      error.value = cause.message || L('岗位查询失败', 'Post lookup failed')
  }
  finally {
    if (current === epoch)
      loading.value = false
  }
}

function retry() {
  return orgId.value ? load(false) : initialize()
}

function select(value) {
  const ids = Array.isArray(value) ? value : value == null ? [] : [value]
  const selected = ids.map(id => options.value.find(option => option.value === String(id))).filter(Boolean)
  emit('update:modelValue', value)
  emit('update:labelValue', props.multiple ? selected.map(post => post.label) : selected[0]?.label || '')
  emit('select', props.multiple ? selected : selected[0] || null)
}

watch(() => [props.reference?.base, props.reference?.operation, props.disabled], initialize, { immediate: true })
onBeforeUnmount(() => {
  epoch++
})
</script>
