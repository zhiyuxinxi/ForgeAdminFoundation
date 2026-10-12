import { watch, ref, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { buildProjectPayload, getProjectDetailApi, updateProjectApi } from '@/api/project'
import { RequestDataTypeEnum } from '@/enums/httpEnum'
import { normalizeProjectStorage } from '@/utils/reportPages'
import type { ForgeProject } from '@/api/project'
import type { CreateComponentGroupType, CreateComponentType } from '@/packages/index.d'
import type { ReportProjectStorage } from '@/store/modules/chartEditStore/chartEditStore.d'

export type SaveStatus = 'idle' | 'saving' | 'saved' | 'error'

const AUTOSAVE_DELAY = 5000
const MIN_SAVE_INTERVAL = 15000

const cloneForFingerprint = <T>(value: T): T => {
  return JSON.parse(JSON.stringify(value))
}

const omitRuntimeDataset = (component: CreateComponentType | CreateComponentGroupType | any) => {
  if (!component) return
  if (component.request?.requestDataType !== RequestDataTypeEnum.STATIC && component.option) {
    delete component.option.dataset
  }
  if (component.isGroup && Array.isArray(component.groupList)) {
    component.groupList.forEach(omitRuntimeDataset)
  }
}

const createSaveFingerprint = (storageInfo: ReportProjectStorage) => {
  const cloned = cloneForFingerprint(storageInfo)
  const pages = (cloned as any).version === 2 ? (cloned as any).pages : [cloned]
  pages.forEach((page: any) => {
    const componentList = page.componentList || []
    componentList.forEach(omitRuntimeDataset)
  })
  return JSON.stringify(cloned)
}

export function useAutoSave() {
  const chartEditStore = useChartEditStore()
  const route = useRoute()

  const saveStatus = ref<SaveStatus>('idle')
  const lastSaveTime = ref<string>('')
  const saveError = ref<string>('')

  let timer: ReturnType<typeof setTimeout> | null = null
  let enabled = true
  let projectMetaLoaded = false
  let saving = false
  let lastSaveAt = 0
  let lastSavedFingerprint = ''
  const projectMeta = ref<Partial<ForgeProject> | undefined>()

  const ensureProjectMeta = async (projectId: string) => {
    if (projectMetaLoaded) return
    try {
      const res = await getProjectDetailApi(projectId)
      projectMeta.value = res?.data
      if (!chartEditStore.getProjectName && res?.data?.projectName) {
        chartEditStore.setProjectName(res.data.projectName)
      }
      if (!lastSavedFingerprint && res?.data?.componentData) {
        try {
          lastSavedFingerprint = createSaveFingerprint(normalizeProjectStorage(JSON.parse(res.data.componentData)))
        } catch (error) {
          lastSavedFingerprint = ''
        }
      }
      projectMetaLoaded = true
    } catch (error) {
      projectMetaLoaded = false
      console.warn('[useAutoSave] 获取项目元信息失败，继续尝试保存:', error)
    }
  }

  const doSave = async () => {
    const rawId = route.params.id
    const projectId = Array.isArray(rawId) ? rawId[0] : rawId as string
    if (!projectId || !enabled) return

    saveStatus.value = 'saving'
    saveError.value = ''
    try {
      await ensureProjectMeta(projectId)
      const storageInfo = chartEditStore.getProjectStorageInfo()
      const nextFingerprint = createSaveFingerprint(storageInfo)
      if (nextFingerprint === lastSavedFingerprint) {
        saveStatus.value = 'idle'
        return
      }
      const currentTime = Date.now()
      const waitTime = lastSaveAt ? MIN_SAVE_INTERVAL - (currentTime - lastSaveAt) : 0
      if (waitTime > 0) {
        timer = setTimeout(doSave, waitTime)
        saveStatus.value = 'idle'
        return
      }
      if (saving) {
        timer = setTimeout(doSave, AUTOSAVE_DELAY)
        return
      }
      saving = true
      lastSaveAt = Date.now()
      const payload = buildProjectPayload(projectId, storageInfo, undefined, projectMeta.value)
      await updateProjectApi(payload)
      lastSavedFingerprint = nextFingerprint
      projectMeta.value = {
        ...projectMeta.value,
        ...payload
      }
      saveStatus.value = 'saved'
      const now = new Date()
      lastSaveTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
      // 3 秒后恢复 idle
      setTimeout(() => {
        if (saveStatus.value === 'saved') saveStatus.value = 'idle'
      }, 3000)
    } catch (e: any) {
      saveStatus.value = 'error'
      saveError.value = e?.message || '保存失败'
    } finally {
      saving = false
    }
  }

  const scheduleSave = () => {
    if (!enabled) return
    saveStatus.value = 'idle'
    if (timer) clearTimeout(timer)
    timer = setTimeout(doSave, AUTOSAVE_DELAY)
  }

  watch(
    () => [
      chartEditStore.getEditCanvasConfig.projectName,
      chartEditStore.getEditCanvasConfig.width,
      chartEditStore.getEditCanvasConfig.height,
      chartEditStore.getEditCanvasConfig.background,
      chartEditStore.getProjectName,
      chartEditStore.getComponentList.length,
      chartEditStore.getProjectPages.length,
      chartEditStore.getProjectPages.map(item => `${item.id}:${item.name}:${item.sort}`).join('|'),
      chartEditStore.getActivePageId,
      chartEditStore.getHomePageId,
    ],
    () => {
      scheduleSave()
    }
  )

  watch(
    () => chartEditStore.getComponentList,
    () => {
      scheduleSave()
    },
    { deep: true }
  )

  onUnmounted(() => {
    enabled = false
    if (timer) clearTimeout(timer)
  })

  return { saveStatus, lastSaveTime, saveError }
}
