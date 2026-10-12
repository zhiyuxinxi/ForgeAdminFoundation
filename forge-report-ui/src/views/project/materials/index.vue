<template>
  <div class="go-project-materials">
    <section class="materials-hero go-float-up">
      <div class="hero-copy">
        <div class="hero-kicker">ASSET MATRIX</div>
        <h2 class="hero-title go-text-neon">
          <span class="title-accent">//</span>
          {{ $ui("素材库") }}
        </h2>
        <p class="hero-desc">
          {{ $ui("统一管理大屏项目常用图片素材。上传时先标记用途分类，后续背景、装饰、图标位都能直接复用。") }}
        </p>
      </div>
      <div class="hero-stats">
        <div class="stat-card">
          <span class="stat-label">{{ $ui("素材总量") }}</span>
          <strong class="stat-value">{{ overview.total }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ $ui("背景") }}</span>
          <strong class="stat-value">{{ overview.background }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ $ui("图标") }}</span>
          <strong class="stat-value">{{ overview.icon }}</strong>
        </div>
      </div>
    </section>

    <section class="materials-shell">
      <div class="materials-side">
        <div class="panel upload-panel">
          <div class="panel-header">
            <div>
              <div class="panel-eyebrow">UPLOAD PORT</div>
              <h3>{{ $ui("上传素材") }}</h3>
            </div>
            <n-tag size="small" type="success" round>
              {{ $ui("仅图片") }}
            </n-tag>
          </div>

          <div class="upload-category">
            <span>{{ $ui("素材分类") }}</span>
            <n-select
              v-model:value="uploadCategory"
              :options="uploadCategoryOptions"
              size="small"
            />
          </div>

          <div class="upload-category">
            <span>{{ $ui("可见范围") }}</span>
            <n-select
              v-model:value="uploadVisibility"
              :options="visibilityOptions"
              size="small"
            />
          </div>

          <n-upload
            multiple
            accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
            :show-file-list="false"
            :custom-request="handleUploadRequest"
            :on-before-upload="handleBeforeUpload"
            :disabled="uploading"
          >
            <n-upload-dragger>
              <div class="upload-dragger">
                <n-icon size="26" class="upload-icon">
                  <component :is="ImagesIcon"></component>
                </n-icon>
                <div class="upload-title">
                  {{ uploading ? $ui("素材上传中...") : $ui("拖入图片或点击上传") }}
                </div>
                <div class="upload-hint">
                  {{ $ui("支持 JPG / PNG / WEBP / GIF / SVG，单张建议 15MB 内") }}
                </div>
              </div>
            </n-upload-dragger>
          </n-upload>

          <div class="upload-tips">
            <div class="tip-item">
              <span class="tip-dot"></span>
              {{ $ui("背景类适合整屏底图、氛围图") }}
            </div>
            <div class="tip-item">
              <span class="tip-dot"></span>
              {{ $ui("面板类适合边框、容器、装饰片段") }}
            </div>
            <div class="tip-item">
              <span class="tip-dot"></span>
              {{ $ui("图标类适合功能图标、状态标识") }}
            </div>
          </div>
        </div>
      </div>

      <div class="materials-main">
        <div class="materials-toolbar">
          <div class="toolbar-filters">
            <n-input
              v-model:value="keyword"
              clearable
              :placeholder="$ui(&quot;按文件名搜索&quot;)"
              size="small"
              class="toolbar-search"
              @keyup.enter="handleSearch"
            />
            <n-radio-group
              v-model:value="activeCategory"
              size="small"
              @update:value="handleCategoryChange"
            >
              <n-radio-button
                v-for="item in materialCategoryOptions"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </n-radio-button>
            </n-radio-group>
            <span class="toolbar-sep"></span>
            <div class="visibility-chips">
              <button
                v-for="opt in visibilityChips"
                :key="String(opt.value)"
                class="visibility-chip"
                :class="{ active: activeVisibility === opt.value }"
                @click="activeVisibility = opt.value; handleVisibilityChange()"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>
          <div class="toolbar-info">
            <span class="toolbar-count">
              {{ $ui("共") }} {{ pagination.total }} {{ $ui("张") }}
            </span>
            <n-pagination
              simple
              :page="pagination.pageNum"
              :page-size="pagination.pageSize"
              :item-count="pagination.total"
              @update:page="handlePageChange"
            />
          </div>
        </div>

        <n-spin :show="loading">
          <div v-if="materials.length" class="materials-grid">
            <article
              v-for="item in materials"
              :key="item.fileId"
              class="material-card"
            >
              <div class="material-preview" @click="openPreview(item)">
                <fg-auth-image
                  :src="item.fileId"
                  :alt="item.originalName"
                  img-class="material-image"
                  placeholder-class="material-image-placeholder"
                  placeholder-text="图片加载中..."
                />
              </div>

              <div class="material-body">
                <div class="material-row">
                  <n-tag size="small" :bordered="false" type="info">
                    {{ getCategoryLabel(item.businessId) }}
                  </n-tag>
                  <n-tag size="small" :bordered="false" :type="item.isPrivate ? 'warning' : 'success'">
                    {{ item.isPrivate ? $ui("私有") : $ui("公共") }}
                  </n-tag>
                  <span class="material-size">{{ formatFileSize(item.fileSize) }}</span>
                </div>

                <div
                  class="material-name"
                  :class="{ editing: editingId === item.fileId }"
                  :title="editingId !== item.fileId ? item.originalName : ''"
                  @click.stop
                >
                  <template v-if="editingId === item.fileId">
                    <input
                      v-model="editingName"
                      class="rename-input"
                      maxlength="255"
                      @keyup.enter="confirmRename(item)"
                      @keyup.escape="cancelRename"
                      @blur="confirmRename(item)"
                    >
                  </template>
                  <template v-else>
                    {{ item.originalName }}
                    <n-icon
                      v-permission="'report:user:editName'"
                      size="14"
                      class="rename-icon"
                      :title="$ui(&quot;重命名&quot;)"
                      @click="startRename(item)"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14"><path d="M17 3a2.83 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                    </n-icon>
                  </template>
                </div>

                <div class="material-meta">
                  <span>{{ formatDate(item.uploadTime) }}</span>
                  <span>{{ $ui("下载") }} {{ item.downloadCount || 0 }}</span>
                </div>

                <div class="material-actions">
                  <a href="" class="action-link" @click.prevent="openPreview(item)">{{ $ui("预览") }}</a>
                  <span class="action-divider">|</span>
                  <a v-permission="'report:user:down'" href="" class="action-link" @click.prevent="handleDownload(item)">{{ $ui("下载") }}</a>
                  <span v-permission="'report:user:del'" class="action-divider">|</span>
                  <a v-permission="'report:user:del'" href="" class="action-link danger" @click.prevent="handleDelete(item)">{{ $ui("删除") }}</a>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="materials-empty">
            <n-empty :description="$ui(&quot;当前筛选条件下还没有素材&quot;)"></n-empty>
          </div>
        </n-spin>
      </div>
    </section>

    <n-modal
      v-model:show="previewVisible"
      preset="card"
      :title="$ui(&quot;素材预览&quot;)"
      style="width: 960px; max-width: 92vw"
    >
      <div class="preview-shell" v-if="previewTarget">
        <fg-auth-image
          :src="previewTarget.fileId"
          :alt="previewTarget.originalName"
          :lazy="false"
          img-class="preview-image"
          placeholder-text="预览加载中..."
        />
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, nextTick, onMounted, ref } from 'vue'
import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui'
import { icon } from '@/plugins'
import FgAuthImage from '@/components/FgAuthImage/index.vue'
import {
  deleteMaterialAssetApi,
  getMaterialAssetDownloadUrl,
  getMaterialAssetPageApi,
  renameMaterialAssetApi,
  reportMaterialCategoryOptions,
  uploadMaterialAssetApi
} from '@/api/file'
import type { MaterialAsset, ReportMaterialCategory } from '@/api/file'
import { StorageEnum } from '@/enums/storageEnum'
import { getLocalStorage } from '@/utils/storage'
import { useUserStore } from '@/store/modules/userStore/userStore'

const { ImagesIcon } = icon.ionicons5

const userStore = useUserStore()
const isAdmin = computed(() => userStore.permissions.includes('*:*:*'))

const visibilityChips = computed(() => [
  { get label() { return uiText("全部") }, value: undefined },
  { get label() { return uiText("私有") }, value: true },
  { get label() { return uiText("公共") }, value: false }
])

const visibilityOptions = computed(() => [
  { get label() { return uiText("私有") }, value: true },
  { get label() { return uiText("公共") }, value: false, disabled: !isAdmin.value }
])

const materialCategoryOptions = reportMaterialCategoryOptions
const uploadCategoryOptions = materialCategoryOptions.filter(item => item.value !== 'all')

const loading = ref(false)
const uploading = ref(false)
const keyword = ref('')
const activeCategory = ref<ReportMaterialCategory>('all')
const activeVisibility = ref<boolean | undefined>(undefined)
const uploadCategory = ref<Exclude<ReportMaterialCategory, 'all'>>('background')
const uploadVisibility = ref(true)
const materials = ref<MaterialAsset[]>([])
const previewVisible = ref(false)
const previewTarget = ref<MaterialAsset | null>(null)
const editingId = ref('')
const editingName = ref('')

const pagination = ref({
  pageNum: 1,
  pageSize: 24,
  total: 0
})

const overview = ref({
  total: 0,
  background: 0,
  panel: 0,
  icon: 0,
  illustration: 0
})

const currentCategoryLabel = computed(() => {
  const target = materialCategoryOptions.find(item => item.value === activeCategory.value)
  return target?.label || '全部素材'
})

const getCategoryLabel = (value?: string) => {
  const target = materialCategoryOptions.find(item => item.value === value)
  return target?.label || '未分类'
}

const formatDate = (value?: string) => {
  if (!value)
    return '--'
  return new Date(value).toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  })
}

const formatFileSize = (size?: number) => {
  if (!size)
    return '0 B'
  if (size < 1024)
    return `${size} B`
  if (size < 1024 * 1024)
    return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const fetchMaterials = async () => {
  loading.value = true
  try {
    const res = await getMaterialAssetPageApi({
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize,
      originalName: keyword.value.trim() || undefined,
      businessId: activeCategory.value === 'all' ? undefined : activeCategory.value,
      isPrivate: activeVisibility.value
    })
    materials.value = res.data?.records || []
    pagination.value.total = Number(res.data?.total || 0)
  } catch (error: any) {
    window['$message']?.error(error?.message || uiText("素材加载失败"))
  } finally {
    loading.value = false
  }
}

const fetchOverview = async () => {
  try {
    const [total, background, panel, icon, illustration] = await Promise.all([
      getMaterialAssetPageApi({ pageNum: 1, pageSize: 1 }),
      getMaterialAssetPageApi({ pageNum: 1, pageSize: 1, businessId: 'background' }),
      getMaterialAssetPageApi({ pageNum: 1, pageSize: 1, businessId: 'panel' }),
      getMaterialAssetPageApi({ pageNum: 1, pageSize: 1, businessId: 'icon' }),
      getMaterialAssetPageApi({ pageNum: 1, pageSize: 1, businessId: 'illustration' })
    ])
    overview.value = {
      total: Number(total.data?.total || 0),
      background: Number(background.data?.total || 0),
      panel: Number(panel.data?.total || 0),
      icon: Number(icon.data?.total || 0),
      illustration: Number(illustration.data?.total || 0)
    }
  } catch (error) {
    console.error('素材统计加载失败', error)
  }
}

const refreshAll = async () => {
  await Promise.all([fetchMaterials(), fetchOverview()])
}

const handleSearch = async () => {
  pagination.value.pageNum = 1
  await fetchMaterials()
}

const handleCategoryChange = async () => {
  pagination.value.pageNum = 1
  await fetchMaterials()
}

const handleVisibilityChange = async () => {
  pagination.value.pageNum = 1
  await fetchMaterials()
}

const handlePageChange = async (page: number) => {
  pagination.value.pageNum = page
  await fetchMaterials()
}

const handleBeforeUpload = ({ file }: { file: UploadFileInfo }) => {
  const currentFile = file.file
  if (!currentFile)
    return false

  if (!currentFile.type.startsWith('image/')) {
    window['$message']?.warning(uiText("素材库当前只支持上传图片"))
    return false
  }

  if (currentFile.size > 15 * 1024 * 1024) {
    window['$message']?.warning(uiText("单张图片请控制在 15MB 内"))
    return false
  }

  return true
}

const handleUploadRequest = async (options: UploadCustomRequestOptions) => {
  const rawFile = options.file.file
  if (!rawFile)
    return

  uploading.value = true
  try {
    const res = await uploadMaterialAssetApi(rawFile as File, uploadCategory.value, uploadVisibility.value)
    if (res.code === 200) {
      options.onFinish?.()
      window['$message']?.success(uiText("素材上传成功"))
      await refreshAll()
    } else {
      throw new Error(res.msg || uiText("素材上传失败"))
    }
  } catch (error: any) {
    options.onError?.()
    window['$message']?.error(error?.message || uiText("素材上传失败"))
  } finally {
    uploading.value = false
  }
}

const openPreview = (item: MaterialAsset) => {
  previewTarget.value = item
  previewVisible.value = true
}

const handleDownload = async (item: MaterialAsset) => {
  try {
    const token = getLocalStorage(StorageEnum.GO_ACCESS_TOKEN_STORE)
    const url = getMaterialAssetDownloadUrl(item.fileId)

    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })

    if (!response.ok) {
      throw new Error(uiText("下载失败"))
    }

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = blobUrl
    link.download = item.originalName || ''
    link.rel = 'noopener'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    setTimeout(() => URL.revokeObjectURL(blobUrl), 1000)
  } catch (error: any) {
    window['$message']?.error(error?.message || uiText("素材下载失败"))
  }
}

const startRename = (item: MaterialAsset) => {
  editingId.value = item.fileId
  editingName.value = item.originalName
  nextTick(() => {
    const input = document.querySelector('.rename-input') as HTMLInputElement | null
    input?.focus()
    input?.select()
  })
}

const cancelRename = () => {
  editingId.value = ''
  editingName.value = ''
}

const confirmRename = async (item: MaterialAsset) => {
  const newName = editingName.value.trim()
  if (!newName || newName === item.originalName) {
    cancelRename()
    return
  }
  try {
    const res = await renameMaterialAssetApi(item.fileId, newName)
    if (res.code === 200) {
      item.originalName = newName
      window['$message']?.success(uiText("重命名成功"))
    } else {
      window['$message']?.error(res.msg || uiText("重命名失败"))
    }
  } catch (e: any) {
    window['$message']?.error(e?.message || uiText("重命名失败"))
  } finally {
    cancelRename()
  }
}

const handleDelete = (item: MaterialAsset) => {
  window['$dialog']?.warning({
    title: uiText("删除素材"),
    content: uiText("确定删除素材“{0}”吗？", { 0: (item.originalName) }),
    positiveText: uiText("删除"),
    negativeText: uiText("取消"),
    onPositiveClick: async () => {
      try {
        const res = await deleteMaterialAssetApi(item.fileId)
        if (res.code === 200) {
          window['$message']?.success(uiText("素材已删除"))
          if (materials.value.length === 1 && pagination.value.pageNum > 1) {
            pagination.value.pageNum -= 1
          }
          await refreshAll()
        } else {
          throw new Error(res.msg || uiText("素材删除失败"))
        }
      } catch (error: any) {
        window['$message']?.error(error?.message || uiText("素材删除失败"))
      }
    }
  })
}

onMounted(async () => {
  await refreshAll()
})
</script>

<style lang="scss" scoped>
@include go(project-materials) {
  padding: 0 32px 32px;
  min-height: calc(100vh - 2px);
}

.materials-hero {
  padding: 26px 0 22px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-end;
}

.hero-copy {
  max-width: 680px;
}

.hero-kicker {
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(var(--app-theme-rgb), 0.78);
  margin-bottom: 8px;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
}

.title-accent {
  color: $--color-accent;
  font-weight: 300;
  margin-right: 8px;
}

.hero-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  max-width: 620px;
  @include fetch-color(3);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(108px, 1fr));
  gap: 12px;
  min-width: 360px;
}

.stat-card {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(var(--app-theme-rgb), 0.1);
  background:
    linear-gradient(160deg, rgba(var(--app-theme-rgb), 0.1), rgba(255, 255, 255, 0.02)),
    rgba(10, 16, 28, 0.62);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.14);
}

.stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 11px;
  letter-spacing: 1px;
  @include fetch-color(3);
}

.stat-value {
  font-size: 22px;
  line-height: 1;
  color: $--color-primary;
}

.materials-shell {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  min-height: calc(100vh - 180px);
}

.materials-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  border-radius: 18px;
  border: 1px solid rgba(var(--app-theme-rgb), 0.1);
  background:
    linear-gradient(180deg, rgba(var(--app-theme-rgb), 0.06), rgba(var(--app-theme-rgb), 0.01)),
    rgba(10, 16, 28, 0.76);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(16px);
  padding: 18px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;

  h3 {
    margin: 2px 0 0;
    font-size: 17px;
    font-weight: 700;
  }

  &.compact {
    margin-bottom: 14px;
  }
}

.panel-eyebrow {
  font-size: 10px;
  letter-spacing: 1.8px;
  color: rgba(var(--app-theme-rgb), 0.7);
}

.upload-category,
.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;

  span {
    font-size: 12px;
    font-weight: 600;
    @include fetch-color(2);
  }
}

.upload-dragger {
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.upload-icon {
  color: $--color-primary;
  filter: drop-shadow(0 0 10px rgba(var(--app-theme-rgb), 0.38));
}

.upload-title {
  font-size: 14px;
  font-weight: 700;
}

.upload-hint {
  font-size: 12px;
  text-align: center;
  line-height: 1.7;
  @include fetch-color(3);
}

.upload-tips {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  @include fetch-color(3);
}

.tip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $--color-primary;
  box-shadow: 0 0 10px rgba(var(--app-theme-rgb), 0.5);
}

.materials-main {
  min-width: 0;
  border-radius: 22px;
  border: 1px solid rgba(var(--app-theme-rgb), 0.1);
  background:
    linear-gradient(180deg, rgba(var(--app-theme-rgb), 0.05), rgba(var(--app-theme-rgb), 0.01)),
    rgba(8, 13, 24, 0.82);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.18);
  padding: 18px;
}

.materials-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(var(--app-theme-rgb), 0.08);
}

.toolbar-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}

.toolbar-search {
  width: 200px;
  flex-shrink: 0;
}

.visibility-chips {
  display: flex;
  gap: 4px;
}

.visibility-chip {
  height: 28px;
  padding: 0 12px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 999px;
  font-size: 12px;
  color: rgba(203, 213, 225, 0.76);
  background: rgba(15, 23, 42, 0.5);
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    color: #fff;
    border-color: rgba(var(--app-theme-rgb), 0.3);
  }
  &.active {
    color: #fff;
    border-color: rgba(var(--app-theme-rgb), 0.4);
    background: linear-gradient(135deg, rgba(var(--app-theme-rgb), 0.2), rgba(var(--app-theme-rgb), 0.06));
  }
}

/* 分类 radio 换行不溢出 */
.toolbar-filters :deep(.n-radio-group) {
  flex-wrap: wrap;
  display: flex;
}

.toolbar-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.toolbar-count {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  @include fetch-color(2);
}

.materials-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(218px, 1fr));
  gap: 16px;
}

.material-card {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(var(--app-theme-rgb), 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent),
    rgba(12, 18, 30, 0.9);
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;

  &:hover {
    transform: translateY(-3px);
    border-color: rgba(var(--app-theme-rgb), 0.22);
    box-shadow: 0 18px 40px rgba(0, 0, 0, 0.2);
  }
}

.material-preview {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  cursor: pointer;
  background: rgba(var(--app-theme-rgb), 0.04);
  :deep(img){
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
  }
}

.material-body {
  padding: 12px 12px 14px;
}

.material-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;

  .material-size {
    margin-left: auto;
  }
}

.material-meta,
.material-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.material-size,
.material-meta {
  font-size: 12px;
  @include fetch-color(3);
}

.material-name {
  margin: 10px 0 8px;
  font-size: 13px;
  line-height: 1.6;
  min-height: 42px;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  word-break: break-all;

  &:not(.editing) {
    display: -webkit-box;
    overflow: hidden;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  &.editing {
    display: block;
  }
}

.rename-icon {
  flex-shrink: 0;
  margin-top: 2px;
  opacity: 0;
  cursor: pointer;
  color: $--color-primary;
  transition: opacity 0.15s;

  .material-name:hover & {
    opacity: 0.6;
  }
  &:hover {
    opacity: 1 !important;
  }
}

.rename-input {
  width: 100%;
  padding: 2px 4px;
  font-size: 13px;
  border: 1px solid $--color-primary;
  border-radius: 4px;
  background: rgba(var(--app-theme-rgb), 0.06);
  color: inherit;
  outline: none;
  line-height: 1.6;

  &:focus {
    box-shadow: 0 0 0 2px rgba(var(--app-theme-rgb), 0.15);
  }
}

.material-actions {
  margin-top: 12px;
  justify-content: flex-start;
}

.action-link {
  color: $--color-primary;
  text-decoration: none;
  font-size: 12px;
  transition: color 0.2s ease;

  &:hover {
    color: lighten($--color-primary, 6%);
  }

  &.danger {
    color: #ff6b72;
  }
}

.action-divider {
  font-size: 12px;
  @include fetch-color(3);
}

.materials-empty {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-shell {
  border-radius: 14px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.28);
  padding: 12px;
}

.preview-image {
  width: 100%;
  max-height: 72vh;
  object-fit: contain;
  display: block;
}

@media (max-width: 1200px) {
  .materials-shell {
    grid-template-columns: 1fr;
  }

  .materials-side {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
  }
}

@media (max-width: 900px) {
  .materials-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-stats {
    min-width: 0;
    width: 100%;
  }

  .materials-side {
    grid-template-columns: 1fr;
  }

  .materials-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  @include go(project-materials) {
    padding: 0 16px 24px;
  }

  .materials-grid {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}
</style>
