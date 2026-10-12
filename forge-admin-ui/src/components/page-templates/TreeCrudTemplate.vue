<template>
  <!--
    TreeCrudTemplate - 树形 CRUD 页面模板
    适合具有父子层级结构的数据（部门、分类等）
    布局：左侧树 + 右侧标准 CRUD 表格
    当 treeConfig 未配置时，降级为标准 CRUD
  -->
  <MasterDetailWorkspace v-if="hasTree && !crudProps.formOnly" :aside-width="220" class="tree-crud-layout">
    <template #aside>
      <!-- 左侧树形导航 -->
      <div class="tree-crud-left">
        <div class="tree-header">
          <span>{{ treeTitle }}</span>
          <n-button text size="tiny" @click="clearTreeSelect">
            {{ localizeLabel('全部') }}
          </n-button>
        </div>
        <n-spin :show="treeLoading || loadingNodeKeys.length > 0" class="tree-crud-content">
          <PremiumTree
            :data="normalizedTreeData"
            key-field="key"
            label-field="label"
            children-field="children"
            :expanded-keys="expandedKeys"
            :selected-keys="selectedKeys"
            @update:selected-keys="handleTreeSelect"
            @update:expanded-keys="handleTreeExpand"
          />
        </n-spin>
      </div>
    </template>
    <!-- 右侧 CRUD 表格 -->
    <div class="tree-crud-right">
      <AiCrudPage
        ref="crudRef"
        v-bind="mergedCrudProps"
        @submit-success="loadTreeData"
        @delete="loadTreeData"
      />
    </div>
  </MasterDetailWorkspace>
  <!-- 无树形配置时降级为标准 CRUD -->
  <AiCrudPage v-else ref="crudRef" v-bind="crudProps" />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'

import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import PremiumTree from '@/components/common/PremiumTree.vue'
import { localizeLabel } from '@/locales'
import { request } from '@/utils'

const props = defineProps({
  crudProps: {
    type: Object,
    required: true,
  },
})

// 从 options 中读取 treeConfig
const treeConfig = computed(() => {
  return props.crudProps?.options?.treeConfig || null
})

const hasTree = computed(() => !!treeConfig.value)
const treeTitle = computed(() => treeConfig.value?.treeTitle || localizeLabel('树形分类'))
const treeLoadMode = computed(() => treeConfig.value?.loadMode === 'lazy' ? 'lazy' : 'full')
const isLazyTree = computed(() => treeLoadMode.value === 'lazy')

const treeLoading = ref(false)
const treeData = ref([])
const selectedKeys = ref([])
const expandedKeys = ref([])
const loadingNodeKeys = ref([])
const selectedId = ref(null)
const crudRef = ref(null)
const normalizedTreeData = computed(() => treeData.value)

// 合并 crudProps，注入 parentId 过滤
const mergedCrudProps = computed(() => {
  const filterField = treeConfig.value?.filterField || treeConfig.value?.parentField || 'parentId'
  const includeChildren = treeConfig.value?.includeChildren !== false
  const publicParams = { ...(props.crudProps.publicParams || {}) }
  if (selectedId.value !== null && selectedId.value !== undefined && selectedId.value !== '') {
    publicParams[filterField] = selectedId.value
    if (includeChildren) {
      publicParams[`${filterField}_includeChildren`] = true
    }
  }

  return {
    ...props.crudProps,
    publicParams,
    beforeRenderForm: buildBeforeRenderForm(filterField),
  }
})

function buildBeforeRenderForm(filterField) {
  return async (row) => {
    const originalHook = props.crudProps.beforeRenderForm
    const originalData = typeof originalHook === 'function' ? await originalHook(row) : null
    const nextData = row && typeof row === 'object' ? { ...row } : {}
    if (originalData && typeof originalData === 'object') {
      Object.assign(nextData, originalData)
    }
    if (selectedId.value !== null && selectedId.value !== undefined && selectedId.value !== '' && !row) {
      nextData[filterField] = selectedId.value
    }
    return nextData
  }
}

async function loadTreeData() {
  if (!treeConfig.value)
    return
  const apiConfig = props.crudProps?.apiConfig || {}
  // 优先使用 tree 接口，fallback 到 list 接口
  const treeApi = apiConfig.tree || apiConfig.list
  if (!treeApi)
    return
  const [method, url] = treeApi.split('@')
  if (!url)
    return
  treeLoading.value = true
  try {
    const res = await request({
      method: method.toLowerCase(),
      url,
      params: {
        ...resolveTreeSortParams(),
        loadMode: treeLoadMode.value,
      },
    })
    if (res.code === 200) {
      treeData.value = normalizeTreeNodes(res.data || [])
      if (!isLazyTree.value && !expandedKeys.value.length)
        expandedKeys.value = collectExpandedKeys(treeData.value)
    }
  }
  catch (e) {
    console.warn('[TreeCrudTemplate] 加载树形数据失败:', e.message)
  }
  finally {
    treeLoading.value = false
  }
}

async function handleLoadTreeNode(node) {
  const apiConfig = props.crudProps?.apiConfig || {}
  const treeApi = apiConfig.tree || apiConfig.list
  if (!treeApi || !node)
    return
  const [method, url] = treeApi.split('@')
  if (!url)
    return
  const config = treeConfig.value || {}
  const parentValue = node?.[config.keyField || 'id'] ?? node?.key ?? node?.targetValue
  try {
    const res = await request({
      method: method.toLowerCase(),
      url,
      params: {
        ...resolveTreeSortParams(),
        loadMode: 'lazy',
        parentValue,
      },
    })
    if (res?.code !== 200)
      return false
    node.children = normalizeTreeNodes(res?.data || [])
    if (!node.children.length)
      node.isLeaf = true
    return true
  }
  catch (error) {
    console.warn('[TreeCrudTemplate] 加载树形子节点失败:', error.message)
    return false
  }
}

function collectExpandedKeys(nodes = []) {
  return nodes.flatMap(node => node.children?.length ? [node.key, ...collectExpandedKeys(node.children)] : [])
}

async function handleTreeExpand(keys, node) {
  const expanding = keys.includes(node.key)
  if (expanding && isLazyTree.value && node.isLeaf !== true && !node.children?.length) {
    if (loadingNodeKeys.value.includes(node.key))
      return
    loadingNodeKeys.value = [...loadingNodeKeys.value, node.key]
    const loaded = await handleLoadTreeNode(node)
    loadingNodeKeys.value = loadingNodeKeys.value.filter(key => key !== node.key)
    if (!loaded)
      return
  }
  expandedKeys.value = keys
}

function handleTreeSelect(keys, node) {
  selectedKeys.value = keys
  selectedId.value = resolveTreeTargetValue(Array.isArray(node) ? node[0] : node, keys[0])
}

function clearTreeSelect() {
  selectedKeys.value = []
  selectedId.value = null
}

function resolveTreeSortParams() {
  const params = props.crudProps?.publicParams || {}
  return {
    ...(params.orderByColumn ? { orderByColumn: params.orderByColumn } : {}),
    ...(params.isAsc ? { isAsc: params.isAsc } : {}),
  }
}

onMounted(() => {
  if (hasTree.value) {
    loadTreeData()
  }
})

watch(() => props.crudProps?.apiConfig, () => {
  if (hasTree.value) {
    loadTreeData()
  }
}, { deep: true })

function normalizeTreeNodes(nodes = []) {
  if (!Array.isArray(nodes))
    return []
  const config = treeConfig.value || {}
  const keyField = config.keyField || 'id'
  const childrenField = config.childrenField || 'children'
  return nodes.map((node) => {
    const children = Array.isArray(node?.[childrenField])
      ? node[childrenField]
      : Array.isArray(node?.children)
        ? node.children
        : []
    const normalized = {
      ...(node || {}),
      key: node?.key ?? node?.[keyField],
      label: resolveTreeLabel(node, config),
    }
    const normalizedChildren = normalizeTreeNodes(children)
    if (normalizedChildren.length) {
      normalized.children = normalizedChildren
      normalized.isLeaf = false
    }
    else if (node?.isLeaf !== undefined) {
      normalized.isLeaf = !!node.isLeaf
    }
    else if (isLazyTree.value) {
      normalized.isLeaf = false
    }
    return normalized
  })
}

function resolveTreeLabel(node, config) {
  const keyField = config.keyField || 'id'
  const fields = [
    config.labelField,
    'label',
    'name',
    'title',
    'treeName',
    'deptName',
    'orgName',
    keyField,
  ].filter(Boolean)
  for (const field of fields) {
    const value = node?.[field]
    if (value !== undefined && value !== null && String(value) !== '')
      return String(value)
  }
  return localizeLabel('未命名节点')
}

function resolveTreeTargetValue(node, fallbackKey) {
  if (!node)
    return fallbackKey ?? null
  const targetField = treeConfig.value?.targetField || treeConfig.value?.keyField || 'id'
  return node[targetField] ?? node.targetValue ?? node.key ?? fallbackKey ?? null
}

defineExpose({
  showAdd: (...args) => crudRef.value?.showAdd?.(...args),
  showDetail: (...args) => crudRef.value?.showDetail?.(...args),
  refresh: (...args) => crudRef.value?.refresh?.(...args),
})
</script>

<style scoped>
.tree-crud-layout {
  height: 100%;
  min-height: 0;
}

.tree-crud-left {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 12px 8px;
  overflow: hidden;
  background: var(--bg-primary);
}

.tree-crud-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  padding: 0 6px 10px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 8px;
}

.tree-crud-right {
  flex: 1;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}
</style>
