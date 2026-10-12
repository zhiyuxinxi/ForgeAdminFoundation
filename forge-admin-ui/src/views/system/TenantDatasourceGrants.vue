<template>
  <n-modal v-model:show="visible" preset="card" :title="localizeLabel('业务数据源授权')" style="width: 720px">
    <p>{{ localizeLabel('仅可授权启用且可写的业务数据源，撤销后立即停止路由。') }}</p>
    <n-data-table :columns="columns" :data="rows" :loading="loading" :row-key="row => row.datasourceId" />
  </n-modal>
</template>

<script setup>
import { NSwitch } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { localizeLabel } from '@/locales'
import { useUserStore } from '@/store'
import { request } from '@/utils'

const emit = defineEmits(['changed'])
const userStore = useUserStore()
const visible = ref(false)
const loading = ref(false)
const tenantId = ref(null)
const rows = ref([])
const pending = ref(null)
const columns = computed(() => [
  { title: localizeLabel('数据源名称'), key: 'datasourceName' },
  { title: localizeLabel('数据源编码'), key: 'datasourceCode' },
  {
    title: localizeLabel('授权'),
    key: 'granted',
    render: row => h(NSwitch, {
      'value': row.granted,
      'disabled': !userStore.isAdmin || pending.value !== null || (!row.selectable && !row.granted),
      'loading': pending.value === row.datasourceId,
      'onUpdate:value': value => setGrant(row, value),
    }),
  },
])

async function open(tenant) {
  if (!userStore.isAdmin || !tenant?.id)
    return
  tenantId.value = tenant.id
  rows.value = []
  visible.value = true
  loading.value = true
  try {
    const response = await request.get(`/system/tenant/${tenant.id}/datasource-grants`)
    if (tenantId.value === tenant.id && response.code === 200)
      rows.value = response.data || []
  }
  finally {
    if (tenantId.value === tenant.id)
      loading.value = false
  }
}

async function setGrant(row, granted) {
  if (!userStore.isAdmin || pending.value !== null || (granted && !row.selectable))
    return
  const currentTenant = tenantId.value
  pending.value = row.datasourceId
  try {
    const response = await request.post(`/system/tenant/${currentTenant}/datasource-grants/${row.datasourceId}`, null, {
      params: { granted },
    })
    if (response.code === 200 && tenantId.value === currentTenant) {
      row.granted = granted
      emit('changed', currentTenant)
    }
  }
  finally {
    pending.value = null
  }
}

defineExpose({ open })
</script>
