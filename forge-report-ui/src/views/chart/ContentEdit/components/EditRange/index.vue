<template>
  <div
    class="go-edit-range go-transition"
    :class="editRangeClass"
    :style="rangeStyle"
    @mousedown="mousedownBoxSelect($event, undefined)"
  >
    <div
      v-if="showModalTitlePreview"
      class="go-edit-modal-titlebar"
      :style="modalTitlebarStyle"
    >
      <span>{{ modalTitle }}</span>
    </div>
    <div
      v-if="showModalClosePreview"
      class="go-edit-modal-close"
      :style="modalCloseStyle"
    >
      ×
    </div>
    <div
      v-if="showModalFooterPreview"
      class="go-edit-modal-footer"
      :style="modalFooterStyle"
    >
      <span v-if="modalConfig.showCancel !== false" class="modal-footer-btn secondary">
        {{ modalConfig.cancelText || $ui("取消") }}
      </span>
      <span v-if="modalConfig.showConfirm !== false" class="modal-footer-btn primary">
        {{ modalConfig.confirmText || $ui("确认") }}
      </span>
    </div>
    <slot></slot>
    <!-- 水印 -->
    <edit-watermark></edit-watermark>
    <!-- 拖拽时的辅助线 -->
    <edit-align-line></edit-align-line>
    <!-- 框选时的样式框 -->
    <edit-select></edit-select>
    <!-- 拖拽时的遮罩 -->
    <div class="go-edit-range-model" :style="rangeModelStyle"></div>
  </div>
</template>

<script setup lang="ts">
import { toRefs, computed } from 'vue'
import { useSizeStyle } from '../../hooks/useStyle.hook'
import { canvasModelIndex } from '@/settings/designSetting'
import { mousedownBoxSelect } from '../../hooks/useDrag.hook'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { createDefaultModalConfig } from '@/utils/reportPages'
import { EditAlignLine } from '../EditAlignLine'
import { EditWatermark } from '../EditWatermark'
import { EditSelect } from '../EditSelect'

const chartEditStore = useChartEditStore()

const { getEditCanvasConfig, getEditCanvas } = toRefs(chartEditStore)

const activePage = computed(() => {
  return chartEditStore.getProjectPages.find(page => page.id === chartEditStore.getActivePageId)
})

const isModalPage = computed(() => activePage.value?.pageType === 'modal')

const modalConfig = computed(() => ({
  ...createDefaultModalConfig(),
  ...(activePage.value?.modalConfig || {})
}))

const getComponentBottom = (component: any): number => {
  const attr = component?.attr || {}
  const selfBottom = Number(attr.y || 0) + Number(attr.h || 0)
  const groupBottom = Array.isArray(component?.groupList)
    ? component.groupList.reduce((max: number, item: any) => Math.max(max, getComponentBottom(item)), 0)
    : 0
  return Math.max(selfBottom, groupBottom)
}

const modalAutoHeight = computed(() => {
  const bottom = chartEditStore.getComponentList.reduce((max: number, item: any) => {
    return Math.max(max, getComponentBottom(item))
  }, 0)
  const edgePadding = modalConfig.value.showFooter === true ? 74 : 18
  return Math.max(modalConfig.value.height || getEditCanvasConfig.value.height, bottom + edgePadding, 120)
})

const size = computed(() => {
  return {
    w: getEditCanvasConfig.value.width,
    h: isModalPage.value && modalConfig.value.heightMode === 'auto'
      ? modalAutoHeight.value
      : getEditCanvasConfig.value.height
  }
})

const modalTitle = computed(() => {
  return modalConfig.value.title?.trim() || activePage.value?.name || '弹窗'
})

const showModalTitlePreview = computed(() => {
  return isModalPage.value && modalConfig.value.showTitle !== false
})

const showModalClosePreview = computed(() => {
  return isModalPage.value && modalConfig.value.showClose !== false
})

const showModalFooterPreview = computed(() => {
  return isModalPage.value && modalConfig.value.showFooter === true
})

const editRangeClass = computed(() => {
  if (!isModalPage.value) return ''
  return [
    'is-modal-preview',
    `modal-theme-${modalConfig.value.theme || 'screen'}`
  ]
})

const rangeStyle = computed(() => {
  // 缩放
  const scale = {
    transform: `scale(${getEditCanvas.value.scale})`
  }
  // @ts-ignore
  return {
    ...useSizeStyle(size.value),
    ...scale,
    ...(isModalPage.value ? {
      borderRadius: `${modalConfig.value.borderRadius ?? 8}px`
    } : {})
  }
})

const modalTitlebarStyle = computed(() => {
  const align = modalConfig.value.titleAlign || 'left'
  const justifyContentMap = {
    left: 'flex-start',
    center: 'center',
    right: 'flex-end'
  }
  return {
    height: `${modalConfig.value.titleHeight || 44}px`,
    color: modalConfig.value.titleColor,
    background: modalConfig.value.titleBackground,
    fontSize: `${modalConfig.value.titleFontSize || 15}px`,
    fontWeight: modalConfig.value.titleFontWeight || 650,
    justifyContent: justifyContentMap[align]
  }
})

const modalCloseStyle = computed(() => {
  const titleHeight = modalConfig.value.titleHeight || 44
  return {
    top: `${Math.max(6, Math.round((titleHeight - 32) / 2))}px`
  }
})

const modalFooterStyle = computed(() => {
  return {
    height: `${Math.max(40, Math.min(64, Math.round(56 / Math.max(getEditCanvas.value.scale || 1, 0.4))))}px`
  }
})

// 模态层
const rangeModelStyle = computed(() => {
  const dragStyle = getEditCanvas.value.isCreate && { 'z-index': 99999 }
  // @ts-ignore
  return { ...useSizeStyle(size.value), ...dragStyle }
})
</script>

<style lang="scss" scoped>
@include go(edit-range) {
  position: relative;
  transform-origin: left top;
  background-size: cover;
  overflow: hidden;
  border: 1px solid rgba(var(--app-theme-rgb), 0.22);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.42),
    0 0 0 1px rgba(255, 255, 255, 0.025),
    0 0 28px rgba(var(--app-theme-rgb), 0.08);
  @include fetch-bg-color('background-color2');

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    z-index: 0;
    background:
      linear-gradient(90deg, rgba(var(--app-theme-rgb), 0.12), transparent 18%, transparent 82%, rgba(var(--app-theme-rgb), 0.12)),
      linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 16%);
    mix-blend-mode: screen;
  }

  &.is-modal-preview {
    overflow: hidden;
    border-color: rgba(125, 211, 252, 0.34);
    box-shadow:
      0 0 0 1px rgba(15, 23, 42, 0.72) inset,
      0 0 36px rgba(56, 189, 248, 0.16),
      0 26px 90px rgba(0, 0, 0, 0.58);

    &.modal-theme-glass {
      border-color: rgba(226, 232, 240, 0.28);
      box-shadow:
        0 22px 80px rgba(0, 0, 0, 0.5),
        0 0 44px rgba(59, 130, 246, 0.14);
    }

    &.modal-theme-neon {
      border-color: rgba(34, 211, 238, 0.48);
      box-shadow:
        0 0 0 1px rgba(168, 85, 247, 0.18) inset,
        0 0 34px rgba(34, 211, 238, 0.22),
        0 0 68px rgba(168, 85, 247, 0.16),
        0 28px 96px rgba(0, 0, 0, 0.62);
    }

    &.modal-theme-panel {
      border-color: rgba(71, 85, 105, 0.72);
      box-shadow:
        0 1px 0 rgba(255, 255, 255, 0.08) inset,
        0 24px 80px rgba(0, 0, 0, 0.54);
    }

    &.modal-theme-light {
      border-color: rgba(148, 163, 184, 0.38);
      box-shadow: 0 24px 80px rgba(15, 23, 42, 0.34);

      .go-edit-modal-titlebar,
      .go-edit-modal-footer {
        color: #0f172a;
        background: rgba(241, 245, 249, 0.92);
        border-color: rgba(148, 163, 184, 0.28);
      }

      .go-edit-modal-close {
        color: rgba(15, 23, 42, 0.72);
        background: rgba(255, 255, 255, 0.92);
        border-color: rgba(148, 163, 184, 0.34);
      }
    }

    &.modal-theme-flat {
      border-color: transparent;
      box-shadow: none;
    }

    &.modal-theme-aurora {
      border-color: rgba(167, 139, 250, 0.46);
      box-shadow:
        0 0 0 1px rgba(52, 211, 153, 0.12) inset,
        0 0 34px rgba(167, 139, 250, 0.22),
        0 0 64px rgba(52, 211, 153, 0.1),
        0 28px 96px rgba(0, 0, 0, 0.64);
    }

    &.modal-theme-cyber {
      border-color: rgba(251, 146, 60, 0.52);
      box-shadow:
        0 0 0 1px rgba(251, 146, 60, 0.1) inset,
        0 0 32px rgba(251, 146, 60, 0.2),
        0 28px 96px rgba(0, 0, 0, 0.72);

      .go-edit-modal-titlebar,
      .go-edit-modal-footer {
        border-color: rgba(251, 146, 60, 0.28);
      }
    }

    &.modal-theme-gold {
      border-color: rgba(202, 138, 4, 0.6);
      box-shadow:
        0 0 0 1px rgba(234, 179, 8, 0.12) inset,
        0 0 0 3px rgba(120, 80, 0, 0.16),
        0 28px 90px rgba(0, 0, 0, 0.68);

      .go-edit-modal-titlebar,
      .go-edit-modal-footer {
        border-color: rgba(202, 138, 4, 0.32);
      }
    }

    &.modal-theme-fog {
      border-color: rgba(214, 200, 186, 0.2);
      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.48);
    }
  }

  .go-edit-modal-titlebar {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
    z-index: 40;
    display: flex;
    align-items: center;
    padding: 0 18px;
    box-sizing: border-box;
    pointer-events: none;
    color: rgba(241, 245, 249, 0.96);
    font-size: 14px;
    font-weight: 650;
    letter-spacing: 0;
    border-radius: inherit;
    border-bottom-left-radius: 0;
    border-bottom-right-radius: 0;
    border: 1px solid rgba(125, 211, 252, 0.26);
    border-bottom-color: rgba(148, 163, 184, 0.16);
    background:
      linear-gradient(90deg, rgba(125, 211, 252, 0.12), transparent 52%),
      rgba(2, 6, 23, 0.86);
    box-shadow: 0 12px 34px rgba(0, 0, 0, 0.22);

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .go-edit-modal-close {
    position: absolute;
    right: 12px;
    z-index: 45;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;
    box-sizing: border-box;
    border: 1px solid rgba(226, 232, 240, 0.24);
    border-radius: 50%;
    color: rgba(226, 232, 240, 0.86);
    font-size: 22px;
    line-height: 1;
    background: rgba(2, 6, 23, 0.76);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.28);
  }

  .go-edit-modal-footer {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 40;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
    padding: 0 16px;
    box-sizing: border-box;
    pointer-events: none;
    border: 1px solid rgba(125, 211, 252, 0.18);
    border-top-color: rgba(148, 163, 184, 0.16);
    border-radius: inherit;
    border-top-left-radius: 0;
    border-top-right-radius: 0;
    background:
      linear-gradient(90deg, transparent, rgba(125, 211, 252, 0.08)),
      rgba(2, 6, 23, 0.84);
    box-shadow: 0 -12px 34px rgba(0, 0, 0, 0.22);

    .modal-footer-btn {
      min-width: 66px;
      height: 30px;
      padding: 0 12px;
      border-radius: 7px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;
      font-size: 12px;
      line-height: 1;
      border: 1px solid rgba(148, 163, 184, 0.22);

      &.secondary {
        color: rgba(226, 232, 240, 0.82);
        background: rgba(15, 23, 42, 0.52);
      }

      &.primary {
        color: #fff;
        border-color: rgba(56, 189, 248, 0.5);
        background:
          linear-gradient(135deg, rgba(14, 165, 233, 0.9), rgba(37, 99, 235, 0.82));
      }
    }
  }

  @include go(edit-range-model) {
    z-index: -1;
    position: absolute;
    left: 0;
    top: 0;
  }
}
</style>
