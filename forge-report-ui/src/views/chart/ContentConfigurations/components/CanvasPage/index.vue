<template>
  <div class="go-canvas-setting">
    <div class="canvas-overview">
      <div>
        <span class="overview-label">{{ $ui("画布尺寸") }}</span>
        <strong>{{ canvasConfig.width }} x {{ canvasConfig.height }}</strong>
      </div>
      <n-button size="tiny" secondary @click="fitCanvasHandle">{{ $ui("适配视图") }}</n-button>
    </div>

    <section class="canvas-section size-section">
      <div class="section-title">
        <span>SIZE</span>
        <strong>{{ $ui("基础尺寸") }}</strong>
      </div>
      <div class="size-grid">
        <label>
          <span>{{ $ui("宽度") }}</span>
          <n-input-number
            size="small"
            v-model:value="canvasConfig.width"
            :disabled="editCanvas.lockScale"
            :validator="validator"
            @update:value="changeSizeHandle"
          ></n-input-number>
        </label>
        <label>
          <span>{{ $ui("高度") }}</span>
          <n-input-number
            size="small"
            v-model:value="canvasConfig.height"
            :disabled="editCanvas.lockScale"
            :validator="validator"
            @update:value="changeSizeHandle"
          ></n-input-number>
        </label>
      </div>
    </section>

    <section v-if="isModalPage" class="canvas-section modal-section">
      <div class="section-title">
        <span>MODAL</span>
        <strong>{{ $ui("弹窗外壳") }}</strong>
      </div>
      <div class="modal-grid">
        <label class="modal-title-field">
          <span>{{ $ui("弹窗标题") }}</span>
          <n-input
            size="small"
            v-model:value="modalTitle"
            :placeholder="$ui(&quot;默认使用页面名称&quot;)"
          />
        </label>
        <label>
          <span>{{ $ui("弹窗宽度") }}</span>
          <n-input-number
            size="small"
            v-model:value="modalWidth"
            :min="120"
            :max="7680"
          />
        </label>
        <label>
          <span>{{ activeModalConfig.heightMode === 'auto' ? $ui("最小高度") : $ui("弹窗高度") }}</span>
          <n-input-number
            size="small"
            v-model:value="modalHeight"
            :min="120"
            :max="4320"
          />
        </label>
        <label>
          <span>{{ $ui("高度模式") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.heightMode"
            :options="modalHeightModeOptions"
          />
        </label>
        <label>
          <span>{{ $ui("打开位置") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.placement"
            :options="modalPlacementOptions"
          />
        </label>
        <label>
          <span>{{ $ui("外观风格") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.theme"
            :options="modalThemeOptions"
          />
        </label>
        <label>
          <span>{{ $ui("弹出动效") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.animation"
            :options="modalAnimationOptions"
          />
        </label>
        <label>
          <span>{{ $ui("标题高度") }}</span>
          <n-input-number
            size="small"
            v-model:value="activeModalConfig.titleHeight"
            :min="28"
            :max="96"
          />
        </label>
        <label>
          <span>{{ $ui("标题字号") }}</span>
          <n-input-number
            size="small"
            v-model:value="activeModalConfig.titleFontSize"
            :min="10"
            :max="40"
          />
        </label>
        <label>
          <span>{{ $ui("标题粗细") }}</span>
          <n-input-number
            size="small"
            v-model:value="activeModalConfig.titleFontWeight"
            :min="100"
            :max="900"
            :step="50"
          />
        </label>
        <label>
          <span>{{ $ui("标题位置") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.titleAlign"
            :options="modalTitleAlignOptions"
          />
        </label>
        <label>
          <span>{{ $ui("标题颜色") }}</span>
          <n-color-picker
            size="small"
            v-model:value="activeModalConfig.titleColor"
            :show-alpha="true"
          />
        </label>
        <label class="modal-title-field">
          <span>{{ $ui("标题背景（支持渐变）") }}</span>
          <n-input
            size="small"
            v-model:value="activeModalConfig.titleBackground"
            :placeholder="$ui(&quot;颜色或渐变，如 linear-gradient(90deg, #0f2, #08f)&quot;)"
            clearable
          />
        </label>
        <label>
          <span>{{ $ui("圆角") }}</span>
          <n-input-number
            size="small"
            v-model:value="activeModalConfig.borderRadius"
            :min="0"
            :max="40"
          />
        </label>
        <label>
          <span>{{ $ui("遮罩透明度") }}</span>
          <n-input-number
            size="small"
            v-model:value="activeModalConfig.maskOpacity"
            :min="0"
            :max="0.95"
            :step="0.05"
          />
        </label>
      </div>
      <div class="modal-switches">
        <n-checkbox v-model:checked="activeModalConfig.showTitle">{{ $ui("显示标题栏") }}</n-checkbox>
        <n-checkbox v-model:checked="activeModalConfig.showFooter">{{ $ui("显示底部按钮") }}</n-checkbox>
        <n-checkbox v-model:checked="activeModalConfig.showMask">{{ $ui("显示遮罩") }}</n-checkbox>
        <n-checkbox v-model:checked="activeModalConfig.maskClosable">{{ $ui("点击遮罩关闭") }}</n-checkbox>
        <n-checkbox v-model:checked="activeModalConfig.showClose">{{ $ui("显示关闭按钮") }}</n-checkbox>
      </div>
      <div v-if="activeModalConfig.showFooter" class="modal-footer-grid">
        <label>
          <span>{{ $ui("取消按钮") }}</span>
          <n-input size="small" v-model:value="activeModalConfig.cancelText" />
        </label>
        <label>
          <span>{{ $ui("确认按钮") }}</span>
          <n-input size="small" v-model:value="activeModalConfig.confirmText" />
        </label>
        <label>
          <span>{{ $ui("取消事件") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.cancelAction.type"
            :options="modalButtonActionOptions"
            @update:value="() => handleModalButtonActionTypeChange(activeModalConfig.cancelAction)"
          />
        </label>
        <label>
          <span>{{ $ui("确认事件") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.confirmAction.type"
            :options="modalButtonActionOptions"
            @update:value="() => handleModalButtonActionTypeChange(activeModalConfig.confirmAction)"
          />
        </label>
        <label v-if="isPageAction(activeModalConfig.cancelAction?.type)">
          <span>{{ $ui("取消目标页") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.cancelAction.targetPageId"
            :options="pageTargetOptions"
          />
        </label>
        <label v-if="isPageAction(activeModalConfig.confirmAction?.type)">
          <span>{{ $ui("确认目标页") }}</span>
          <n-select
            size="small"
            v-model:value="activeModalConfig.confirmAction.targetPageId"
            :options="pageTargetOptions"
          />
        </label>
        <div v-if="isSubmitAction(activeModalConfig.cancelAction?.type)" class="modal-action-config">
          <div class="modal-action-config-head">
            <span>{{ $ui("取消提交接口") }}</span>
            <n-button size="tiny" secondary @click="openActionRequestConfig('cancel')">{{ $ui("配置请求") }}</n-button>
          </div>
          <label>
            <span>{{ $ui("成功后") }}</span>
            <n-select
              size="small"
              v-model:value="activeModalConfig.cancelAction.submitSuccessAction"
              :options="modalSubmitSuccessActionOptions"
            />
          </label>
          <label v-if="isPageAction(activeModalConfig.cancelAction.submitSuccessAction)">
            <span>{{ $ui("成功目标页") }}</span>
            <n-select
              size="small"
              v-model:value="activeModalConfig.cancelAction.submitSuccessTargetPageId"
              :options="pageTargetOptions"
            />
          </label>
        </div>
        <div v-if="isSubmitAction(activeModalConfig.confirmAction?.type)" class="modal-action-config">
          <div class="modal-action-config-head">
            <span>{{ $ui("确认提交接口") }}</span>
            <n-button size="tiny" secondary @click="openActionRequestConfig('confirm')">{{ $ui("配置请求") }}</n-button>
          </div>
          <label>
            <span>{{ $ui("成功后") }}</span>
            <n-select
              size="small"
              v-model:value="activeModalConfig.confirmAction.submitSuccessAction"
              :options="modalSubmitSuccessActionOptions"
            />
          </label>
          <label v-if="isPageAction(activeModalConfig.confirmAction.submitSuccessAction)">
            <span>{{ $ui("成功目标页") }}</span>
            <n-select
              size="small"
              v-model:value="activeModalConfig.confirmAction.submitSuccessTargetPageId"
              :options="pageTargetOptions"
            />
          </label>
        </div>
        <div class="modal-footer-switches">
          <n-checkbox v-model:checked="activeModalConfig.showCancel">{{ $ui("显示取消") }}</n-checkbox>
          <n-checkbox v-model:checked="activeModalConfig.showConfirm">{{ $ui("显示确认") }}</n-checkbox>
        </div>
      </div>
    </section>

    <section class="canvas-section background-section">
      <div class="section-title">
        <span>BACKGROUND</span>
        <strong>{{ $ui("背景设置") }}</strong>
      </div>
      <div class="background-layout">
        <div class="upload-box">
          <n-upload
            v-model:file-list="uploadFileListRef"
            :show-file-list="false"
            :customRequest="customRequest"
            :onBeforeUpload="beforeUploadHandle"
          >
            <n-upload-dragger>
              <fg-auth-image
                v-if="canvasConfig.backgroundImage"
                :src="canvasConfig.backgroundImage"
                class="upload-show"
                img-class="upload-show-image"
                alt="背景"
                :lazy="false"
              />
              <div class="upload-img" v-show="!canvasConfig.backgroundImage">
                <img src="@/assets/images/canvas/noImage.png" />
                <n-text class="upload-desc" depth="3">
                  png / jpg / gif，{{ backgroundImageSize }}{{ $ui("M 内") }}
                </n-text>
              </div>
            </n-upload-dragger>
          </n-upload>
        </div>
        <div class="background-controls">
          <label>
            <span>{{ $ui("背景颜色") }}</span>
            <div class="picker-height">
              <n-color-picker
                v-if="!switchSelectColorLoading"
                size="small"
                v-model:value="canvasConfig.background"
                :showPreview="true"
                :swatches="swatchesColors"
              ></n-color-picker>
            </div>
          </label>
          <label>
            <span>{{ $ui("应用类型") }}</span>
            <n-select
              size="small"
              v-model:value="selectColorValue"
              :disabled="!canvasConfig.backgroundImage"
              :options="selectColorOptions"
              @update:value="selectColorValueHandle"
            />
          </label>
          <div class="background-actions">
            <material-asset-selector
              :value="canvasConfig.backgroundImage"
              button-text="从素材库选择"
              :show-preview="false"
              @confirm="handleBackgroundMaterialSelect"
              @clear="clearImage"
            />
            <div class="background-button-row">
              <n-button class="clear-btn" size="small" :disabled="!canvasConfig.backgroundImage" @click="clearImage">
                {{ $ui("清除背景") }}
              </n-button>
              <n-button class="clear-btn" size="small" :disabled="!canvasConfig.background" @click="clearColor">
                {{ $ui("清除颜色") }}
              </n-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="canvas-section preview-section">
      <div class="section-title">
        <span>PREVIEW</span>
        <strong>{{ $ui("适配方式") }}</strong>
      </div>
      <div class="preview-type-grid">
        <button
          v-for="item in previewTypeList"
          :key="item.key"
          class="preview-type-card"
          :class="{ active: canvasConfig.previewScaleType === item.key }"
          type="button"
          @click="selectPreviewType(item.key)"
        >
          <n-icon size="18">
            <component :is="item.icon"></component>
          </n-icon>
          <span>{{ $ui(item.title) }}</span>
        </button>
      </div>
    </section>

    <!-- 滤镜 -->
    <section class="canvas-section filter-section">
      <div class="section-title">
        <span>FILTER</span>
        <strong>{{ $ui("滤镜效果") }}</strong>
      </div>
      <styles-setting :isCanvas="true" :chartStyles="canvasConfig"></styles-setting>
    </section>

    <!-- 主题选择和全局配置 -->
    <n-tabs class="tabs-box" size="small" type="segment">
      <n-tab-pane
        v-for="item in globalTabList"
        :key="item.key"
        :name="item.key"
        size="small"
        display-directive="show:lazy"
      >
        <template #tab>
          <n-space>
            <span>{{ $ui(item.title) }}</span>
            <n-icon size="16" class="icon-position">
              <component :is="item.icon"></component>
            </n-icon>
          </n-space>
        </template>
        <component :is="item.render"></component>
      </n-tab-pane>
    </n-tabs>
  </div>
  <chart-data-request
    v-if="showModalRequestConfig && modalRequestTarget"
    v-model:model-show="showModalRequestConfig"
    :target-data="modalRequestTarget"
    save-btn-text="保存请求配置"
  />
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { ref, nextTick, watch, computed } from 'vue'
import cloneDeep from 'lodash/cloneDeep'
import { backgroundImageSize } from '@/settings/designSetting'
import { swatchesColors } from '@/settings/chartThemes/index'
import { FileTypeEnum } from '@/enums/fileTypeEnum'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { EditCanvasConfigEnum } from '@/store/modules/chartEditStore/chartEditStore.d'
import { StylesSetting } from '@/components/Pages/ChartItemSetting'
import { UploadCustomRequestOptions } from 'naive-ui'
import { fileToUrl, loadAsyncComponent } from '@/utils'
import { PreviewScaleEnum } from '@/enums/styleEnum'
import { icon } from '@/plugins'
import MaterialAssetSelector from '@/components/Pages/MaterialAssetSelector/index.vue'
import FgAuthImage from '@/components/FgAuthImage/index.vue'
import { createDefaultModalConfig, createDefaultModalSubmitRequestConfig } from '@/utils/reportPages'
import { ChartDataRequest } from '../ChartData/components/ChartDataRequest'
import { RequestDataTypeEnum, RequestHttpEnum } from '@/enums/httpEnum'
import type { ReportModalButtonAction } from '@/store/modules/chartEditStore/chartEditStore.d'

const { ColorPaletteIcon } = icon.ionicons5
const { ScaleIcon, FitToScreenIcon, FitToHeightIcon, FitToWidthIcon } = icon.carbon

const chartEditStore = useChartEditStore()
const canvasConfig = chartEditStore.getEditCanvasConfig
const editCanvas = chartEditStore.getEditCanvas

const uploadFileListRef = ref()
const switchSelectColorLoading = ref(false)
const selectColorValue = ref(0)

const ChartThemeColor = loadAsyncComponent(() => import('./components/ChartThemeColor/index.vue'))
const VChartThemeColor = loadAsyncComponent(() => import('./components/VChartThemeColor/index.vue'))

// 默认应用类型
const selectColorOptions = [
  {
    get label() { return uiText("应用颜色") },
    value: 0
  },
  {
    get label() { return uiText("应用背景") },
    value: 1
  }
]

const globalTabList = [
  {
    key: 'ChartTheme',
    title: '默认主题',
    icon: ColorPaletteIcon,
    render: ChartThemeColor
  },
  {
    key: 'VChartTheme',
    title: 'VChart主题',
    icon: ColorPaletteIcon,
    render: VChartThemeColor
  },
]

const previewTypeList = [
  {
    key: PreviewScaleEnum.FIT,
    title: '自适应',
    icon: ScaleIcon,
    desc: '自适应比例展示，页面会有留白'
  },
  {
    key: PreviewScaleEnum.SCROLL_Y,
    title: 'Y轴滚动',
    icon: FitToWidthIcon,
    desc: 'X轴铺满，Y轴自适应滚动'
  },
  {
    key: PreviewScaleEnum.SCROLL_X,
    title: 'X轴滚动',
    icon: FitToHeightIcon,
    desc: 'Y轴铺满，X轴自适应滚动'
  },
  {
    key: PreviewScaleEnum.FULL,
    title: '铺满',
    icon: FitToScreenIcon,
    desc: '强行拉伸画面，填充所有视图'
  }
]

const modalPlacementOptions = [
  { get label() { return uiText("居中") }, value: 'center' },
  { get label() { return uiText("右侧抽屉") }, value: 'right' },
  { get label() { return uiText("底部弹层") }, value: 'bottom' }
]

const modalHeightModeOptions = [
  { get label() { return uiText("固定高度") }, value: 'fixed' },
  { get label() { return uiText("自适应高度") }, value: 'auto' }
]

const modalThemeOptions = [
  { get label() { return uiText("大屏浮层") }, value: 'screen' },
  { get label() { return uiText("玻璃质感") }, value: 'glass' },
  { get label() { return uiText("霓虹描边") }, value: 'neon' },
  { get label() { return uiText("极光渐变") }, value: 'aurora' },
  { get label() { return uiText("赛博科技") }, value: 'cyber' },
  { get label() { return uiText("描金镶边") }, value: 'gold' },
  { get label() { return uiText("深色面板") }, value: 'panel' },
  { get label() { return uiText("晨雾质感") }, value: 'fog' },
  { get label() { return uiText("浅色卡片") }, value: 'light' },
  { get label() { return uiText("无边框") }, value: 'flat' }
]

const modalTitleAlignOptions = [
  { get label() { return uiText("左对齐") }, value: 'left' },
  { get label() { return uiText("居中") }, value: 'center' },
  { get label() { return uiText("右对齐") }, value: 'right' }
]

const modalAnimationOptions = [
  { get label() { return uiText("缩放弹出") }, value: 'zoom' },
  { get label() { return uiText("右侧滑入") }, value: 'slideRight' },
  { get label() { return uiText("底部滑入") }, value: 'slideUp' },
  { get label() { return uiText("淡入") }, value: 'fade' }
]

const modalButtonActionOptions = [
  { get label() { return uiText("提交接口") }, value: 'submitRequest' },
  { get label() { return uiText("关闭弹窗") }, value: 'closeModal' },
  { get label() { return uiText("跳转页面") }, value: 'goPage' },
  { get label() { return uiText("打开弹窗") }, value: 'openModal' },
  { get label() { return uiText("无动作") }, value: 'none' }
]

const modalSubmitSuccessActionOptions = [
  { get label() { return uiText("关闭弹窗") }, value: 'closeModal' },
  { get label() { return uiText("跳转页面") }, value: 'goPage' },
  { get label() { return uiText("打开弹窗") }, value: 'openModal' },
  { get label() { return uiText("无动作") }, value: 'none' }
]

const activePage = computed(() => {
  return chartEditStore.getProjectPages.find(page => page.id === chartEditStore.getActivePageId)
})

const isModalPage = computed(() => activePage.value?.pageType === 'modal')

const pageTargetOptions = computed(() => {
  return chartEditStore.getProjectPages
    .filter(page => page.id !== activePage.value?.id)
    .map(page => ({
      label: page.pageType === 'modal' ? `${page.name}（弹窗）` : page.name,
      value: page.id
    }))
})

const isPageAction = (type?: string) => {
  return type === 'goPage' || type === 'openModal'
}

const isSubmitAction = (type?: string) => {
  return type === 'submitRequest'
}

const activeModalConfig = computed(() => {
  if (!activePage.value) return createDefaultModalConfig()
  return activePage.value.modalConfig || createDefaultModalConfig()
})

const normalizeModalButtonAction = (action?: ReportModalButtonAction): ReportModalButtonAction => {
  const nextAction = {
    type: 'closeModal',
    submitSuccessAction: 'closeModal',
    ...action
  } as ReportModalButtonAction
  if (nextAction.type === 'submitRequest' && !nextAction.requestConfig) {
    nextAction.requestConfig = createDefaultModalSubmitRequestConfig()
  } else if (
    nextAction.type === 'submitRequest' &&
    nextAction.requestConfig?.requestDataType === RequestDataTypeEnum.STATIC
  ) {
    nextAction.requestConfig = {
      ...nextAction.requestConfig,
      requestDataType: RequestDataTypeEnum.AJAX,
      requestHttpType: nextAction.requestConfig.requestHttpType || RequestHttpEnum.POST,
      requestInterval: nextAction.requestConfig.requestInterval ?? 0
    }
  }
  return nextAction
}

watch(
  () => activePage.value?.id,
  () => {
    if (activePage.value?.pageType === 'modal' && !activePage.value.modalConfig) {
      activePage.value.modalConfig = createDefaultModalConfig()
    }
    if (activePage.value?.modalConfig) {
      activePage.value.modalConfig = {
        ...createDefaultModalConfig(),
        ...activePage.value.modalConfig,
        cancelAction: {
          ...normalizeModalButtonAction(createDefaultModalConfig().cancelAction),
          ...normalizeModalButtonAction(activePage.value.modalConfig.cancelAction || {})
        },
        confirmAction: {
          ...normalizeModalButtonAction(createDefaultModalConfig().confirmAction),
          ...normalizeModalButtonAction(activePage.value.modalConfig.confirmAction || {})
        }
      }
    }
  },
  { immediate: true }
)

const handleModalButtonActionTypeChange = (action?: ReportModalButtonAction) => {
  if (!action) return
  if (action.type === 'submitRequest') {
    action.requestConfig = action.requestConfig || createDefaultModalSubmitRequestConfig()
    if (action.requestConfig.requestDataType === RequestDataTypeEnum.STATIC) {
      action.requestConfig.requestDataType = RequestDataTypeEnum.AJAX
      action.requestConfig.requestHttpType = action.requestConfig.requestHttpType || RequestHttpEnum.POST
      action.requestConfig.requestInterval = action.requestConfig.requestInterval ?? 0
    }
    action.submitSuccessAction = action.submitSuccessAction || 'closeModal'
  }
}

const editingModalActionType = ref<'cancel' | 'confirm'>('confirm')
const showModalRequestConfig = ref(false)

const editingModalAction = computed(() => {
  return editingModalActionType.value === 'cancel'
    ? activeModalConfig.value.cancelAction
    : activeModalConfig.value.confirmAction
})

const modalRequestTarget = computed(() => {
  const action = editingModalAction.value
  if (!action) return undefined
  action.requestConfig = action.requestConfig || createDefaultModalSubmitRequestConfig()
  return {
    id: `modal-${editingModalActionType.value}-request`,
    key: 'ModalButtonRequest',
    chartConfig: {
      key: 'ModalButtonRequest',
      chartKey: 'ModalButtonRequest',
      conKey: 'ModalButtonRequest',
      category: 'modal',
      categoryName: editingModalActionType.value === 'cancel' ? '弹窗取消按钮' : '弹窗确认按钮',
      package: 'modal',
      title: editingModalActionType.value === 'cancel' ? '弹窗取消按钮' : '弹窗确认按钮',
      image: ''
    },
    request: action.requestConfig
  } as any
})

const openActionRequestConfig = (type: 'cancel' | 'confirm') => {
  editingModalActionType.value = type
  const action = type === 'cancel'
    ? activeModalConfig.value.cancelAction
    : activeModalConfig.value.confirmAction
  if (action) {
    action.requestConfig = action.requestConfig || cloneDeep(createDefaultModalSubmitRequestConfig())
    if (action.requestConfig.requestDataType === RequestDataTypeEnum.STATIC) {
      action.requestConfig.requestDataType = RequestDataTypeEnum.AJAX
      action.requestConfig.requestHttpType = action.requestConfig.requestHttpType || RequestHttpEnum.POST
      action.requestConfig.requestInterval = action.requestConfig.requestInterval ?? 0
    }
  }
  showModalRequestConfig.value = true
}

const setModalSize = (field: 'width' | 'height', value?: number | null) => {
  if (!activePage.value || value === undefined || value === null) return
  activePage.value.modalConfig = {
    ...createDefaultModalConfig(),
    ...(activePage.value.modalConfig || {}),
    [field]: value
  }
  canvasConfig[field] = value
  chartEditStore.computedScale()
}

const modalTitle = computed({
  get: () => activeModalConfig.value.title || '',
  set: value => {
    activeModalConfig.value.title = value
  }
})

const modalWidth = computed({
  get: () => activeModalConfig.value.width || canvasConfig.width,
  set: value => setModalSize('width', value)
})

const modalHeight = computed({
  get: () => activeModalConfig.value.height || canvasConfig.height,
  set: value => setModalSize('height', value)
})

watch(
  () => canvasConfig.selectColor,
  newValue => {
    selectColorValue.value = newValue ? 0 : 1
  },
  {
    immediate: true
  }
)

// 画布尺寸规则
const validator = (x: number) => x > 50

// 修改尺寸
const changeSizeHandle = () => {
  if (activePage.value?.pageType === 'modal') {
    activePage.value.modalConfig = {
      ...createDefaultModalConfig(),
      ...(activePage.value.modalConfig || {}),
      width: canvasConfig.width,
      height: canvasConfig.height
    }
  }
  chartEditStore.computedScale()
}

// 上传图片前置处理
//@ts-ignore
const beforeUploadHandle = async ({ file }) => {
  uploadFileListRef.value = []
  const type = file.file.type
  const size = file.file.size

  if (size > 1024 * 1024 * backgroundImageSize) {
    window['$message'].warning(uiText("图片超出 {0}M 限制，请重新上传！", { 0: (backgroundImageSize) }))
    return false
  }
  if (type !== FileTypeEnum.PNG && type !== FileTypeEnum.JPEG && type !== FileTypeEnum.GIF) {
    window['$message'].warning(uiText("文件格式不符合，请重新上传！"))
    return false
  }
  return true
}

// 应用颜色
const selectColorValueHandle = (value: number) => {
  canvasConfig.selectColor = value == 0
}

// 清除背景
const clearImage = () => {
  chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.BACKGROUND_IMAGE, undefined)
  chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.SELECT_COLOR, true)
}

// 启用/关闭 颜色（强制更新）
const switchSelectColorHandle = () => {
  switchSelectColorLoading.value = true
  setTimeout(() => {
    switchSelectColorLoading.value = false
  })
}

// 清除颜色
const clearColor = () => {
  chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.BACKGROUND, undefined)
  if (canvasConfig.backgroundImage) {
    chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.SELECT_COLOR, false)
  }
  switchSelectColorHandle()
}

// 自定义上传操作
const customRequest = (options: UploadCustomRequestOptions) => {
  const { file } = options
  nextTick(() => {
    if (file.file) {
      const ImageUrl = fileToUrl(file.file)
      chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.BACKGROUND_IMAGE, ImageUrl)
      chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.SELECT_COLOR, false)
    } else {
      window['$message'].error(uiText("添加图片失败，请稍后重试！"))
    }
  })
}

const handleBackgroundMaterialSelect = (value: string) => {
  chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.BACKGROUND_IMAGE, value || undefined)
  chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.SELECT_COLOR, false)
}

// 选择适配方式
const selectPreviewType = (key: PreviewScaleEnum) => {
  chartEditStore.setEditCanvasConfig(EditCanvasConfigEnum.PREVIEW_SCALE_TYPE, key)
}

const fitCanvasHandle = () => {
  chartEditStore.computedScale()
}
</script>

<style lang="scss" scoped>
$uploadWidth: 326px;
$uploadHeight: 193px;

@include go(canvas-setting) {
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .canvas-overview {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    border-radius: 10px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.1);
    background:
      linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.08), transparent),
      rgba(15, 23, 42, 0.28);

    .overview-label {
      display: block;
      margin-bottom: 3px;
      font-size: 10px;
      letter-spacing: 1px;
      @include fetch-color(4);
    }

    strong {
      font-size: 17px;
      color: var(--app-theme, $--color-primary);
      font-family: 'Courier New', monospace;
      font-weight: 700;
      text-shadow: 0 0 10px rgba(var(--app-theme-rgb), 0.24);
    }
  }

  .canvas-section {
    padding: 14px;
    border-radius: 10px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.08);
    background: rgba(15, 23, 42, 0.16);

    .section-title {
      display: flex;
      flex-direction: column;
      gap: 2px;
      margin-bottom: 14px;

      span {
        font-size: 9px;
        line-height: 1;
        letter-spacing: 1px;
        @include fetch-color(4);
      }

      strong {
        font-size: 14px;
        line-height: 17px;
        color: rgba(226, 232, 240, 0.92);
        font-weight: 650;
      }
    }
  }

  .size-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;

    label {
      display: flex;
      flex-direction: column;
      gap: 5px;

      > span {
        font-size: 11px;
        @include fetch-color(3);
        padding-left: 2px;
      }
    }
  }

  .background-layout {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .upload-box {
    cursor: pointer;
    @include deep() {
      .n-upload-dragger {
        padding: 5px;
        //width: $uploadWidth;
        border-radius: 10px;
        border-color: rgba(var(--app-theme-rgb), 0.12);
        background:
          radial-gradient(circle at center, rgba(var(--app-theme-rgb), 0.06), transparent 66%),
          rgba(2, 6, 23, 0.14);
        transition: border-color 0.22s ease;

        &:hover {
          border-color: rgba(var(--app-theme-rgb), 0.24);
        }
      }
    }
    .upload-show {
      width: -webkit-fill-available;
      height: $uploadHeight;
      border-radius: 6px;
      @include deep() {
        .upload-show-image {
          width: 100%;
          height: $uploadHeight !important;
          object-fit: cover !important;
          display: block;
          border-radius: 6px;
        }
      }
    }
    .upload-img {
      display: flex;
      flex-direction: column;
      align-items: center;
      img {
        height: 140px;
        opacity: 0.6;
      }
      .upload-desc {
        padding: 10px 0;
      }
    }
  }

  .background-controls {
    display: flex;
    flex-direction: column;
    gap: 10px;

    label {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;

      > span {
        font-size: 12px;
        @include fetch-color(3);
        white-space: nowrap;
        flex-shrink: 0;
      }

      .picker-height {
        min-height: 35px;
        min-width: 120px;
        display: flex;
        align-items: center;
        justify-content: flex-end;
      }
    }

    .background-actions {
      display: flex;
      flex-direction: column;
      gap: 10px;

      @include deep() {
        .material-selector {
          .selector-actions {
            display: flex;
            gap: 8px;
          }
        }
      }

      .background-button-row {
        display: flex;
        gap: 8px;
      }

      .clear-btn {
        flex: 1;
        height: 32px;
        border-radius: 8px;
        font-size: 12px;
        border: 1px solid rgba(var(--app-theme-rgb), 0.1);
        background: rgba(15, 23, 42, 0.28);
        color: rgba(203, 213, 225, 0.7);
        transition: all 0.2s ease;

        &:hover:not(:disabled) {
          border-color: rgba(var(--app-theme-rgb), 0.24);
          color: rgba(255, 255, 255, 0.9);
        }

        &:disabled {
          opacity: 0.35;
        }
      }
    }
  }

  .modal-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;

    label {
      display: flex;
      flex-direction: column;
      gap: 6px;
      min-width: 0;

      > span {
        font-size: 11px;
        @include fetch-color(3);
      }
    }

    .modal-title-field {
      grid-column: 1 / -1;
    }
  }

  .modal-switches {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
    margin-top: 10px;
    padding: 10px;
    border-radius: 8px;
    background: rgba(15, 23, 42, 0.24);
    border: 1px solid rgba(var(--app-theme-rgb), 0.08);
  }

  .modal-footer-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    margin-top: 10px;
    padding: 10px;
    border-radius: 8px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.08);
    background: rgba(2, 6, 23, 0.18);

    label {
      display: flex;
      flex-direction: column;
      gap: 6px;
      min-width: 0;

      > span {
        font-size: 11px;
        @include fetch-color(3);
      }
    }

    .modal-footer-switches {
      grid-column: 1 / -1;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      padding-top: 2px;
    }

    .modal-action-config {
      grid-column: 1 / -1;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      padding: 10px;
      border-radius: 8px;
      border: 1px solid rgba(var(--app-theme-rgb), 0.1);
      background:
        linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.08), transparent),
        rgba(15, 23, 42, 0.22);

      .modal-action-config-head {
        grid-column: 1 / -1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;

        span {
          font-size: 12px;
          color: rgba(226, 232, 240, 0.86);
          font-weight: 650;
        }
      }
    }
  }

  .preview-type-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;

    .preview-type-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 12px 8px 10px;
      border-radius: 10px;
      border: 1px solid rgba(148, 163, 184, 0.1);
      background: rgba(15, 23, 42, 0.22);
      cursor: pointer;
      color: rgba(203, 213, 225, 0.64);
      transition: all 0.22s ease;

      span {
        font-size: 11px;
        line-height: 1;
      }

      &:hover {
        color: rgba(255, 255, 255, 0.88);
        border-color: rgba(var(--app-theme-rgb), 0.2);
        background: rgba(var(--app-theme-rgb), 0.06);
        transform: translateY(-1px);
      }

      &.active {
        color: #fff;
        border-color: rgba(var(--app-theme-rgb), 0.38);
        background:
          linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.22), rgba(var(--app-theme-rgb), 0.06)),
          rgba(15, 23, 42, 0.48);
        box-shadow: 0 6px 16px rgba(var(--app-theme-rgb), 0.1);
      }
    }
  }

  .icon-position {
    padding-top: 2px;
  }

  .tabs-box {
    margin-top: 4px;
    padding: 10px 2px 4px;
    border-radius: 10px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.08);
    background: rgba(15, 23, 42, 0.16);

    @include deep() {
      .n-tabs-nav {
        border-radius: 10px;
        padding: 2px 4px;
      }
    }
  }
}
</style>
