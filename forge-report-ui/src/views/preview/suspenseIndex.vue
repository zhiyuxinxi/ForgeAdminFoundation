<template>
  <div :class="`go-preview ${chartEditStore.editCanvasConfig.previewScaleType}`" @mousedown="dragCanvas">
    <template v-if="isActiveModalPage">
      <div ref="entityRef" class="go-preview-entity">
        <div ref="previewRef" class="go-preview-scale go-preview-modal-direct-scale"></div>
      </div>
    </template>
    <template v-else-if="showEntity">
      <!-- 实体区域 -->
      <div ref="entityRef" class="go-preview-entity">
        <!-- 缩放层 -->
        <div ref="previewRef" class="go-preview-scale">
          <!-- 展示层 -->
          <transition :name="previewTransitionName" mode="out-in">
            <div
              v-if="show"
              :key="chartEditStore.getActivePageId"
              class="go-preview-page"
              :style="previewRefStyle"
            >
              <!-- 渲染层 -->
              <preview-render-list :key="chartEditStore.getActivePageId"></preview-render-list>
            </div>
          </transition>
        </div>
      </div>
    </template>
    <template v-else>
      <!-- 缩放层 -->
      <div ref="previewRef" class="go-preview-scale">
        <!-- 展示层 -->
        <transition :name="previewTransitionName" mode="out-in">
          <div
            v-if="show"
            :key="chartEditStore.getActivePageId"
            class="go-preview-page"
            :style="previewRefStyle"
          >
            <!-- 渲染层 -->
            <preview-render-list :key="chartEditStore.getActivePageId"></preview-render-list>
          </div>
        </transition>
      </div>
    </template>
    <transition
      v-for="modal in modalRenderList"
      :key="modal.id"
      :name="getModalTransitionName(modal)"
      appear
    >
      <div
        class="go-preview-modal-layer"
        :class="[
          `placement-${modal.modalConfig.placement || 'center'}`,
          `theme-${modal.modalConfig.theme || 'screen'}`,
          { 'no-mask': modal.modalConfig.showMask === false }
        ]"
        :style="getModalLayerStyle(modal)"
        @click.self="handleModalMaskClick(modal)"
      >
        <div class="go-preview-modal" :style="getModalBoxStyle(modal)">
          <div
            v-if="isModalTitleVisible(modal)"
            class="go-preview-modal-titlebar"
            :style="getModalTitlebarStyle(modal)"
          >
            <span>{{ getModalTitle(modal) }}</span>
          </div>
          <button
            v-if="modal.modalConfig.showClose !== false && modal.id !== DIRECT_MODAL_PREVIEW_ID"
            class="go-preview-modal-close"
            :style="getModalCloseStyle(modal)"
            type="button"
            :aria-label="$ui(&quot;关闭弹窗&quot;)"
            @click.stop="handleModalClose(modal)"
          >
            ×
          </button>
          <div class="go-preview-modal-canvas" :style="getModalCanvasStyle(modal)">
            <preview-render-list
              :component-list="modal.pageStorage.componentList"
              :canvas-config="modal.pageStorage.editCanvasConfig"
              :request-global-config="modal.pageStorage.requestGlobalConfig"
              :page-context="modal.context"
            ></preview-render-list>
          </div>
          <div
            v-if="isModalFooterVisible(modal)"
            class="go-preview-modal-footer"
          >
            <button
              v-if="modal.modalConfig.showCancel !== false"
              class="go-preview-modal-action secondary"
              :class="{ loading: isModalActionSubmitting(modal, 'cancel') }"
              type="button"
              :disabled="isModalActionSubmitting(modal, 'cancel')"
              @click.stop="handleModalButtonAction(modal, modal.modalConfig.cancelAction, 'cancel')"
            >
              {{ isModalActionSubmitting(modal, 'cancel') ? $ui("提交中...") : (modal.modalConfig.cancelText || $ui("取消")) }}
            </button>
            <button
              v-if="modal.modalConfig.showConfirm !== false"
              class="go-preview-modal-action primary"
              :class="{ loading: isModalActionSubmitting(modal, 'confirm') }"
              type="button"
              :disabled="isModalActionSubmitting(modal, 'confirm')"
              @click.stop="handleModalButtonAction(modal, modal.modalConfig.confirmAction, 'confirm')"
            >
              {{ isModalActionSubmitting(modal, 'confirm') ? $ui("提交中...") : (modal.modalConfig.confirmText || $ui("确认")) }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { PreviewRenderList } from './components/PreviewRenderList'
import { getFilterStyle, setTitle } from '@/utils'
import {
  getPreviewHashInfo,
  getSessionStorageInfo,
  keyRecordHandle,
  dragCanvas,
  restorePreviewPageFromUrl,
  switchPreviewPage
} from './utils'
import { useComInstall } from './hooks/useComInstall.hook'
import { useScale } from './hooks/useScale.hook'
import { useStore } from './hooks/useStore.hook'
import { PreviewScaleEnum } from '@/enums/styleEnum'
import type { ChartEditStorageType } from './index.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { useInitVChartsTheme } from '@/hooks'
import { resolveAssetSourceUrl } from '@/utils'
import { customizeHttp } from '@/api/http'
import { createDefaultModalConfig, normalizeModalConfig } from '@/utils/reportPages'
import type {
  ChartEditStorage,
  ReportModalButtonAction,
  ReportModalRuntime
} from '@/store/modules/chartEditStore/chartEditStore.d'

// const localStorageInfo: ChartEditStorageType = getSessionStorageInfo() as ChartEditStorageType

await getSessionStorageInfo()
const chartEditStore = useChartEditStore() as unknown as ChartEditStorageType
const resolvedBackgroundImage = ref('')
const submittingModalActionKey = ref('')
const resolvedModalBgMap = ref<Map<string, string>>(new Map())

const DIRECT_MODAL_PREVIEW_ID = '__direct_modal_preview__'

useStore(chartEditStore)
const { entityRef, previewRef } = useScale(chartEditStore)
const { show } = useComInstall(chartEditStore)

setTitle(`预览-${chartEditStore.editCanvasConfig.projectName}`)

const previewRefStyle = computed(() => {
  const canvas = chartEditStore.editCanvasConfig
  const computedBackground = canvas.selectColor
    ? { background: canvas.background }
    : resolvedBackgroundImage.value
      ? { background: `url(${resolvedBackgroundImage.value}) center center / cover no-repeat !important` }
      : {}
  return {
    overflow: 'hidden',
    position: 'relative',
    width: canvas.width ? `${canvas.width || 100}px` : '100%',
    height: canvas.height ? `${canvas.height}px` : '100%',
    ...computedBackground,
    ...getFilterStyle(chartEditStore.editCanvasConfig)
  }
})

const activePage = computed(() => {
  return chartEditStore.getProjectPages.find(page => page.id === chartEditStore.getActivePageId)
})

const isActiveModalPage = computed(() => activePage.value?.pageType === 'modal')

const currentPageStorage = computed<ChartEditStorage>(() => ({
  editCanvasConfig: chartEditStore.editCanvasConfig,
  requestGlobalConfig: chartEditStore.requestGlobalConfig,
  componentList: chartEditStore.componentList
}))

const directPreviewModal = computed<ReportModalRuntime | undefined>(() => {
  if (!isActiveModalPage.value || !activePage.value || !show.value) return undefined
  return {
    id: DIRECT_MODAL_PREVIEW_ID,
    pageId: activePage.value.id,
    pageName: activePage.value.name,
    context: chartEditStore.getRuntimePageContext || {},
    pageStorage: currentPageStorage.value,
    modalConfig: normalizeModalConfig(activePage.value.modalConfig || createDefaultModalConfig())
  }
})

const modalRenderList = computed(() => {
  return directPreviewModal.value ? [directPreviewModal.value] : chartEditStore.getModalStack
})

const getComponentBottom = (component: any): number => {
  const attr = component?.attr || {}
  const selfBottom = Number(attr.y || 0) + Number(attr.h || 0)
  const groupBottom = Array.isArray(component?.groupList)
    ? component.groupList.reduce((max: number, item: any) => Math.max(max, getComponentBottom(item)), 0)
    : 0
  return Math.max(selfBottom, groupBottom)
}

const getModalAutoHeight = (modal: ReportModalRuntime) => {
  const bottom = (modal.pageStorage.componentList || []).reduce((max: number, item: any) => {
    return Math.max(max, getComponentBottom(item))
  }, 0)
  const minHeight = modal.modalConfig.height || modal.pageStorage.editCanvasConfig.height || 420
  const edgePadding = isModalFooterVisible(modal) ? 74 : 12
  return Math.max(120, minHeight, bottom + edgePadding)
}

const getModalContentSize = (modal: ReportModalRuntime) => {
  const canvas = modal.pageStorage.editCanvasConfig
  return {
    width: modal.modalConfig.width || canvas.width || 720,
    height: modal.modalConfig.heightMode === 'auto'
      ? getModalAutoHeight(modal)
      : modal.modalConfig.height || canvas.height || 420
  }
}

const isModalTitleVisible = (modal: ReportModalRuntime) => {
  return modal.modalConfig.showTitle !== false
}

const isModalFooterVisible = (modal: ReportModalRuntime) => {
  return modal.modalConfig.showFooter === true
}

const getModalTitle = (modal: ReportModalRuntime) => {
  return modal.modalConfig.title?.trim() || modal.pageName || '弹窗'
}

const getModalTitlebarStyle = (modal: ReportModalRuntime) => {
  const align = modal.modalConfig.titleAlign || 'left'
  const justifyContentMap = {
    left: 'flex-start',
    center: 'center',
    right: 'flex-end'
  }
  return {
    height: `${modal.modalConfig.titleHeight || 44}px`,
    color: modal.modalConfig.titleColor,
    background: modal.modalConfig.titleBackground,
    fontSize: `${modal.modalConfig.titleFontSize || 15}px`,
    fontWeight: modal.modalConfig.titleFontWeight || 650,
    justifyContent: justifyContentMap[align]
  }
}

const getModalCloseStyle = (modal: ReportModalRuntime) => {
  const titleHeight = modal.modalConfig.titleHeight || 44
  return {
    top: `${Math.max(6, Math.round((titleHeight - 32) / 2))}px`
  }
}

const getModalLayerStyle = (modal: ReportModalRuntime) => {
  const opacity = modal.modalConfig.maskOpacity ?? 0.58
  return {
    background: modal.modalConfig.showMask === false
      ? 'transparent'
      : `rgba(0, 0, 0, ${opacity})`
  }
}

const getModalBoxStyle = (modal: ReportModalRuntime) => {
  const { width, height } = getModalContentSize(modal)
  return {
    width: `${width}px`,
    height: `${height}px`,
    borderRadius: `${modal.modalConfig.borderRadius ?? 8}px`
  }
}

const getModalCanvasStyle = (modal: ReportModalRuntime) => {
  const canvas = modal.pageStorage.editCanvasConfig
  const { width, height } = getModalContentSize(modal)
  const resolvedBg = modal.id === DIRECT_MODAL_PREVIEW_ID
    ? resolvedBackgroundImage.value
    : (canvas.backgroundImage ? (resolvedModalBgMap.value.get(canvas.backgroundImage) || canvas.backgroundImage) : '')
  const computedBackground = canvas.selectColor
    ? { background: canvas.background }
    : resolvedBg
      ? { background: `url(${resolvedBg}) center center / cover no-repeat` }
      : {}
  return {
    overflow: 'hidden',
    position: 'relative',
    width: `${width}px`,
    height: `${height}px`,
    ...computedBackground,
    ...getFilterStyle(canvas)
  }
}

const handleModalMaskClick = (modal: ReportModalRuntime) => {
  if (modal.id === DIRECT_MODAL_PREVIEW_ID) return
  if (modal.modalConfig.maskClosable !== false) {
    chartEditStore.closeModal(modal.id)
  }
}

const handleModalClose = (modal: ReportModalRuntime) => {
  if (modal.id === DIRECT_MODAL_PREVIEW_ID) return
  chartEditStore.closeModal(modal.id)
}

const getSubmittingKey = (modal: ReportModalRuntime, role: 'cancel' | 'confirm') => {
  return `${modal.id}:${role}`
}

const isModalActionSubmitting = (modal: ReportModalRuntime, role: 'cancel' | 'confirm') => {
  return submittingModalActionKey.value === getSubmittingKey(modal, role)
}

const runModalSuccessAction = async (
  modal: ReportModalRuntime,
  action: ReportModalButtonAction
) => {
  const successAction = action.submitSuccessAction || 'closeModal'
  if (successAction === 'none') return
  if (successAction === 'closeModal') {
    handleModalClose(modal)
    return
  }
  if (successAction === 'goPage' && action.submitSuccessTargetPageId) {
    await switchPreviewPage(action.submitSuccessTargetPageId, modal.context || {})
    return
  }
  if (successAction === 'openModal' && action.submitSuccessTargetPageId) {
    chartEditStore.openModal(action.submitSuccessTargetPageId, modal.context || {})
  }
}

const handleModalButtonAction = async (
  modal: ReportModalRuntime,
  action?: ReportModalButtonAction,
  role: 'cancel' | 'confirm' = 'confirm'
) => {
  const actionType = action?.type || 'closeModal'
  if (actionType === 'none') return
  if (actionType === 'closeModal') {
    handleModalClose(modal)
    return
  }
  if (actionType === 'goPage' && action.targetPageId) {
    await switchPreviewPage(action.targetPageId, modal.context || {})
    return
  }
  if (actionType === 'openModal' && action.targetPageId) {
    chartEditStore.openModal(action.targetPageId, modal.context || {})
    return
  }
  if (actionType === 'submitRequest') {
    if (!action?.requestConfig) {
      window['$message']?.warning?.(uiText("请先配置弹窗按钮提交接口"))
      return
    }
    submittingModalActionKey.value = getSubmittingKey(modal, role)
    try {
      await customizeHttp(
        action.requestConfig,
        modal.pageStorage.requestGlobalConfig,
        modal.pageStorage.componentList,
        modal.context || {}
      )
      window['$message']?.success?.(uiText("提交成功"))
      await runModalSuccessAction(modal, action)
    } catch (error) {
      window['$message']?.error?.(uiText("提交失败，请检查接口配置"))
      throw error
    } finally {
      submittingModalActionKey.value = ''
    }
  }
}

const getModalTransitionName = (modal: ReportModalRuntime) => {
  return `go-preview-modal-${modal.modalConfig.animation || 'zoom'}`
}

const openRouteModal = async (modalPageId?: string, pageContext: Record<string, any> = {}) => {
  if (!modalPageId || !show.value) return
  await nextTick()
  chartEditStore.closeAllModals()
  chartEditStore.openModal(modalPageId, pageContext)
}

watch(
  () => chartEditStore.editCanvasConfig.backgroundImage,
  async (newValue) => {
    resolvedBackgroundImage.value = newValue ? await resolveAssetSourceUrl(newValue) : ''
  },
  { immediate: true }
)

watch(
  () => chartEditStore.getModalStack.map(m => m.pageStorage.editCanvasConfig.backgroundImage).filter(Boolean) as string[],
  async (images) => {
    for (const img of images) {
      if (resolvedModalBgMap.value.has(img)) continue
      const resolved = await resolveAssetSourceUrl(img)
      const next = new Map(resolvedModalBgMap.value)
      next.set(img, resolved)
      resolvedModalBgMap.value = next
    }
  },
  { immediate: true, deep: true }
)

const showEntity = computed(() => {
  const type = chartEditStore.editCanvasConfig.previewScaleType
  return type === PreviewScaleEnum.SCROLL_Y || type === PreviewScaleEnum.SCROLL_X
})

const previewTransitionName = computed(() => {
  const transition = chartEditStore.getRuntimePageTransition || chartEditStore.getPageTransition
  return transition === 'none' ? 'go-preview-page-none' : `go-preview-page-${transition || 'fade'}`
})

let stopInitialRouteModalWatch: () => void = () => {}
stopInitialRouteModalWatch = watch(
  show,
  async value => {
    if (!value) return
    const { modalPageId, pageContext } = getPreviewHashInfo()
    await openRouteModal(modalPageId, pageContext)
    stopInitialRouteModalWatch()
  },
  { immediate: true }
)

// 开启键盘监听
keyRecordHandle()

const handlePreviewHistoryChange = async () => {
  const runtimeInfo = await restorePreviewPageFromUrl()
  await openRouteModal(runtimeInfo?.modalPageId, runtimeInfo?.pageContext || {})
}

onMounted(() => {
  window.addEventListener('popstate', handlePreviewHistoryChange)
  window.addEventListener('hashchange', handlePreviewHistoryChange)
})

onUnmounted(() => {
  window.removeEventListener('popstate', handlePreviewHistoryChange)
  window.removeEventListener('hashchange', handlePreviewHistoryChange)
})

// 处理全局的 vChart 主题
useInitVChartsTheme(chartEditStore)
</script>

<style lang="scss" scoped>
@include go('preview') {
  position: relative;
  height: 100vh;
  width: 100vw;
  @include background-image('background-image');
  &.fit,
  &.full {
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    .go-preview-scale {
      transform-origin: center center;
    }
  }
  &.scrollY {
    overflow-x: hidden;
    .go-preview-scale {
      transform-origin: left top;
    }
  }
  &.scrollX {
    overflow-y: hidden;
    .go-preview-scale {
      transform-origin: left top;
    }
  }
  .go-preview-entity {
    overflow: hidden;
  }
  .go-preview-page {
    transform-origin: center center;
  }
}

.go-preview-page-fade-enter-active,
.go-preview-page-fade-leave-active,
.go-preview-page-slide-left-enter-active,
.go-preview-page-slide-left-leave-active,
.go-preview-page-slide-right-enter-active,
.go-preview-page-slide-right-leave-active,
.go-preview-page-zoom-enter-active,
.go-preview-page-zoom-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.go-preview-page-fade-enter-from,
.go-preview-page-fade-leave-to {
  opacity: 0;
}

.go-preview-page-slide-left-enter-from {
  opacity: 0;
  transform: translateX(32px);
}

.go-preview-page-slide-left-leave-to {
  opacity: 0;
  transform: translateX(-32px);
}

.go-preview-page-slide-right-enter-from {
  opacity: 0;
  transform: translateX(-32px);
}

.go-preview-page-slide-right-leave-to {
  opacity: 0;
  transform: translateX(32px);
}

.go-preview-page-zoom-enter-from,
.go-preview-page-zoom-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

.go-preview-modal-layer {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  padding: 28px;
  box-sizing: border-box;
  overflow: auto;
  backdrop-filter: blur(4px);

  &.placement-center {
    align-items: center;
    justify-content: center;
  }

  &.placement-right {
    align-items: stretch;
    justify-content: flex-end;

    .go-preview-modal {
      max-height: calc(100vh - 56px);
    }
  }

  &.placement-bottom {
    align-items: flex-end;
    justify-content: center;

    .go-preview-modal {
      max-width: calc(100vw - 56px);
    }
  }

  &.no-mask {
    pointer-events: none;

    .go-preview-modal {
      pointer-events: auto;
    }
  }

  &.theme-screen {
    .go-preview-modal {
      background:
        linear-gradient(135deg, rgba(31, 46, 82, 0.42), rgba(7, 12, 24, 0.96) 42%, rgba(8, 17, 34, 0.98)),
        rgba(8, 13, 22, 0.96);
      border: 1px solid rgba(125, 211, 252, 0.34);
      box-shadow:
        0 0 0 1px rgba(15, 23, 42, 0.72) inset,
        0 0 36px rgba(56, 189, 248, 0.16),
        0 26px 90px rgba(0, 0, 0, 0.58);
    }
  }

  &.theme-glass {
    .go-preview-modal {
      background:
        linear-gradient(145deg, rgba(15, 23, 42, 0.58), rgba(2, 6, 23, 0.38)),
        rgba(15, 23, 42, 0.46);
      border: 1px solid rgba(226, 232, 240, 0.28);
      box-shadow:
        0 22px 80px rgba(0, 0, 0, 0.5),
        0 0 44px rgba(59, 130, 246, 0.14);
      backdrop-filter: blur(18px) saturate(132%);
    }
  }

  &.theme-flat {
    .go-preview-modal {
      background: transparent;
      border-color: transparent;
      box-shadow: none;
    }
  }

  &.theme-aurora {
    .go-preview-modal {
      background:
        linear-gradient(145deg, rgba(10, 4, 32, 0.97), rgba(4, 14, 28, 0.98)),
        rgba(4, 8, 24, 0.98);
      border: 1px solid rgba(167, 139, 250, 0.46);
      box-shadow:
        0 0 0 1px rgba(52, 211, 153, 0.12) inset,
        0 0 34px rgba(167, 139, 250, 0.22),
        0 0 64px rgba(52, 211, 153, 0.1),
        0 28px 96px rgba(0, 0, 0, 0.64);
    }
  }

  &.theme-cyber {
    .go-preview-modal {
      background:
        linear-gradient(145deg, rgba(18, 10, 2, 0.98), rgba(8, 5, 2, 0.99)),
        rgba(8, 5, 2, 0.99);
      border: 1px solid rgba(251, 146, 60, 0.52);
      box-shadow:
        0 0 0 1px rgba(251, 146, 60, 0.1) inset,
        0 0 32px rgba(251, 146, 60, 0.2),
        0 28px 96px rgba(0, 0, 0, 0.72);
    }
  }

  &.theme-gold {
    .go-preview-modal {
      background:
        linear-gradient(145deg, rgba(20, 13, 3, 0.98), rgba(10, 7, 2, 0.99)),
        rgba(10, 7, 2, 0.99);
      border: 1px solid rgba(202, 138, 4, 0.6);
      box-shadow:
        0 0 0 1px rgba(234, 179, 8, 0.12) inset,
        0 0 0 3px rgba(120, 80, 0, 0.16),
        0 28px 90px rgba(0, 0, 0, 0.68);
    }
  }

  &.theme-fog {
    .go-preview-modal {
      background:
        linear-gradient(145deg, rgba(22, 18, 14, 0.64), rgba(14, 11, 9, 0.74)),
        rgba(16, 12, 10, 0.68);
      border: 1px solid rgba(214, 200, 186, 0.2);
      box-shadow:
        0 24px 80px rgba(0, 0, 0, 0.48);
      backdrop-filter: blur(24px) saturate(120%) sepia(18%);
    }
  }

  &.theme-neon {
    .go-preview-modal {
      background:
        linear-gradient(145deg, rgba(10, 15, 32, 0.96), rgba(5, 8, 18, 0.98)),
        rgba(2, 6, 23, 0.98);
      border: 1px solid rgba(34, 211, 238, 0.48);
      box-shadow:
        0 0 0 1px rgba(168, 85, 247, 0.18) inset,
        0 0 34px rgba(34, 211, 238, 0.22),
        0 0 68px rgba(168, 85, 247, 0.16),
        0 28px 96px rgba(0, 0, 0, 0.62);
    }
  }

  &.theme-panel {
    .go-preview-modal {
      background:
        linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(7, 12, 24, 0.98)),
        rgba(15, 23, 42, 0.98);
      border: 1px solid rgba(71, 85, 105, 0.72);
      box-shadow:
        0 1px 0 rgba(255, 255, 255, 0.08) inset,
        0 24px 80px rgba(0, 0, 0, 0.54);
    }
  }

  &.theme-light {
    .go-preview-modal {
      background: rgba(248, 250, 252, 0.98);
      border: 1px solid rgba(148, 163, 184, 0.38);
      box-shadow: 0 24px 80px rgba(15, 23, 42, 0.34);
      color: #0f172a;

      .go-preview-modal-titlebar,
      .go-preview-modal-footer {
        color: #0f172a;
        background: rgba(241, 245, 249, 0.92);
        border-color: rgba(148, 163, 184, 0.28);
      }

      .go-preview-modal-close {
        color: rgba(15, 23, 42, 0.72);
        background: rgba(255, 255, 255, 0.92);
        border-color: rgba(148, 163, 184, 0.34);
      }
    }
  }
}

.go-preview-modal {
  position: relative;
  overflow: hidden;
  background: rgba(8, 13, 22, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.48);
  transform-origin: center center;
  transition: box-shadow 0.22s ease, border-color 0.22s ease, background 0.22s ease;
}

.go-preview-modal-canvas {
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

.go-preview-modal-titlebar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 2;
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 54px 0 18px;
  box-sizing: border-box;
  color: rgba(241, 245, 249, 0.96);
  font-size: 15px;
  font-weight: 650;
  letter-spacing: 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(90deg, rgba(125, 211, 252, 0.12), transparent 52%),
    rgba(2, 6, 23, 0.32);

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.go-preview-modal-footer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
  box-sizing: border-box;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(90deg, transparent, rgba(125, 211, 252, 0.08)),
    rgba(2, 6, 23, 0.3);
}

.go-preview-modal-action {
  min-width: 76px;
  height: 32px;
  padding: 0 14px;
  border-radius: 7px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  cursor: pointer;
  font-size: 13px;
  line-height: 30px;
  letter-spacing: 0;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease, color 0.18s ease;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.68;
    transform: none;
  }

  &.secondary {
    color: rgba(226, 232, 240, 0.82);
    background: rgba(15, 23, 42, 0.52);
  }

  &.primary {
    color: #fff;
    border-color: rgba(56, 189, 248, 0.5);
    background:
      linear-gradient(135deg, rgba(14, 165, 233, 0.9), rgba(37, 99, 235, 0.82));
    box-shadow: 0 8px 22px rgba(37, 99, 235, 0.24);
  }

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    border-color: rgba(125, 211, 252, 0.64);
  }

  &:focus-visible {
    outline: 2px solid rgba(125, 211, 252, 0.72);
    outline-offset: 2px;
  }
}

.go-preview-modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid rgba(226, 232, 240, 0.24);
  border-radius: 50%;
  cursor: pointer;
  color: rgba(226, 232, 240, 0.86);
  font-size: 22px;
  line-height: 28px;
  background: rgba(2, 6, 23, 0.76);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.34);
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;

  &:hover {
    color: #ffffff;
    border-color: rgba(125, 211, 252, 0.62);
    background: rgba(15, 23, 42, 0.92);
    transform: translateY(-1px);
  }

  &:focus-visible {
    outline: 2px solid rgba(125, 211, 252, 0.72);
    outline-offset: 2px;
  }
}

.go-preview-modal-zoom-enter-active,
.go-preview-modal-zoom-leave-active,
.go-preview-modal-slideRight-enter-active,
.go-preview-modal-slideRight-leave-active,
.go-preview-modal-slideUp-enter-active,
.go-preview-modal-slideUp-leave-active,
.go-preview-modal-fade-enter-active,
.go-preview-modal-fade-leave-active {
  transition: opacity 0.24s ease;

  .go-preview-modal {
    transition: opacity 0.24s ease, transform 0.24s ease;
  }
}

.go-preview-modal-zoom-enter-from,
.go-preview-modal-zoom-leave-to,
.go-preview-modal-slideRight-enter-from,
.go-preview-modal-slideRight-leave-to,
.go-preview-modal-slideUp-enter-from,
.go-preview-modal-slideUp-leave-to,
.go-preview-modal-fade-enter-from,
.go-preview-modal-fade-leave-to {
  opacity: 0;
}

.go-preview-modal-zoom-enter-from .go-preview-modal,
.go-preview-modal-zoom-leave-to .go-preview-modal {
  transform: scale(0.94);
}

.go-preview-modal-slideRight-enter-from .go-preview-modal,
.go-preview-modal-slideRight-leave-to .go-preview-modal {
  transform: translateX(42px);
}

.go-preview-modal-slideUp-enter-from .go-preview-modal,
.go-preview-modal-slideUp-leave-to .go-preview-modal {
  transform: translateY(42px);
}

@media (prefers-reduced-motion: reduce) {
  .go-preview-modal-zoom-enter-active,
  .go-preview-modal-zoom-leave-active,
  .go-preview-modal-slideRight-enter-active,
  .go-preview-modal-slideRight-leave-active,
  .go-preview-modal-slideUp-enter-active,
  .go-preview-modal-slideUp-leave-active,
  .go-preview-modal-fade-enter-active,
  .go-preview-modal-fade-leave-active {
    transition: none;

    .go-preview-modal {
      transition: none;
    }
  }
}
</style>
