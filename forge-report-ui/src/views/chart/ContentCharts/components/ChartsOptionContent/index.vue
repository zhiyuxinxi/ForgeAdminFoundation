<template>
  <!-- 侧边栏和数据分发处理 -->
  <div class="go-chart-common">
    <div v-show="hidePackageOneCategory" class="category-rail">
      <div class="category-rail-scroll">
        <button
          v-for="item in packages.menuOptions"
          :key="item.key"
          class="category-chip"
          :class="{ active: selectValue === item.key }"
          type="button"
          @click="clickItemHandle(item.key)"
        >
          <span>{{ $ui(item.label) }}</span>
          <em>{{ packages.categorys[item.key]?.length || 0 }}</em>
        </button>
      </div>
      <n-popover trigger="click" :show-arrow="false" placement="bottom-end" :to="false">
        <template #trigger>
          <button class="strip-expand-btn" type="button">
            <n-icon size="18"><chevron-down-icon /></n-icon>
          </button>
        </template>
        <div class="category-popover-menu">
          <div
            v-for="item in packages.menuOptions"
            :key="item.key"
            class="category-popover-item"
            :class="{ active: selectValue === item.key }"
            @click="clickItemHandle(item.key)"
          >
            <span class="category-popover-item-count">{{ packages.categorys[item.key]?.length || 0 }}</span>
            <span class="category-popover-item-label">{{ $ui(item.label) }}</span>
          </div>
        </div>
      </n-popover>
    </div>
    <div class="chart-content-list">
      <n-spin :show="packagesStore.materialPhotosLoading" class="chart-content-spin">
        <n-scrollbar trigger="none">
          <charts-item-box :menuOptions="packages.selectOptions"></charts-item-box>
        </n-scrollbar>
      </n-spin>
      <div v-if="isPhotosPanel && materialPageCount > 1" class="material-pagebar">
        <span class="material-pagebar-total">{{ $ui("共") }} {{ packagesStore.materialPhotosTotal }} {{ $ui("张素材") }}</span>
        <n-pagination
          simple
          :page="packagesStore.materialPhotosPageNum"
          :page-size="packagesStore.materialPhotosPageSize"
          :item-count="packagesStore.materialPhotosTotal"
          @update:page="handleMaterialPhotoPageChange"
        />
      </div>
    </div>

    <n-modal
      v-model:show="packagesStore.materialUploadVisible"
      preset="card"
      :title="$ui(&quot;上传素材图片&quot;)"
      style="width: 520px"
    >
      <div class="material-upload-modal">
        <n-select
          v-model:value="materialUploadCategory"
          :options="materialCategoryUploadOptions"
          :placeholder="$ui(&quot;选择素材分类&quot;)"
        />
        <n-select
          v-model:value="materialUploadVisibility"
          :options="visibilityOptions"
          :placeholder="$ui(&quot;可见范围&quot;)"
        />
        <n-upload
          accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
          :show-file-list="false"
          :custom-request="handleMaterialUpload"
        >
          <n-button type="primary" block :loading="materialUploading">
            {{ $ui("选择图片并上传到素材库") }}
          </n-button>
        </n-upload>
        <div class="material-upload-tip">
          {{ $ui("上传成功后会自动出现在左侧图片素材列表里。") }}
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { ref, watch, computed, reactive } from 'vue'
import { ConfigType, PackagesCategoryEnum } from '@/packages/index.d'
import { useSettingStore } from '@/store/modules/settingStore/settingStore'
import { loadAsyncComponent } from '@/utils'
import { usePackagesStore } from '@/store/modules/packagesStore/packagesStore'
import { icon } from '@/plugins'
import { reportMaterialCategoryOptions, uploadMaterialAssetApi } from '@/api/file'
import { useUserStore } from '@/store/modules/userStore/userStore'
import type { UploadCustomRequestOptions } from 'naive-ui'

const { ChevronDownIcon } = icon.ionicons5

const ChartsItemBox = loadAsyncComponent(() => import('../ChartsItemBox/index.vue'))
const packagesStore = usePackagesStore()
const userStore = useUserStore()
const materialUploading = ref(false)
const materialUploadCategory = ref('background')
const materialUploadVisibility = ref(true)
const materialCategoryUploadOptions = reportMaterialCategoryOptions.filter(item => item.value !== 'all')

const isAdmin = computed(() => userStore.permissions.includes('*:*:*'))
const visibilityOptions = computed(() => [
  { get label() { return uiText("私有") }, value: true },
  { get label() { return uiText("公共") }, value: false, disabled: !isAdmin.value }
])

const props = defineProps({
  selectOptions: {
    type: Object,
    default: () => ({})
  }
})

const isPhotosPanel = computed(() => props.selectOptions?.key === PackagesCategoryEnum.PHOTOS)
const materialPageCount = computed(() => Math.ceil(packagesStore.materialPhotosTotal / packagesStore.materialPhotosPageSize))

// 隐藏设置
const settingStore = useSettingStore()

const hidePackageOneCategory = computed(() => {
  if (packages.categorysNum > 2) return true
  return !settingStore.getHidePackageOneCategory
})

let packages = reactive<{
  [T: string]: any
}>({
  // 侧边栏
  menuOptions: [],
  // 当前选择
  selectOptions: {},
  // 分类归档
  categorys: {
    all: []
  },
  categoryNames: {
    all: '所有'
  },
  // 分类归档数量
  categorysNum: 0,
  // 存储不同类别组件进来的选中值
  saveSelectOptions: {}
})

const selectValue = ref<string>('all')

// 设置初始列表
const setSelectOptions = (categorys: any) => {
  for (const val in categorys) {
    packages.selectOptions = categorys[val]
    break
  }
}

const rebuildPackagesCategory = (newData: { list: ConfigType[] }) => {
  packages.categorysNum = 0
  packages.menuOptions = []
  packages.categorys = {
    all: []
  }
  packages.categoryNames = {
    all: '所有'
  }
  if (!newData) return
  newData.list.forEach((e: ConfigType) => {
    const value: ConfigType[] = (packages.categorys as any)[e.category]
    packages.categorys[e.category] = value && value.length ? [...value, e] : [e]
    packages.categoryNames[e.category] = e.categoryName
    packages.categorys['all'].push(e)
  })
  for (const val in packages.categorys) {
    packages.categorysNum += 1
    packages.menuOptions.push({
      key: val,
      label: packages.categoryNames[val]
    })
  }
  setSelectOptions(packages.categorys)
  selectValue.value = packages.menuOptions[0]['key']
}

watch(
  // @ts-ignore
  () => props.selectOptions,
  async (newData: { list: ConfigType[]; key?: string }) => {
    if (newData?.key === PackagesCategoryEnum.PHOTOS) {
      await packagesStore.loadMaterialPhotos(1)
    }
    rebuildPackagesCategory(newData)
  },
  {
    immediate: true
  }
)

watch(
  () => packagesStore.materialPhotosVersion,
  () => {
    if (props.selectOptions?.key === PackagesCategoryEnum.PHOTOS) {
      rebuildPackagesCategory(props.selectOptions as { list: ConfigType[] })
    }
  }
)

// 处理点击事件
const clickItemHandle = (key: string) => {
  selectValue.value = key
  packages.selectOptions = packages.categorys[key]
}

const handleMaterialPhotoPageChange = async (page: number) => {
  await packagesStore.loadMaterialPhotos(page)
}

const handleMaterialUpload = async (options: UploadCustomRequestOptions) => {
  const rawFile = options.file.file
  if (!rawFile) {
    return
  }
  materialUploading.value = true
  try {
    const res = await uploadMaterialAssetApi(rawFile as File, materialUploadCategory.value, materialUploadVisibility.value)
    if (res.code === 200) {
      options.onFinish?.()
      packagesStore.closeMaterialUploadDialog()
      await packagesStore.loadMaterialPhotos(1)
      window['$message']?.success(uiText("素材上传成功"))
    } else {
      throw new Error(res.msg || uiText("素材上传失败"))
    }
  } catch (error: any) {
    options.onError?.()
    window['$message']?.error(error?.message || uiText("素材上传失败"))
  } finally {
    materialUploading.value = false
  }
}
</script>

<style lang="scss" scoped>
/* 此高度与 ContentBox 组件关联*/
$topHeight: 40px;
$workbenchGapHeight: 24px;
@include go('chart-common') {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;

  .category-rail {
    position: relative;
    flex-shrink: 0;
    background: rgba(2, 6, 23, 0.18);

    &::after {
      content: '';
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      width: 48px;
      pointer-events: none;
      z-index: 2;
      background: linear-gradient(to right, transparent 0%, rgba(8, 13, 22, 0.96) 60%);
    }

    .strip-expand-btn {
      position: absolute;
      top: 50%;
      right: 4px;
      transform: translateY(-50%);
      z-index: 3;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid rgba(148, 163, 184, 0.16);
      border-radius: 4px;
      background: rgba(15, 23, 42, 0.64);
      color: rgba(203, 213, 225, 0.78);
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        color: #fff;
        border-color: rgba(var(--app-theme-rgb), 0.3);
        background: rgba(var(--app-theme-rgb), 0.12);
      }
    }

    .category-rail-scroll {
      display: flex;
      gap: 7px;
      padding: 10px 34px 8px 12px;
      overflow-x: auto;
      overflow-y: hidden;
      scrollbar-width: none;
      cursor: grab;

      &::-webkit-scrollbar {
        display: none;
      }
    }

    .category-chip {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      height: 30px;
      flex: 0 0 auto;
      padding: 0 6px 0 10px;
      border: 1px solid rgba(148, 163, 184, 0.12);
      border-radius: 999px;
      color: rgba(203, 213, 225, 0.76);
      background: rgba(15, 23, 42, 0.54);
      cursor: pointer;
      transition: all 0.2s ease;

      span {
        max-width: 74px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 12px;
        line-height: 1;
      }

      em {
        min-width: 18px;
        height: 18px;
        padding: 0 5px;
        border-radius: 999px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-style: normal;
        font-size: 10px;
        line-height: 18px;
        color: rgba(226, 232, 240, 0.86);
        background: rgba(255, 255, 255, 0.06);
      }

      &:hover {
        color: #fff;
        border-color: rgba(var(--app-theme-rgb), 0.24);
        transform: translateY(-1px);
      }

      &.active {
        color: #fff;
        border-color: rgba(var(--app-theme-rgb), 0.4);
        background:
          linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.24), rgba(var(--app-theme-rgb), 0.07)),
          rgba(15, 23, 42, 0.72);

        em {
          color: #fff;
          background: rgba(var(--app-theme-rgb), 0.3);
        }
      }
    }
  }

  .chart-content-list {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    align-items: center;

    .chart-content-spin {
      width: 100%;
      flex: 1;
      min-height: 0;

      :deep(.n-spin-content) {
        height: 100%;
      }
    }

    .material-pagebar {
      width: 100%;
      min-height: 38px;
      padding: 7px 10px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      flex-shrink: 0;
      border-top: 1px solid rgba(148, 163, 184, 0.12);
      background: rgba(8, 13, 22, 0.78);

      .material-pagebar-total {
        font-size: 12px;
        color: rgba(203, 213, 225, 0.72);
        white-space: nowrap;
      }
    }
  }
}

.material-upload-modal {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.material-upload-tip {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.92);
}
</style>

<style lang="scss">
.category-popover-menu {
  min-width: 140px;
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;

  .category-popover-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 7px 10px;
    border-radius: 8px;
    cursor: pointer;
    color: rgba(203, 213, 225, 0.82);
    font-size: 13px;
    transition: all 0.18s ease;

    .category-popover-item-count {
      min-width: 22px;
      height: 20px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
      font-size: 10px;
      font-weight: 600;
      color: rgba(226, 232, 240, 0.7);
      background: rgba(255, 255, 255, 0.08);
    }

    .category-popover-item-label {
      white-space: nowrap;
    }

    &:hover {
      color: #fff;
      background: rgba(var(--app-theme-rgb), 0.12);

      .category-popover-item-count {
        color: #fff;
        background: rgba(255, 255, 255, 0.12);
      }
    }

    &.active {
      color: #fff;
      background: rgba(var(--app-theme-rgb), 0.2);

      .category-popover-item-count {
        color: #fff;
        background: rgba(var(--app-theme-rgb), 0.28);
      }
    }
  }
}
</style>
