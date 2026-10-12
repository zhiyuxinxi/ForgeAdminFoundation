<template>
  <div class="material-selector">
    <div v-if="showPreview" class="selector-preview" :class="{ empty: !normalizedValue.length }">
      <template v-if="mode === 'single'">
        <div v-if="normalizedValue[0]" class="preview-single">
          <fg-auth-image :src="normalizedValue[0]" img-class="preview-single-image" :lazy="false" />
        </div>
        <div v-else class="preview-empty">
          <span>{{ $ui("未选择素材") }}</span>
        </div>
      </template>
      <template v-else>
        <div v-if="normalizedValue.length" class="preview-multi">
          <div class="preview-chip" v-for="(item, index) in normalizedValue.slice(0, 4)" :key="`${item}-${index}`">
            <fg-auth-image :src="item" img-class="preview-chip-image" :lazy="false" />
          </div>
          <div v-if="normalizedValue.length > 4" class="preview-count">+{{ normalizedValue.length - 4 }}</div>
        </div>
        <div v-else class="preview-empty">
          <span>{{ $ui("未添加素材") }}</span>
        </div>
      </template>
    </div>

    <div class="selector-actions">
      <n-button size="small" secondary @click="showModal = true">
        {{ buttonText }}
      </n-button>
      <n-button
        v-if="normalizedValue.length"
        size="small"
        quaternary
        @click="emitClear"
      >
        {{ $ui("清空") }}
      </n-button>
    </div>

    <n-modal
      v-model:show="showModal"
      preset="card"
      :title="mode === 'multiple' ? $ui(&quot;选择素材（可多选）&quot;) : $ui(&quot;选择素材&quot;)"
      style="width: 1080px; max-width: 94vw"
      @after-enter="openAndFetch"
    >
      <div class="selector-modal">
        <div class="modal-toolbar">
          <n-input
            v-model:value="keyword"
            clearable
            :placeholder="$ui(&quot;按文件名搜索素材&quot;)"
            @keyup.enter="fetchMaterials"
          />
          <n-select
            v-model:value="activeCategory"
            :options="categoryOptions"
            style="width: 160px"
            @update:value="handleCategoryChange"
          />
          <n-select
            v-model:value="activeVisibility"
            :options="visibilityOptions"
            style="width: 130px"
            @update:value="handleVisibilityChange"
          />
          <n-button type="primary" @click="fetchMaterials">{{ $ui("搜索") }}</n-button>
        </div>

        <div class="selector-upload">
          <n-button size="tiny" secondary @click="showUploadDrawer = true">
            {{ $ui("上传素材") }}
          </n-button>
        </div>

        <n-drawer
          v-model:show="showUploadDrawer"
          :width="340"
          placement="right"
        >
          <n-drawer-content :title="$ui(&quot;上传素材&quot;)" closable>
            <div class="upload-drawer-body">
              <n-select
                v-model:value="uploadCategory"
                :options="uploadCategoryOptions"
                :placeholder="$ui(&quot;素材分类&quot;)"
              />
              <n-select
                v-model:value="uploadVisibility"
                :options="uploadVisibilityOptions"
                :placeholder="$ui(&quot;可见范围&quot;)"
              />
              <n-upload
                accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
                :show-file-list="false"
                :custom-request="handleUploadMaterial"
              >
                <n-button type="primary" block :loading="uploading">
                  {{ $ui("选择图片并上传") }}
                </n-button>
              </n-upload>
              <div class="upload-drawer-tip">
                {{ $ui("上传后会自动出现在素材列表中") }}
              </div>
            </div>
          </n-drawer-content>
        </n-drawer>

        <div v-if="mode === 'multiple' && tempSelected.length" class="selected-hint">
          {{ $ui("已选") }} {{ tempSelected.length }} {{ $ui("张素材") }}
        </div>

        <n-spin :show="loading">
          <div v-if="materials.length" class="selector-grid">
            <button
              v-for="item in materials"
              :key="item.fileId"
              class="asset-card"
              :class="{ active: isSelected(item.fileId) }"
              type="button"
              @click="toggleAsset(item.fileId)"
            >
              <div class="asset-image">
                <fg-auth-image :src="item.fileId" img-class="asset-image-inner" />
              </div>
              <div class="asset-body">
                <div class="asset-top">
                  <n-tag size="small" :bordered="false" type="info">{{ getCategoryLabel(item.businessId) }}</n-tag>
                  <n-tag size="small" :bordered="false" :type="item.isPrivate ? 'warning' : 'success'">{{ item.isPrivate ? $ui("私有") : $ui("公共") }}</n-tag>
                  <span class="asset-size">{{ formatFileSize(item.fileSize) }}</span>
                </div>
                <div class="asset-name" :title="item.originalName">{{ item.originalName }}</div>
              </div>
            </button>
          </div>
          <div v-else class="selector-empty">
            <n-empty :description="$ui(&quot;当前筛选条件下没有素材&quot;)"></n-empty>
          </div>
        </n-spin>

        <div class="modal-footer">
          <n-pagination
            simple
            :page="pagination.pageNum"
            :page-size="pagination.pageSize"
            :item-count="pagination.total"
            @update:page="handlePageChange"
          />
          <div class="footer-actions">
            <n-button @click="showModal = false">{{ $ui("取消") }}</n-button>
            <n-button type="primary" :disabled="!tempSelected.length" @click="confirmSelection">
              {{ $ui("确认选择") }}
            </n-button>
          </div>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, ref, watch } from 'vue'
import FgAuthImage from '@/components/FgAuthImage/index.vue'
import { createFileAssetRef } from '@/utils'
import {
  getMaterialAssetPageApi,
  reportMaterialCategoryOptions,
  uploadMaterialAssetApi
} from '@/api/file'
import type { MaterialAsset } from '@/api/file'
import type { UploadCustomRequestOptions } from 'naive-ui'

type SelectorMode = 'single' | 'multiple'

const props = defineProps({
  value: {
    type: [String, Array],
    default: ''
  },
  mode: {
    type: String as () => SelectorMode,
    default: 'single'
  },
  buttonText: {
    type: String,
    default: '从素材库选择'
  },
  showPreview: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['confirm', 'clear'])

const showModal = ref(false)
const showUploadDrawer = ref(false)
const loading = ref(false)
const uploading = ref(false)
const keyword = ref('')
const activeCategory = ref('all')
const activeVisibility = ref<boolean | undefined>(undefined)
const uploadCategory = ref('background')
const uploadVisibility = ref(true)
const materials = ref<MaterialAsset[]>([])
const tempSelected = ref<string[]>([])
const pagination = ref({
  pageNum: 1,
  pageSize: 18,
  total: 0
})

const categoryOptions = reportMaterialCategoryOptions
const uploadCategoryOptions = reportMaterialCategoryOptions.filter(item => item.value !== 'all')

const visibilityOptions = [
  { get label() { return uiText("全部可见") }, value: undefined },
  { get label() { return uiText("私有") }, value: true },
  { get label() { return uiText("公共") }, value: false }
]

const uploadVisibilityOptions = [
  { get label() { return uiText("私有") }, value: true },
  { get label() { return uiText("公共") }, value: false }
]

const normalizedValue = computed(() => {
  if (Array.isArray(props.value)) {
    return props.value.filter(Boolean)
  }
  return props.value ? [props.value] : []
})

const syncSelection = () => {
  tempSelected.value = [...normalizedValue.value]
}

watch(() => props.value, syncSelection, { immediate: true, deep: true })

const getCategoryLabel = (value?: string) => {
  const target = categoryOptions.find(item => item.value === value)
  return target?.label || '未分类'
}

const formatFileSize = (size?: number) => {
  if (!size)
    return '0 B'
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

const openAndFetch = async () => {
  syncSelection()
  pagination.value.pageNum = 1
  await fetchMaterials()
}

const isSelected = (fileId: string) => {
  const refValue = createFileAssetRef(fileId)
  return tempSelected.value.includes(refValue)
}

const toggleAsset = (fileId: string) => {
  const refValue = createFileAssetRef(fileId)
  if (props.mode === 'single') {
    tempSelected.value = [refValue]
    confirmSelection()
    return
  }

  if (tempSelected.value.includes(refValue)) {
    tempSelected.value = tempSelected.value.filter(item => item !== refValue)
  } else {
    tempSelected.value = [...tempSelected.value, refValue]
  }
}

const handleCategoryChange = async () => {
  pagination.value.pageNum = 1
  if (activeCategory.value !== 'all') {
    uploadCategory.value = activeCategory.value
  }
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

const confirmSelection = () => {
  const value = props.mode === 'single' ? (tempSelected.value[0] || '') : [...tempSelected.value]
  emit('confirm', value)
  showModal.value = false
}

const emitClear = () => {
  emit('clear')
}

const handleUploadMaterial = async (options: UploadCustomRequestOptions) => {
  const rawFile = options.file.file
  if (!rawFile) {
    return
  }
  uploading.value = true
  try {
    const res = await uploadMaterialAssetApi(rawFile as File, uploadCategory.value, uploadVisibility.value)
    if (res.code !== 200 || !res.data?.fileId) {
      throw new Error(res.msg || uiText("素材上传失败"))
    }
    const refValue = createFileAssetRef(res.data.fileId)
    options.onFinish?.()
    if (activeCategory.value !== 'all' && activeCategory.value !== uploadCategory.value) {
      activeCategory.value = uploadCategory.value
    }
    pagination.value.pageNum = 1
    await fetchMaterials()
    if (props.mode === 'single') {
      tempSelected.value = [refValue]
      confirmSelection()
    } else if (!tempSelected.value.includes(refValue)) {
      tempSelected.value = [...tempSelected.value, refValue]
    }
    window['$message']?.success(uiText("素材上传成功"))
  } catch (error: any) {
    options.onError?.()
    window['$message']?.error(error?.message || uiText("素材上传失败"))
  } finally {
    uploading.value = false
  }
}
</script>

<style lang="scss" scoped>
.material-selector {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.selector-preview {
  min-height: 74px;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid rgba(var(--app-theme-rgb), 0.1);
  background: rgba(var(--app-theme-rgb), 0.03);
  overflow: hidden;

  &.empty {
    border-style: dashed;
  }
}

.preview-empty {
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  opacity: 0.7;
}

.preview-single {
  width: 100%;
  height:80px;
  overflow: hidden;
  border-radius: 10px;
  background: rgba(var(--app-theme-rgb), 0.04);
}

.preview-single :deep(.preview-single-image) {
  width: 100%;
  height: 100% !important;
  display: block;
  object-fit: contain !important;
}

.preview-multi {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-chip {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  overflow: hidden;
  background: rgba(var(--app-theme-rgb), 0.04);
}

.preview-chip :deep(.preview-chip-image) {
  width: 100%;
  height: 100% !important;
  display: block;
  object-fit: contain !important;
}

.preview-count {
  font-size: 12px;
  min-width: 36px;
  text-align: center;
}

.selector-actions {
  display: flex;
  gap: 8px;
}

.selector-modal {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modal-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px 130px auto;
  gap: 10px;
}

.selected-hint {
  font-size: 12px;
  color: rgba(var(--app-theme-rgb), 0.82);
}

.selector-upload {
  display: flex;
  align-items: center;
  padding: 6px 0;
  border-top: 1px solid rgba(148, 163, 184, 0.08);
}

.upload-drawer-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.upload-drawer-tip {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.72);
}

.upload-category {
  width: 140px;
}

.selector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  max-height: 56vh;
  overflow-y: auto;
}

.asset-card {
  color: rgba(226, 232, 240, 0.92);
  border: 1px solid rgba(var(--app-theme-rgb), 0.08);
  background: rgba(var(--app-theme-rgb), 0.03);
  border-radius: 14px;
  overflow: hidden;
  text-align: left;
  padding: 0;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(var(--app-theme-rgb), 0.22);
  }

  &.active {
    border-color: rgba(var(--app-theme-rgb), 0.44);
    box-shadow: 0 0 0 1px rgba(var(--app-theme-rgb), 0.25);
  }
}

.asset-image {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  cursor: pointer;
  background: rgba(var(--app-theme-rgb), 0.04);
}

.asset-image :deep(.asset-image-inner) {
  width: 100%;
  height: 100% !important;
  display: block;
  object-fit: contain !important;
}

.asset-body {
  padding: 10px;
}

.asset-top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.asset-size {
  margin-left: auto;
  font-size: 11px;
  color: rgba(148, 163, 184, 0.9);
}

.asset-name {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(226, 232, 240, 0.94);
  min-height: 36px;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

.selector-empty {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 900px) {
  .modal-toolbar {
    grid-template-columns: 1fr;
  }

  .modal-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
