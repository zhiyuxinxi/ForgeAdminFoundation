<template>
  <div
    class="go-chart-edit-tools"
    :class="[settingStore.getChartToolsStatus, toolsCollapsed ? 'isMini' : 'unMini', { pinned: isPinnedOpen }]"
    @click="toolsCollapsed && openAsideTools"
    @mouseenter="toolsMouseoverHandle"
    @mouseleave="toolsMouseoutHandle"
  >
    <!-- PawIcon -->
    <n-icon
      v-show="settingStore.getChartToolsStatus === ToolsStatusEnum.ASIDE && isMiniComputed"
      class="asideLogo"
      size="22"
    >
      <PawIcon></PawIcon>
    </n-icon>

    <n-tooltip v-if="isAside" trigger="hover" placement="left">
      <template #trigger>
        <n-button class="aside-toggle" quaternary circle size="tiny" @click.stop="toggleAsidePinned">
          <template #icon>
            <n-icon size="14">
              <component :is="toolsCollapsed ? ChevronBackOutlineIcon : ChevronForwardIcon"></component>
            </n-icon>
          </template>
        </n-button>
      </template>
      {{ toolsCollapsed ? $ui("展开工具栏") : $ui("收起工具栏") }}
    </n-tooltip>

    <n-tooltip
      v-for="(item, index) in btnListComputed"
      :key="item.key"
      :disabled="!isAside || (isHide && asideTootipDis)"
      trigger="hover"
      placement="left"
    >
      <template #trigger>
        <div class="btn-item">
          <n-button v-if="item.type === TypeEnum.BUTTON" :circle="isAside" secondary @click="item.handle">
            <template #icon>
              <n-icon size="18" v-if="isAside">
                <component :is="item.icon"></component>
              </n-icon>
              <component v-else :is="item.icon"></component>
            </template>
            <n-text depth="3" v-show="!isAside">{{ $ui(item.name) }}</n-text>
          </n-button>
          <!-- 下载 -->
          <n-upload
            v-else-if="item.type === TypeEnum.IMPORTUPLOAD"
            v-model:file-list="importUploadFileListRef"
            :show-file-list="false"
            :customRequest="importCustomRequest"
            @before-upload="importBeforeUpload"
          >
            <n-button :circle="isAside" secondary>
              <template #icon>
                <n-icon size="18" v-if="isAside">
                  <component :is="item.icon"></component>
                </n-icon>
                <component v-else :is="item.icon"></component>
              </template>
              <n-text depth="3" v-show="!isAside">{{ $ui(item.name) }}</n-text>
            </n-button>
          </n-upload>
        </div>
      </template>
      <!-- 提示 -->
      <span>{{ $ui(item.name) }}</span>
    </n-tooltip>
  </div>
  <!-- 系统设置 model -->
  <fg-system-set v-model:modelShow="globalSettingModel"></fg-system-set>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { ref, computed } from 'vue'
import { useSettingStore } from '@/store/modules/settingStore/settingStore'
import { ToolsStatusEnum } from '@/store/modules/settingStore/settingStore.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { EditCanvasTypeEnum } from '@/store/modules/chartEditStore/chartEditStore.d'
import {
  fetchRouteParamsLocation,
  fetchPathByName,
  routerTurnByPath,
  setSessionStorage,
  getSessionStorage
} from '@/utils'
import { EditEnum } from '@/enums/pageEnum'
import { StorageEnum } from '@/enums/storageEnum'
import { FgSystemSet } from '@/components/FgSystemSet/index'
import { exportHandle } from './utils'
import { useFile } from './hooks/useFile.hooks'
import { useSyncUpdate } from './hooks/useSyncUpdate.hook'
import { BtnListType, TypeEnum } from './index.d'
import { icon } from '@/plugins'

const {
  DownloadIcon,
  ShareIcon,
  PawIcon,
  SettingsSharpIcon,
  CreateIcon,
  ChevronBackOutlineIcon,
  ChevronForwardIcon
} = icon.ionicons5
const settingStore = useSettingStore()
const chartEditStore = useChartEditStore()
// 初始化编辑 JSON 模块
useSyncUpdate()

// 鼠标悬停定时器
let mouseTime: any = null
// 系统设置 model
const globalSettingModel = ref(false)
// 最小化
const isMini = ref<boolean>(true)
const isPinnedOpen = ref<boolean>(false)
// 控制 tootip 提示时机
const asideTootipDis = ref(true)
// 文件上传
const { importUploadFileListRef, importCustomRequest, importBeforeUpload } = useFile()

// 是否是侧边栏
const isAside = computed(() => settingStore.getChartToolsStatus === ToolsStatusEnum.ASIDE)

// 是否隐藏（悬浮展示）
const isHide = computed(() => settingStore.getChartToolsStatusHide)

// 是否展示最小化（与全局配置相关）
const isMiniComputed = computed(() => isMini.value && isHide.value)
const toolsCollapsed = computed(() => {
  if (isAside.value) return isMini.value
  return isMiniComputed.value
})

// 页面渲染配置
const btnListComputed = computed(() => {
  if (!isAside.value) return btnList
  const reverseArr: BtnListType[] = []
  btnList.map(item => {
    reverseArr.unshift(item)
  })
  return reverseArr
})

// 鼠标移入
const toolsMouseoverHandle = () => {
  if (isPinnedOpen.value) return
  mouseTime = setTimeout(() => {
    if (isMini.value) {
      isMini.value = false
      asideTootipDis.value = true
    }
  }, 200)
  setTimeout(() => {
    asideTootipDis.value = false
  }, 400)
}

// 鼠标移出
const toolsMouseoutHandle = () => {
  clearTimeout(mouseTime)
  if (isPinnedOpen.value) return
  if (!isMini.value) {
    isMini.value = true
  }
}

const openAsideTools = () => {
  if (!isAside.value) return
  isMini.value = false
}

const toggleAsidePinned = () => {
  if (!isAside.value) return
  if (isMini.value) {
    isMini.value = false
    isPinnedOpen.value = true
    asideTootipDis.value = false
    return
  }
  isMini.value = true
  isPinnedOpen.value = false
  asideTootipDis.value = true
}

// 编辑处理
const editHandle = () => {
  window['$message'].warning(uiText("请通过顶部【同步内容】按钮同步最新数据！"))
  chartEditStore.setEditCanvas(EditCanvasTypeEnum.IS_CODE_EDIT, true)
  setTimeout(() => {
    // 获取id路径
    const path = fetchPathByName(EditEnum.CHART_EDIT_NAME, 'href')
    if (!path) return
    const id = fetchRouteParamsLocation()
    updateToSession(id)
    routerTurnByPath(path, [id], undefined, true)
  }, 2000)
}

// 把内存中的数据同步到SessionStorage 便于传递给新窗口初始化数据
const updateToSession = (id: string) => {
  const storageInfo = chartEditStore.getProjectStorageInfo()
  const sessionStorageInfo = getSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST) || []

  if (sessionStorageInfo?.length) {
    const repeateIndex = sessionStorageInfo.findIndex((e: { id: string }) => e.id === id)
    // 重复替换
    if (repeateIndex !== -1) {
      sessionStorageInfo.splice(repeateIndex, 1, { ...storageInfo, id })
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
    } else {
      sessionStorageInfo.push({ ...storageInfo, id })
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, sessionStorageInfo)
    }
  } else {
    setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, [{ ...storageInfo, id }])
  }
}

// 配置列表
const btnList: BtnListType[] = [
  {
    key: 'import',
    type: TypeEnum.IMPORTUPLOAD,
    name: '导入',
    icon: ShareIcon
  },
  {
    key: 'export',
    type: TypeEnum.BUTTON,
    name: '导出',
    icon: DownloadIcon,
    handle: exportHandle
  },
  {
    key: 'edit',
    type: TypeEnum.BUTTON,
    name: '编辑',
    icon: CreateIcon,
    handle: editHandle
  },
  {
    key: 'setting',
    type: TypeEnum.BUTTON,
    name: '设置',
    icon: SettingsSharpIcon,
    handle: () => {
      globalSettingModel.value = true
    }
  }
]
</script>

<style lang="scss" scoped>
/* 底部区域的高度 */
$dockHeight: 30px;
$dockBottom: 60px;
$dockMiniWidth: 200px;
$dockMiniBottom: 53px;

$asideHeight: 118px;
$asideMiniHeight: 30px;
$asideBottom: 70px;

@include go('chart-edit-tools') {
  @extend .go-background-filter;
  position: absolute;
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-radius: 999px;
  border: 1px solid rgba(var(--app-theme-rgb), 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), transparent),
    rgba(10, 14, 23, 0.72);
  box-shadow:
    0 18px 42px rgba(0, 0, 0, 0.36),
    0 0 22px rgba(var(--app-theme-rgb), 0.1);

  :deep(.n-button) {
    border-radius: 999px;
    border: 1px solid rgba(var(--app-theme-rgb), 0.08);
    background: rgba(15, 23, 42, 0.42);
    transition: all 0.22s ease;

    &:hover {
      border-color: rgba(var(--app-theme-rgb), 0.28);
      background: rgba(var(--app-theme-rgb), 0.11);
      transform: translateY(-1px);
    }
  }

  &.aside {
    display: flex;
    justify-content: center;
    flex-direction: column;
    height: auto;
    right: 24px;
    padding: 8px 6px;
    bottom: $asideBottom;
    overflow: hidden;
    transition: height ease 0.4s;
    border-radius: 16px;

    .aside-toggle {
      width: 24px;
      height: 24px;
      margin-bottom: 10px;
      color: rgba(226, 232, 240, 0.72);
      background: rgba(15, 23, 42, 0.62);
      border: 1px solid rgba(var(--app-theme-rgb), 0.14);
      flex-shrink: 0;

      &:hover {
        color: #fff;
        border-color: rgba(var(--app-theme-rgb), 0.34);
        background: rgba(var(--app-theme-rgb), 0.14);
      }
    }

    .btn-item {
      margin-bottom: 6px;
      &:last-of-type {
        margin-bottom: 0;
      }
      @include deep() {
        .n-button {
          width: 34px;
          height: 34px;
          border-radius: 10px;
          padding: 0;
        }

        .n-button__icon {
          margin: 0;
          display: inline-flex;
          align-items: center;
          justify-content: center;
        }
      }
    }
    &.unMini {
      animation: aside-in 0.4s ease forwards;
      background:
        linear-gradient(180deg, rgba(var(--app-theme-rgb), 0.12), transparent),
        rgba(10, 14, 23, 0.78);
      border-color: rgba(var(--app-theme-rgb), 0.18);

      @keyframes aside-in {
        0% {
          opacity: 0.5;
          height: $asideMiniHeight;
        }
        100% {
          height: 182px;
          opacity: 1;
        }
      }
      .btn-item {
        position: relative;
        display: block;

        &::before {
          content: '';
          position: absolute;
          left: -7px;
          top: 8px;
          width: 2px;
          height: 18px;
          border-radius: 999px;
          background: rgba(var(--app-theme-rgb), 0);
          transition: background 0.2s ease;
        }

        &:hover::before {
          background: var(--app-theme, #00d4ff);
          box-shadow: 0 0 8px rgba(var(--app-theme-rgb), 0.5);
        }
      }
      .asideLogo {
        opacity: 0.4;
      }
    }
    &.isMini {
      cursor: pointer;
      padding: 6px;
      background:
        radial-gradient(circle, rgba(var(--app-theme-rgb), 0.18), rgba(15, 23, 42, 0.78));
      animation: aside-mini-in 0.4s ease forwards;
      border-radius: 999px;

      @keyframes aside-mini-in {
        0% {
          opacity: 0.5;
          height: $asideHeight;
        }
        100% {
          opacity: 1;
          height: $asideMiniHeight;
        }
      }
      .btn-item {
        position: relative;
        display: none;
      }
      .aside-toggle {
        margin-bottom: 0;
      }
      .asideLogo {
        display: none;
      }
    }
  }

  &.dock {
    width: auto;
    left: 50%;
    transform: translateX(-50%);
    .btn-item {
      margin-right: 20px;
      &:last-child {
        margin-right: 0;
      }
    }
    &.unMini {
      animation: dock-in 0.4s ease forwards;
      @keyframes dock-in {
        0% {
          opacity: 0;
          height: 0;
          padding: 5px;
          bottom: $dockMiniBottom;
        }
        100% {
          height: $dockHeight;
          padding: 8px 30px;
          bottom: $dockBottom;
          border-radius: 25px;
        }
      }
      &::after {
        content: '';
        position: absolute;
        left: 0;
        width: 100%;
        height: 10px;
        bottom: -10px;
        cursor: pointer;
      }
    }
    /* 最小化 */
    &.isMini {
      height: 0;
      width: $dockMiniWidth;
      bottom: $dockMiniBottom;
      padding: 5px;
      border-radius: 8px;
      cursor: pointer;
      border: 1px solid rgba(var(--app-theme-rgb), 0.12);
      animation: dock-mini-in 1s ease forwards;
      @keyframes dock-mini-in {
        0% {
          opacity: 1;
          height: $dockHeight;
          padding: 8px 30px;
          bottom: $dockBottom;
          border-radius: 25px;
        }
        20% {
          height: 0;
          border-radius: 8px;
        }
        50% {
          opacity: 0;
          bottom: calc(#{$dockMiniBottom} - 10px);
          pointer-events: none;
        }
        100% {
          opacity: 1;
          height: 0;
          padding: 5px;
          bottom: $dockMiniBottom;
        }
      }
      .btn-item {
        position: relative;
        bottom: -50px;
        display: none;
      }
    }
  }
}
</style>
