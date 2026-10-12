import { uiText } from '@/i18n/display'
import { ref, nextTick } from 'vue'
import { UploadCustomRequestOptions } from 'naive-ui'
import { FileTypeEnum } from '@/enums/fileTypeEnum'
import { readFile, goDialog, JSONParse } from '@/utils'
import { useSync } from '@/views/chart/hooks/useSync.hook'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { extractPageStorage, normalizeProjectStorage } from '@/utils/reportPages'

export const useFile = () => {
  const importUploadFileListRef = ref()
  const { updateComponent } = useSync()
  const chartEditStore = useChartEditStore()

  // 上传-前置
  //@ts-ignore
  const importBeforeUpload = ({ file }) => {
    importUploadFileListRef.value = []
    const type = file.file.type
    if (type !== FileTypeEnum.JSON && type !== FileTypeEnum.TXT) {
      window['$message'].warning(uiText("仅支持上传 【JSON】 格式文件，请重新上传！"))
      return false
    }
    return true
  }

  // 上传-导入
  const importCustomRequest = (options: UploadCustomRequestOptions) => {
    const { file } = options
    nextTick(() => {
      if (file.file) {
        readFile(file.file).then((fileData: any) => {
          goDialog({
            message: uiText("请选择导入方式:"),
            positiveText: uiText("新增（可撤回）"),
            negativeText: uiText("覆盖（不可撤回）"),
            negativeButtonProps: { type: 'info', ghost: false },
            // 新增
            onPositiveCallback: async () => {
              try {
                const projectStorage = normalizeProjectStorage(JSONParse(fileData))
                const pageStorage = extractPageStorage(projectStorage, projectStorage.activePageId)
                await updateComponent(pageStorage, false, true)
                window['$message'].success(uiText("导入成功！"))
              } catch (error) {
                console.log(error)
                window['$message'].error(uiText("组件导入失败，请检查文件完整性!"))
              }
            },
            // 覆盖
            onNegativeCallback: async () => {
              try {
                const projectStorage = normalizeProjectStorage(JSONParse(fileData))
                const pageStorage = chartEditStore.loadProjectStorage(projectStorage, projectStorage.activePageId)
                await updateComponent(pageStorage, true, false)
                window['$message'].success(uiText("导入成功！"))
              } catch (error) {
                console.log(error)
                window['$message'].error(uiText("组件导入失败，请检查文件完整性!"))
              }
            }
          })
        })
      } else {
        window['$message'].error(uiText("导入失败，请检查数据或联系管理员！"))
      }
    })
  }

  return {
    importUploadFileListRef,
    importBeforeUpload,
    importCustomRequest
  }
}
