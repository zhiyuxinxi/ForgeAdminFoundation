<template>
  <n-modal v-model:show="visible" preset="card" :title="modalTitle" style="width: min(980px, calc(100vw - 32px))">
    <n-data-table
      remote
      size="small"
      :loading="loading"
      :columns="columns"
      :data="versionList"
      :row-key="row => row.id"
      :pagination="pagination"
      :scroll-x="980"
      @update:page="handlePageChange"
    />
  </n-modal>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { NButton, NSpace, NTag } from 'naive-ui'
import { computed, h, ref, watch } from 'vue'
import { DialogEnum } from '@/enums/pluginEnum'
import { PreviewEnum } from '@/enums/pageEnum'
import {
  getProjectVersionDetailApi,
  getProjectVersionPageApi,
  rollbackProjectVersionApi,
  type ReportProjectVersion
} from '@/api/project'
import { downloadTextFile, fetchPathByName, goDialog, openNewWindow } from '@/utils'

const props = defineProps<{
  show: boolean
  projectId?: string | number
  projectName?: string
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'rollback'): void
}>()

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
})

const modalTitle = computed(() => props.projectName ? `版本历史 - ${props.projectName}` : '版本历史')
const loading = ref(false)
const versionList = ref<ReportProjectVersion[]>([])
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: false
})

const operationMeta = {
  publish: { get label() { return uiText("发布") }, type: 'success' },
  rollback: { get label() { return uiText("回退") }, type: 'warning' }
} as const

const getOperationMeta = (operationType?: string) => {
  return operationType && operationMeta[operationType as keyof typeof operationMeta]
    ? operationMeta[operationType as keyof typeof operationMeta]
    : { label: operationType || '发布', type: 'default' }
}

const formatVersionConfig = (componentData?: string) => {
  if (!componentData) return '{}'
  try {
    return JSON.stringify(JSON.parse(componentData), null, 2)
  } catch {
    return componentData
  }
}

const normalizeFileName = (value?: string) => {
  const name = (value || 'dashboard').trim() || 'dashboard'
  return name.replace(/[\\/:*?"<>|]/g, '_').slice(0, 80)
}

const loadVersions = async () => {
  if (!props.projectId || !visible.value) return
  loading.value = true
  try {
    const res = await getProjectVersionPageApi(props.projectId, {
      pageNum: pagination.value.page,
      pageSize: pagination.value.pageSize
    })
    versionList.value = res?.data?.records || []
    pagination.value.itemCount = res?.data?.total || 0
  } catch (error: any) {
    window['$message']?.error(error?.message || uiText("获取版本历史失败"))
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadVersions()
}

const previewVersion = (row: ReportProjectVersion) => {
  const path = fetchPathByName(PreviewEnum.CHART_PREVIEW_NAME, 'href')
  if (!path) return
  const projectId = row.projectId || props.projectId
  openNewWindow(`${window.location.origin}${path}/${projectId}?versionId=${row.id}`)
}

const downloadVersion = async (row: ReportProjectVersion) => {
  try {
    const detail = (await getProjectVersionDetailApi(row.id))?.data
    const versionName = detail.versionName || row.versionName || `V${row.versionNo || row.id}`
    downloadTextFile(
      formatVersionConfig(detail.componentData),
      `${normalizeFileName(detail.projectName || props.projectName)}-${versionName}`,
      'json'
    )
  } catch (error: any) {
    window['$message']?.error(error?.message || uiText("下载版本配置失败"))
  }
}

const rollbackVersion = (row: ReportProjectVersion) => {
  goDialog({
    type: DialogEnum.WARNING,
    title: uiText("回退版本"),
    message: uiText("确定回退到 {0} 吗？当前草稿会被该版本配置覆盖，并追加一条回退记录。", { 0: (row.versionName || `V${row.versionNo}`) }),
    positiveText: uiText("确认回退"),
    onPositiveCallback: async () => {
      try {
        await rollbackProjectVersionApi(row.id)
        window['$message']?.success(uiText("版本回退成功"))
        emit('rollback')
        loadVersions()
      } catch (error: any) {
        window['$message']?.error(error?.message || uiText("版本回退失败"))
      }
    }
  })
}

const columns = computed(() => [
  {
    get title() { return uiText("版本") },
    key: 'versionName',
    width: 96,
    render(row: ReportProjectVersion) {
      return h(NTag, { type: 'info', bordered: false, size: 'small' }, { default: () => row.versionName || `V${row.versionNo || '-'}` })
    }
  },
  {
    get title() { return uiText("操作") },
    key: 'operationType',
    width: 90,
    render(row: ReportProjectVersion) {
      const meta = getOperationMeta(row.operationType)
      return h(NTag, { type: meta.type as any, bordered: false, size: 'small' }, { default: () => meta.label })
    }
  },
  { get title() { return uiText("发布人") }, key: 'publisherName', width: 120 },
  { get title() { return uiText("发布时间") }, key: 'publishTime', width: 180 },
  { get title() { return uiText("画布") }, key: 'canvasSize', width: 110, render: (row: ReportProjectVersion) => `${row.canvasWidth || '-'} x ${row.canvasHeight || '-'}` },
  {
    get title() { return uiText("操作") },
    key: 'actions',
    width: 260,
    fixed: 'right',
    render(row: ReportProjectVersion) {
      return h(NSpace, { size: 8 }, {
        default: () => [
          h(NButton, { size: 'small', secondary: true, onClick: () => previewVersion(row) }, { default: () => '预览' }),
          h(NButton, { size: 'small', secondary: true, onClick: () => downloadVersion(row) }, { default: () => '下载配置' }),
          h(NButton, { size: 'small', type: 'warning', secondary: true, onClick: () => rollbackVersion(row) }, { default: () => '回退' })
        ]
      })
    }
  }
])

watch(
  () => [props.show, props.projectId],
  () => {
    if (!props.show) return
    pagination.value.page = 1
    loadVersions()
  },
  { immediate: false }
)
</script>
