<template>
  <div class="go-edit">
    <div class="edit-bg-orb"></div>
    <n-layout class="edit-layout">
      <n-layout-header class="go-edit-header">
        <div class="header-left">
          <n-text class="go-edit-title">{{ $ui("页面在线编辑器") }}</n-text>
          <n-button v-if="showOpenFilePicker" size="small" ghost @click="importJSON">
            <template #icon>
              <n-icon><download-icon /></n-icon>
            </template>
            {{ $ui("导入") }}
          </n-button>
        </div>
        <n-space align="center">
          <n-tag :bordered="false" size="small"> {{ $ui("Ctrl + S 更新视图") }} </n-tag>
          <n-button v-if="showOpenFilePicker" size="small" ghost type="primary" @click="updateSync">
            <template #icon>
              <n-icon><analytics-icon /></n-icon>
            </template>
            {{ $ui("保存") }}
          </n-button>
        </n-space>
      </n-layout-header>
      <n-layout-content class="edit-content">
        <div class="editor-wrapper">
          <monaco-editor
            v-model:modelValue="content"
            language="json"
            :editorOptions="{
              lineNumbers: 'on',
              minimap: { enabled: true }
            }"
          />
        </div>
      </n-layout-content>
    </n-layout>
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { ref } from 'vue'
import { MonacoEditor } from '@/components/Pages/MonacoEditor'
import { SavePageEnum } from '@/enums/editPageEnum'
import {
  fetchRouteParamsLocation,
  getLocalStorage,
  getSessionStorage,
  setSessionStorage,
  JSONStringify,
  JSONParse,
  setTitle,
  goDialog
} from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { icon } from '@/plugins'
import { getProjectDetailApi } from '@/api/project'
import { normalizeProjectStorage } from '@/utils/reportPages'
import type { ReportProjectStorage } from '@/store/modules/chartEditStore/chartEditStore.d'

const { DownloadIcon, AnalyticsIcon } = icon.ionicons5
const showOpenFilePicker: Function = (window as any).showOpenFilePicker
const content = ref('')

window['$message'].warning(uiText("请不要刷新此窗口！"))

const stripStorageId = (storage: any): ReportProjectStorage | null => {
  if (!storage) return null
  const { id: _id, ...projectStorage } = storage
  return projectStorage
}

const findStorageById = (storageList: any, id: string) => {
  if (!Array.isArray(storageList)) return null
  const storageItem = storageList.find((item: any) => String(item?.id) === String(id))
  return stripStorageId(storageItem)
}

const writeProjectToSession = (id: string, storage: ReportProjectStorage) => {
  const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []
  const nextStorageInfo = Array.isArray(sessionStorageInfo) ? [...sessionStorageInfo] : []
  const repeatIndex = nextStorageInfo.findIndex((item: any) => String(item?.id) === String(id))
  const storageItem = { ...storage, id }

  if (repeatIndex !== -1) {
    nextStorageInfo.splice(repeatIndex, 1, storageItem)
  } else {
    nextStorageInfo.push(storageItem)
  }

  setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, nextStorageInfo)
}

const getProjectStorageByRoute = async () => {
  const id = fetchRouteParamsLocation()
  const sessionStorageInfo = findStorageById(getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST), id)
  if (sessionStorageInfo) return sessionStorageInfo

  const localStorageInfo = findStorageById(getLocalStorage(StorageEnum.GO_CHART_STORAGE_LIST), id)
  if (localStorageInfo) return localStorageInfo

  const res = await getProjectDetailApi(id)
  const project = res?.data
  if (project?.componentData) {
    return JSONParse(project.componentData)
  }
  return normalizeProjectStorage({}, project?.projectName || '新项目')
}

// 从sessionStorage 获取数据
async function getDataBySession() {
  try {
    const projectStorage = normalizeProjectStorage(await getProjectStorageByRoute())
    setTitle(`编辑-${projectStorage.projectName || projectStorage.pages[0]?.name || '项目'}`)
    content.value = JSONStringify(projectStorage)
  } catch (error) {
    window['$message'].error(uiText("项目数据读取失败，请检查项目 JSON 是否损坏！"))
    console.log(error)
  }
}
setTimeout(getDataBySession)

// 返回父窗口
function back() {
  window.opener.name = Date.now()
  window.open(window.opener.location.href, window.opener.name)
}

// 导入json文本
function importJSON() {
  goDialog({
    message: uiText("导入数据将覆盖内容，此操作不可撤回，是否继续？"),
    isMaskClosable: true,
    transformOrigin: 'center',
    onPositiveCallback: async () => {
      try {
        const files = await showOpenFilePicker()
        const file = await files[0].getFile()
        const fr = new FileReader()
        fr.readAsText(file)
        fr.onloadend = () => {
          content.value = (fr.result || '').toString()
        }
        window['$message'].success(uiText("导入成功！"))
      } catch (error) {
        window['$message'].error(uiText("导入失败，请检查文件是否损坏！"))
        console.log(error)
      }
    }
  })
}

// 同步数据编辑页
if (window.opener) {
  window.opener.addEventListener(SavePageEnum.CHART, (e: any) => {
    window['$message'].success(uiText("正在进行更新..."))
    const projectStorage = normalizeProjectStorage(e.detail)
    writeProjectToSession(fetchRouteParamsLocation(), projectStorage)
    content.value = JSONStringify(projectStorage)
  })
}

// 保存按钮同步数据
document.addEventListener('keydown', function (e) {
  if (e.keyCode == 83 && (navigator.platform.match('Mac') ? e.metaKey : e.ctrlKey)) {
    e.preventDefault()
    updateSync()
  }
})

// 失焦保存（暂时关闭）
// addEventListener('blur', updateSync)

// 同步更新
async function updateSync() {
  if (!window.opener) {
    return window['$message'].error(uiText("源窗口已关闭，视图同步失败！"))
  }
  goDialog({
    message: uiText("是否覆盖源视图内容? 此操作不可撤！"),
    isMaskClosable: true,
    transformOrigin: 'center',
    onPositiveCallback: () => {
      try {
        const detail = normalizeProjectStorage(JSONParse(content.value))
        delete (detail as any).id
        // 保持id不变
        window.opener.dispatchEvent(new CustomEvent(SavePageEnum.JSON, { detail }))
        window['$message'].success(uiText("正在同步内容..."))
      } catch (e) {
        window['$message'].error(uiText("内容格式有误"))
        console.log(e)
      }
    }
  })
}

// 关闭页面发送关闭事件
window.onbeforeunload = () => {
  if (window.opener) {
    window.opener.dispatchEvent(new CustomEvent(SavePageEnum.CLOSE))
  }
}
</script>

<style lang="scss" scoped>
.go-edit {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: #06090f;

  .edit-bg-orb {
    position: fixed;
    top: -20%;
    right: -10%;
    width: 500px;
    height: 500px;
    border-radius: 50%;
    background: rgba(var(--app-theme-rgb), 0.1);
    filter: blur(120px);
    pointer-events: none;
    z-index: 0;
    animation: orbBreath 10s ease-in-out infinite alternate;
  }

  @keyframes orbBreath {
    0% { transform: scale(1); opacity: 0.6; }
    100% { transform: scale(1.3); opacity: 1; }
  }

  .edit-layout {
    position: relative;
    z-index: 1;
    background: transparent;
  }

  .go-edit-header {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 0 22px;
    background: rgba(10, 14, 23, 0.86);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border-bottom: 1px solid rgba(var(--app-theme-rgb), 0.1);

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(var(--app-theme-rgb), 0.35), transparent);
      pointer-events: none;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .go-edit-title {
      font-size: 16px;
      font-weight: 700;
      letter-spacing: 1px;
      color: rgba(226, 232, 240, 0.92);
      text-shadow: 0 0 12px rgba(var(--app-theme-rgb), 0.2);
    }

    :deep(.n-button) {
      height: 32px;
      border-radius: 8px;
      border-color: rgba(var(--app-theme-rgb), 0.14);
      background: rgba(15, 23, 42, 0.36);
      transition: all 0.22s ease;

      &:hover {
        border-color: rgba(var(--app-theme-rgb), 0.28);
        background: rgba(var(--app-theme-rgb), 0.1);
        box-shadow: 0 0 16px rgba(var(--app-theme-rgb), 0.16);
        transform: translateY(-1px);
      }
    }

    :deep(.n-tag) {
      height: 28px;
      font-size: 11px;
      background: rgba(var(--app-theme-rgb), 0.08);
      color: rgba(var(--app-theme-rgb), 0.78);
      border: 1px solid rgba(var(--app-theme-rgb), 0.1);
      border-radius: 8px;
      letter-spacing: 0.5px;
    }
  }

  .edit-content {
    position: relative;

    .editor-wrapper {
      margin: 12px;
      height: calc(100vh - 56px - 24px);
      border-radius: 12px;
      overflow: hidden;
      border: 1px solid rgba(var(--app-theme-rgb), 0.08);
      box-shadow:
        0 0 60px rgba(0, 0, 0, 0.4),
        inset 0 1px 0 rgba(255, 255, 255, 0.02);
      background: #0a0e17;
    }
  }

  @include deep() {
    .go-editor-area {
      height: calc(100vh - 56px - 24px) !important;
    }
  }
}
</style>
