<template>
  <div class="header-right-actions">
    <span class="action-label">{{ $ui("输出") }}</span>
    <n-button
      v-for="item in comBtnList"
      :key="item.title"
      class="action-btn"
      :class="{ publish: item.title === '发布' }"
      :type="item.type"
      ghost
      @click="item.event"
    >
      <template #icon>
        <n-icon v-if="item.title === '版本'" size="16">
          <TimeOutlineIcon />
        </n-icon>
        <component v-else :is="item.icon"></component>
      </template>
      <span>{{ $ui(item.title) }}</span>
    </n-button>
    <project-version-modal
      v-model:show="versionModalVisible"
      :project-id="currentProjectId"
      :project-name="chartEditStore.getProjectName || chartEditStore.getEditCanvasConfig.projectName"
      @rollback="handleVersionRollback"
    />
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, ref } from 'vue'
import { renderIcon, goDialog, fetchPathByName, openNewWindow, setSessionStorage, getSessionStorage, downloadTextFile } from '@/utils'
import { captureProjectScreenshot } from '@/utils/capture'
import { buildApiContractDocument, buildApiContractFileName } from '@/utils/apiContractExport'
import { PreviewEnum } from '@/enums/pageEnum'
import { StorageEnum } from '@/enums/storageEnum'
import { useRoute } from 'vue-router'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { syncData } from '../../ContentEdit/components/EditTools/hooks/useSyncUpdate.hook'
import { icon } from '@/plugins'
import { cloneDeep } from 'lodash'
import { buildProjectPayload, getProjectDetailApi, publishProjectApi, updateProjectApi } from '@/api/project'
import ProjectVersionModal from '@/components/ProjectVersionModal.vue'

const { BrowsersOutlineIcon, SendIcon, AnalyticsIcon, DocumentTextIcon, TimeOutlineIcon } = icon.ionicons5
const chartEditStore = useChartEditStore()

const routerParamsInfo = useRoute()
const versionModalVisible = ref(false)
const currentProjectId = computed(() => {
  const { id } = routerParamsInfo.params
  return typeof id === 'string' ? id : id?.[0]
})

const syncProjectStorageToSession = (id: string, storageInfo: ReturnType<typeof chartEditStore.getProjectStorageInfo>) => {
  const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []
  const saveData = { id, ...storageInfo }
  const repeateIndex = sessionStorageInfo.findIndex((e: { id: string }) => String(e.id) === String(id))

  if (repeateIndex !== -1) {
    sessionStorageInfo.splice(repeateIndex, 1, saveData)
  } else {
    sessionStorageInfo.push(saveData)
  }
  setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
}

const buildPreviewUrl = (path: string, id: string, pageId?: string, modalPageId?: string) => {
  const baseUrl = `${path}/${id}`
  const searchParams = new URLSearchParams()
  if (pageId) searchParams.set('pageId', pageId)
  if (modalPageId) searchParams.set('modalPageId', modalPageId)
  const query = searchParams.toString()
  return query ? `${baseUrl}?${query}` : baseUrl
}

const assertProjectComponentDataSaved = async (id: string, expectedComponentData?: string) => {
  if (!expectedComponentData) return
  const res = await getProjectDetailApi(id)
  const actualComponentData = res?.data?.componentData || ''
  if (actualComponentData !== expectedComponentData) {
    const countComponents = (componentData?: string) => {
      if (!componentData) return 0
      const parsed = JSON.parse(componentData)
      if (Array.isArray(parsed.pages)) {
        return parsed.pages.reduce((total: number, page: any) => total + (page.componentList?.length || 0), 0)
      }
      return parsed.componentList?.length ?? 0
    }
    const expectedLength = countComponents(expectedComponentData)
    const actualLength = countComponents(actualComponentData)
    throw new Error(uiText("项目配置保存校验失败，当前 {0} 个组件，预期 {1} 个组件", { 0: (actualLength), 1: (expectedLength) }))
  }
}

// 预览
const previewHandle = () => {
  const path = fetchPathByName(PreviewEnum.CHART_PREVIEW_NAME, 'href')
  if (!path) return
  const { id } = routerParamsInfo.params
  // id 标识
  const previewId = typeof id === 'string' ? id : id[0]
  const storageInfo = chartEditStore.getProjectStorageInfo()
  const previewUrl = buildPreviewUrl(path, previewId, storageInfo.activePageId)
  const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []

  if (sessionStorageInfo?.length) {
    const repeateIndex = sessionStorageInfo.findIndex((e: { id: string }) => e.id === previewId)
    // 重复替换
    if (repeateIndex !== -1) {
      sessionStorageInfo.splice(repeateIndex, 1, { id: previewId, ...storageInfo })
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
    } else {
      sessionStorageInfo.push({
        id: previewId,
        ...storageInfo
      })
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
    }
  } else {
    setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, [{ id: previewId, ...storageInfo }])
  }
  // 跳转
  openNewWindow(previewUrl)
}

// 发布
const sendHandle = async () => {
  const { id } = routerParamsInfo.params
  const previewId = typeof id === 'string' ? id : id[0]

  try {
    window['$message'].loading(uiText("正在生成项目截图..."))
    console.log('[Publish] 开始发布, projectId:', previewId)
    const currentProject = (await getProjectDetailApi(previewId))?.data
    if (!chartEditStore.getProjectName && currentProject?.projectName) {
      chartEditStore.setProjectName(currentProject.projectName)
    }
    const storageInfo = chartEditStore.getProjectStorageInfo()

    // 尝试生成截图 - 使用正确的画布元素选择器
    let indexImg: string | undefined = undefined
    const canvasElement = document.querySelector('.go-edit-range')
    console.log('[Screenshot] 画布元素:', canvasElement)
    if (canvasElement) {
      indexImg = await captureProjectScreenshot(previewId, canvasElement as HTMLElement)
      console.log('[Screenshot] 截图文件ID:', indexImg)
    } else {
      console.warn('[Screenshot] 未找到画布元素 .go-edit-range')
      throw new Error(uiText("未找到画布元素，无法生成项目截图"))
    }

    // 构建项目数据，包含截图
    const projectPayload = buildProjectPayload(previewId, storageInfo, indexImg, currentProject)
    console.log('[Publish] 项目数据:', projectPayload)

    await updateProjectApi(projectPayload)
    await assertProjectComponentDataSaved(previewId, projectPayload.componentData)
    syncProjectStorageToSession(previewId, storageInfo)

    // 同步写入 sessionStorage，确保预览页优先读到最新数据
    const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []
    const idx = sessionStorageInfo.findIndex((e: { id: string }) => e.id === previewId)
    if (idx !== -1) {
      sessionStorageInfo.splice(idx, 1, { id: previewId, ...storageInfo })
    } else {
      sessionStorageInfo.push({ id: previewId, ...storageInfo })
    }
    setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)

    const previewPath = fetchPathByName(PreviewEnum.CHART_PREVIEW_NAME, 'href')
    if (!previewPath) {
      window['$message'].error(uiText("获取预览路径失败"))
      return
    }
    const previewUrl = `${window.location.origin}${previewPath}/${previewId}`
    await publishProjectApi(previewId, previewUrl)
    await assertProjectComponentDataSaved(previewId, projectPayload.componentData)

    window['$message'].success(uiText("发布成功，项目截图和预览链接已更新，可在项目列表中预览或分享"))
  } catch (error: any) {
    console.error('[Publish] 发布失败:', error)
    window['$message'].error(error?.message || uiText("发布失败"))
  }
}

// 导出接口规范文档
const exportApiContractHandle = () => {
  const document = buildApiContractDocument({
    canvasConfig: chartEditStore.getEditCanvasConfig,
    requestGlobalConfig: chartEditStore.getRequestGlobalConfig,
    componentList: chartEditStore.getComponentList,
    includeStatic: true
  })

  if (!document.endpoints.length) {
    window['$message'].warning(uiText("当前画布没有可导出的组件数据规范"))
    return
  }

  const fileName = buildApiContractFileName(document.meta.projectName)
  const warningText = document.warnings.length ? `\n\n检测到 ${document.warnings.length} 条提示，建议导出后检查 URL、dataset 和 filter 说明。` : ''

  goDialog({
    message: uiText("将导出 {0} 个组件数据规范，后端可按文档中的 dataset 结构开发接口，接口地址可后续自行选择或配置。{1}", { 0: (document.endpoints.length), 1: (warningText) }),
    positiveText: uiText("下载 Markdown"),
    negativeText: uiText("下载 JSON"),
    onPositiveCallback: () => {
      downloadTextFile(document.markdown, fileName, 'md')
      window['$message'].success(uiText("接口规范 Markdown 已导出"))
    },
    onNegativeCallback: () => {
      downloadTextFile(document.json, fileName, 'json')
      window['$message'].success(uiText("接口规范 JSON 已导出"))
    }
  })
}

const openVersionModal = () => {
  versionModalVisible.value = true
}

const handleVersionRollback = () => {
  window.location.reload()
}

const btnList = [
  {
    select: true,
    title: '同步内容',
    type: 'primary',
    icon: renderIcon(AnalyticsIcon),
    event: syncData
  },
  {
    select: true,
    title: '接口文档',
    icon: renderIcon(DocumentTextIcon),
    event: exportApiContractHandle
  },
  {
    select: true,
    title: '版本',
    icon: renderIcon(TimeOutlineIcon),
    event: openVersionModal
  },
  {
    select: true,
    title: '预览',
    icon: renderIcon(BrowsersOutlineIcon),
    event: previewHandle
  },
  {
    select: true,
    title: '发布',
    icon: renderIcon(SendIcon),
    event: sendHandle
  }
]

const comBtnList = computed(() => {
  if (chartEditStore.getEditCanvas.isCodeEdit) {
    return btnList
  }
  const cloneList = cloneDeep(btnList)
  cloneList.shift()
  return cloneList
})
</script>

<style lang="scss" scoped>
.header-right-actions {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px;
  border-radius: 999px;
  border: 1px solid rgba(var(--app-theme-rgb), 0.12);
  background:
    linear-gradient(90deg, rgba(var(--app-theme-rgb), 0.08), transparent),
    rgba(2, 6, 23, 0.3);

  .action-label {
    padding: 0 7px 0 10px;
    font-size: 10px;
    letter-spacing: 1px;
    @include fetch-color(4);
  }

  :deep(.n-button) {
    height: 32px;
    border-radius: 999px;
    padding: 0 14px;
    border-color: rgba(var(--app-theme-rgb), 0.16);
    background: rgba(15, 23, 42, 0.36);
    font-size: 12px;
    letter-spacing: 0.4px;
    transition: all 0.22s ease;

    &:hover {
      border-color: rgba(var(--app-theme-rgb), 0.34);
      background: rgba(var(--app-theme-rgb), 0.1);
      box-shadow: 0 0 16px rgba(var(--app-theme-rgb), 0.16);
      transform: translateY(-1px);
    }
  }

  .publish {
    background:
      linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.22), rgba(167, 139, 250, 0.16)),
      rgba(15, 23, 42, 0.5);
    box-shadow: 0 0 18px rgba(var(--app-theme-rgb), 0.14);
  }
}
</style>
