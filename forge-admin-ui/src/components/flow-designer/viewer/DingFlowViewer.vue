<script setup>
/**
 * DingFlowViewer — 钉钉样式流程查看器
 *
 * 接口与 ProcessDiagramViewer 兼容：
 *   Props:
 *     - processInstanceId   流程实例 ID
 *     - compact             紧凑模式（顶部信息条 + 图例可隐藏）
 *     - bpmnXml             直接传入 BPMN XML（跳过自动 fetch）
 *     - nodeInstanceList    直接传入节点实例信息（跳过自动 fetch）
 *     - nodes               兼容后端 ProcessDiagramInfo.nodes
 *     - nodeStatuses        兼容旧版 nodeStatuses 映射/数组
 *
 * 内部行为：
 *   - 当 processInstanceId 变化时，调用 flowApi.getProcessDiagramInfo 获取 bpmnXml + nodes
 *   - 把 nodes/nodeInstanceList/nodeStatuses 转换为 nodeStatusMap = { [bpmnElementId]: { status, ... } }
 *   - 用 DingFlowDesigner readonly 模式渲染（status 通过 NodeRenderer 透传到 NodeCard）
 *   - 节点点击 → 弹出 NodeDetailPopover 展示该节点的运行实例信息
 *
 * 因为 NodeRenderer 已支持 status props，本组件主要负责数据获取与状态映射。
 */
import { computed, nextTick, ref, watch } from 'vue'
import flowApi from '@/api/flow'
import { localizeLabel, t } from '@/locales'
import {
  EdgeLayer,
  FlowCanvas,
  layoutFlow,
  NodeRenderer,
} from '../canvas/index.js'
import { useFlowDesigner } from '../composables/index.js'
import { convertBpmnToJson } from '../converter/index.js'
import NodeDetailPopover from './NodeDetailPopover.vue'

const props = defineProps({
  processInstanceId: { type: [String, Number], default: null },
  compact: { type: Boolean, default: false },
  bpmnXml: { type: String, default: '' },
  nodeInstanceList: { type: Array, default: null },
  nodes: { type: Array, default: null },
  nodeStatuses: { type: [Array, Object], default: null },
})

const emit = defineEmits(['ready', 'error'])

const designer = useFlowDesigner()
const loading = ref(false)
const renderError = ref(null)
const canvasRef = ref(null)

const diagramInfo = ref({
  bpmnXml: '',
  status: null,
  startUserName: '',
  startTime: null,
  endTime: null,
  nodeInstanceList: [],
})

const nodeStatusMap = computed(() => {
  const m = {}
  for (const inst of diagramInfo.value.nodeInstanceList || []) {
    const nodeId = resolveNodeId(inst)
    if (!nodeId)
      continue
    const normalized = normalizeNodeInstance(inst, nodeId)
    const existing = m[nodeId]
    if (!existing || timeValue(normalized.endTime || normalized.startTime) >= timeValue(existing.endTime || existing.startTime))
      m[nodeId] = normalized
  }
  return m
})

const layoutResult = ref({
  nodePositions: new Map(),
  edgePaths: new Map(),
  canvasBounds: { minX: 0, minY: 0, maxX: 0, maxY: 0 },
})

watch(designer.flowJson, () => {
  layoutResult.value = layoutFlow(designer.flowJson.value)
  scheduleFitView()
}, { immediate: true })

function scheduleFitView() {
  nextTick(() => {
    const run = () => fitViewerToScreen()
    if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function')
      window.requestAnimationFrame(run)
    else
      run()
  })
}

function fitViewerToScreen() {
  const canvas = canvasRef.value
  const containerRef = canvas?.containerRef
  const container = containerRef?.value || containerRef
  if (!canvas || !container)
    return

  const rect = container.getBoundingClientRect?.()
  const viewportW = rect?.width || container.clientWidth || 0
  const viewportH = rect?.height || container.clientHeight || 0
  const bounds = layoutResult.value.canvasBounds
  const contentW = Math.max((bounds?.maxX || 0) - (bounds?.minX || 0), 1)
  const contentH = Math.max((bounds?.maxY || 0) - (bounds?.minY || 0), 1)
  if (!viewportW || !viewportH || !contentW || !contentH)
    return

  canvas.fitToScreen(
    contentW,
    contentH,
    viewportW,
    viewportH,
    props.compact ? 28 : 44,
    bounds.minX || 0,
    bounds.minY || 0,
  )
}

/* ---------- 数据加载 ---------- */

async function loadFromApi(processInstanceId) {
  if (!processInstanceId)
    return
  loading.value = true
  renderError.value = null
  try {
    const res = await flowApi.getProcessDiagramInfo(processInstanceId)
    if (res?.code === 200 || res?.code === 0 || res?.data) {
      const data = res.data || res
      diagramInfo.value = normalizeDiagramInfo(data)
      applyXml(diagramInfo.value.bpmnXml)
      emit('ready', diagramInfo.value)
    }
    else {
      throw new Error(res?.message || localizeLabel('加载流程图失败'))
    }
  }
  catch (err) {
    renderError.value = err
    console.error('[DingFlowViewer] 加载流程图失败', err)
    emit('error', err)
  }
  finally {
    loading.value = false
  }
}

function normalizeDiagramInfo(data = {}) {
  return {
    bpmnXml: data.bpmnXml || data.xml || data.processXml || '',
    status: normalizeProcessStatus(data.status),
    startUserName: data.startUserName || data.initiatorName || '',
    startTime: data.startTime || null,
    endTime: data.endTime || null,
    nodeInstanceList: normalizeNodeList(extractNodeInstances(data)),
  }
}

function extractNodeInstances(data = {}) {
  if (Array.isArray(data.nodeInstanceList) && data.nodeInstanceList.length > 0)
    return data.nodeInstanceList
  if (Array.isArray(data.nodes) && data.nodes.length > 0)
    return data.nodes
  if (Array.isArray(data.nodeList) && data.nodeList.length > 0)
    return data.nodeList
  if (Array.isArray(data.activities) && data.activities.length > 0)
    return data.activities
  if (Array.isArray(data.nodeStatuses) && data.nodeStatuses.length > 0)
    return data.nodeStatuses
  if (data.nodeStatuses && typeof data.nodeStatuses === 'object')
    return objectStatusMapToList(data.nodeStatuses)
  if (Array.isArray(data.nodeInstanceList))
    return data.nodeInstanceList
  if (Array.isArray(data.nodes))
    return data.nodes
  if (Array.isArray(data.nodeList))
    return data.nodeList
  if (Array.isArray(data.activities))
    return data.activities
  if (Array.isArray(data.nodeStatuses))
    return data.nodeStatuses
  return []
}

function objectStatusMapToList(statusMap) {
  return Object.entries(statusMap).map(([nodeId, info]) => ({
    ...(info && typeof info === 'object' ? info : { status: info }),
    nodeId: info?.nodeId || info?.activityId || nodeId,
  }))
}

function normalizeNodeList(list) {
  if (Array.isArray(list))
    return list.map(item => normalizeNodeInstance(item)).filter(item => item.nodeId)
  if (list && typeof list === 'object')
    return normalizeNodeList(objectStatusMapToList(list))
  return []
}

function normalizeNodeInstance(inst = {}, fallbackNodeId = '') {
  const nodeId = fallbackNodeId || resolveNodeId(inst)
  const assigneeName = resolveAssigneeName(inst)
  return {
    ...inst,
    nodeId,
    status: normalizeNodeStatus(inst.status),
    assigneeName,
    startTime: inst.startTime || inst.createTime || inst.beginTime || null,
    endTime: inst.endTime || inst.completeTime || inst.finishTime || null,
    result: normalizeResult(inst.result || inst.approveResult),
    comment: inst.comment || inst.message || '',
  }
}

function resolveNodeId(inst = {}) {
  return inst.nodeId
    || inst.activityId
    || inst.taskDefinitionKey
    || inst.bpmnElementId
    || inst.elementId
    || inst.id
    || ''
}

function resolveAssigneeName(inst = {}) {
  if (inst.assigneeName)
    return inst.assigneeName
  if (Array.isArray(inst.assigneeNames) && inst.assigneeNames.length > 0)
    return inst.assigneeNames.filter(Boolean).join('、')
  if (Array.isArray(inst.candidateUserNames) && inst.candidateUserNames.length > 0)
    return inst.candidateUserNames.filter(Boolean).join('、')
  return inst.assignee || inst.userName || ''
}

function normalizeNodeStatus(status) {
  const value = String(status || '').trim().toLowerCase()
  if (!value)
    return null
  const statusMap = {
    active: 'running',
    processing: 'running',
    in_progress: 'running',
    inprogress: 'running',
    wait: 'pending',
    waiting: 'pending',
    todo: 'pending',
    finished: 'completed',
    complete: 'completed',
    approved: 'completed',
    approve: 'completed',
    reject: 'rejected',
    denied: 'rejected',
    terminated: 'skipped',
    canceled: 'skipped',
    cancelled: 'skipped',
    withdrawn: 'skipped',
  }
  return statusMap[value] || value
}

function normalizeProcessStatus(status) {
  const value = String(status || '').trim().toLowerCase()
  if (!value)
    return null
  const statusMap = {
    active: 'running',
    started: 'running',
    finished: 'completed',
    complete: 'completed',
    approved: 'completed',
    canceled: 'terminated',
    cancelled: 'terminated',
    withdrawn: 'terminated',
  }
  return statusMap[value] || value
}

function normalizeResult(result) {
  const value = String(result || '').trim().toLowerCase()
  if (!value)
    return ''
  if (['reject', 'rejected', 'denied'].includes(value))
    return 'rejected'
  if (['approve', 'approved', 'pass', 'passed', 'completed'].includes(value))
    return 'approved'
  return value
}

function timeValue(value) {
  if (!value)
    return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

function applyXml(xml) {
  if (!xml) {
    designer.reset()
    return
  }
  try {
    const json = convertBpmnToJson(xml)
    designer.loadJson(json)
  }
  catch (err) {
    console.error('[DingFlowViewer] BPMN XML 解析失败', err)
    renderError.value = err
  }
}

/* ---------- 数据来源切换 ---------- */

watch(
  () => [props.processInstanceId, props.bpmnXml, props.nodeInstanceList, props.nodes, props.nodeStatuses],
  ([pid, xml, list, nodes, statuses]) => {
    if (xml) {
      // 直接给入 XML 模式
      diagramInfo.value = normalizeDiagramInfo({
        bpmnXml: xml,
        status: null,
        startUserName: '',
        startTime: null,
        endTime: null,
        nodeInstanceList: list || [],
        nodes: nodes || [],
        nodeStatuses: statuses,
      })
      applyXml(xml)
    }
    else if (pid) {
      loadFromApi(pid)
    }
  },
  { immediate: true },
)

/* ---------- 节点点击 → 弹出详情 ---------- */

const popover = ref({ visible: false, position: { x: 0, y: 0 }, node: null })

function handleNodeClick(node) {
  designer.selectedNodeId.value = node.id
  // 用节点屏幕位置作为气泡定位（这里用一个相对粗略的位置：节点右侧 + 20px）
  // 真实位置需结合 viewport 缩放，但 jsdom + 简单实现先够用
  const target = document.querySelector(`[data-node-id="${node.id}"]`)
  if (target) {
    const rect = target.getBoundingClientRect()
    popover.value = {
      visible: true,
      position: { x: rect.right + 12, y: rect.top },
      node,
    }
  }
  else {
    popover.value = { visible: true, position: { x: 100, y: 100 }, node }
  }
}

function popoverTaskInfo(node) {
  const inst = getNodeStatus(node)
  if (!inst)
    return null
  return {
    status: inst.status,
    assigneeName: inst.assigneeName || inst.assignee,
    startTime: inst.startTime,
    endTime: inst.endTime,
    result: inst.result,
    comment: inst.comment,
  }
}

const popoverTaskInfoComputed = computed(() => popoverTaskInfo(popover.value.node))

function getNodeStatus(node) {
  if (!node)
    return null
  return nodeStatusMap.value[node.id] || nodeStatusMap.value[node.bpmnElementId] || null
}

/* ---------- 流程总状态文案 ---------- */

const STATUS_TEXT = {
  running: { label: '审批中', type: 'info' },
  completed: { label: '已完成', type: 'success' },
  terminated: { label: '已终止', type: 'default' },
  canceled: { label: '已取消', type: 'default' },
  cancelled: { label: '已取消', type: 'default' },
  rejected: { label: '已驳回', type: 'error' },
}

const overallStatus = computed(() => {
  const s = diagramInfo.value.status
  if (!s)
    return null
  return STATUS_TEXT[s] || { label: String(s), type: 'default' }
})

defineExpose({
  designer,
  diagramInfo,
  loading,
  renderError,
  nodeStatusMap,
  /** 给单测 / 外部 ref 用，便于手动切换 BPMN XML */
  applyXml,
  loadFromApi,
  fitViewerToScreen,
})
</script>

<template>
  <div class="ding-flow-viewer relative h-full w-full" :class="{ compact }">
    <!-- 顶部状态条 -->
    <div v-if="!compact && overallStatus" class="text-sm flex items-center gap-3 px-4 py-2">
      <span
        class="text-xs inline-flex items-center rounded px-2 py-0.5"
        :class="{
          'bg-info/10 text-info': overallStatus.type === 'info',
          'bg-success/10 text-success': overallStatus.type === 'success',
          'bg-error/10 text-error': overallStatus.type === 'error',
          'bg-gray-100 text-gray-500': overallStatus.type === 'default',
        }"
      >
        {{ localizeLabel(overallStatus.label) }}
      </span>
      <span v-if="diagramInfo.startUserName" class="text-gray-500">
        {{ t('workflowViewer.initiator', { name: diagramInfo.startUserName }) }}
      </span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-sm absolute inset-0 z-10 flex items-center justify-center bg-white/60 text-gray-400">
      {{ localizeLabel('正在加载流程图…') }}
    </div>

    <!-- 渲染失败 -->
    <div v-if="renderError && !loading" class="text-sm flex items-center justify-center p-8 text-error">
      {{ t('workflowViewer.loadError', { error: renderError.message || renderError }) }}
    </div>

    <!-- 钉钉风格画布（readonly） -->
    <FlowCanvas
      v-if="!renderError"
      ref="canvasRef"
      readonly
      allow-navigation
      :min-scale="0.2"
      :initial-scale="compact ? 0.9 : 1"
    >
      <template #edges>
        <EdgeLayer
          :edges="designer.flowJson.value.edges"
          :paths="layoutResult.edgePaths"
          :canvas-bounds="layoutResult.canvasBounds"
          :node-statuses="nodeStatusMap"
        />
      </template>
      <template #nodes>
        <NodeRenderer
          v-for="node in designer.flowJson.value.nodes"
          :key="node.id"
          :node="node"
          :position="layoutResult.nodePositions.get(node.id)"
          :selected="designer.selectedNodeId.value === node.id"
          :status="getNodeStatus(node)?.status"
          :readonly="true"
          :outgoing-count="designer.getOutgoingEdges(node.id).length"
          @click="handleNodeClick"
        />
      </template>
    </FlowCanvas>

    <!-- 节点详情气泡 -->
    <NodeDetailPopover
      :visible="popover.visible"
      :position="popover.position"
      :node="popover.node"
      :task-info="popoverTaskInfoComputed"
      @close="popover.visible = false"
    />
  </div>
</template>

<style scoped>
.ding-flow-viewer {
  min-height: 420px;
}

.ding-flow-viewer.compact {
  min-height: 360px;
}

.ding-flow-viewer :deep(.flow-canvas) {
  min-height: 420px;
}

.ding-flow-viewer.compact :deep(.flow-canvas) {
  min-height: 360px;
}
</style>
