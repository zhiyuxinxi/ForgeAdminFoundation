<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * NodeConfigDrawer — 节点配置抽屉容器
 *
 * 职责：
 * - 右侧滑出抽屉（Naive UI NDrawer）
 * - 顶部：轻量节点信息 + 关闭按钮
 * - 中部：根据 nodeType 调度对应配置 Tab（通过 panel/index 注册的 RENDERER_MAP）
 * - 底部：取消 / 保存按钮（保存时触发 emit('save', patch)）
 *
 * Props:
 *   - visible        v-model:visible
 *   - node           当前编辑节点（外层 useFlowDesigner.getNode 取）
 *   - outgoingEdges  当前节点出边，网关配置分支条件时使用
 *   - nodes          当前流程节点数组，用于网关配置显示下游节点名称
 *   - formAssetOptions 节点可选表单资产，业务应用进入时来自应用表单资产
 *   - formFieldCatalog 流程动态表单字段目录，条件分支配置时用于生成表达式
 *   - focusEdgeId    从画布分支标签进入时聚焦的分支边 ID
 *   - readonly       只读模式（查看器场景）
 *   - width          抽屉宽度，默认 520
 *
 * Events:
 *   - update:visible (boolean)
 *   - save           (patch: object)  外层 useFlowDesigner.updateNode(node.id, patch)
 *   - update:edge    (edgeId, patch)  外层 useFlowDesigner.updateEdge(edgeId, patch)
 *
 * 实现策略：
 * - 内部维护 draftNode（深拷贝 props.node），用户修改不直接影响外层；点击 "保存" 才 emit
 * - 取消按钮 / 关闭按钮：丢弃 draft，emit('update:visible', false)
 * - 节点 name 编辑通过 BasicConfig 组件；审批人节点为了贴近配置页签体验，基础属性内嵌到“审批人设置”
 */
import { computed, ref, watch } from 'vue'
import FlowPropertyPanelShell from '@/components/flow/FlowPropertyPanelShell.vue'
import BasicConfig from '../panel/BasicConfig.vue'
import { CONFIG_RENDERER_MAP } from '../panel/config-renderer-map.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null },
  outgoingEdges: { type: Array, default: () => [] },
  nodes: { type: Array, default: () => [] },
  formAssetOptions: { type: Array, default: () => [] },
  formFieldCatalog: { type: Array, default: () => [] },
  focusEdgeId: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
  width: { type: Number, default: 520 },
})

const emit = defineEmits(['update:visible', 'save', 'update:edge'])

const draftNode = ref(null)

watch(
  () => [props.visible, props.node],
  ([v, n]) => {
    if (v && n)
      draftNode.value = cloneDeep(n)
    else if (!v)
      draftNode.value = null
  },
  { immediate: true },
)

const ConfigComponent = computed(() => {
  if (!draftNode.value)
    return null
  return CONFIG_RENDERER_MAP[draftNode.value.nodeType] || null
})

const configExtraProps = computed(() => {
  if (draftNode.value?.nodeType === 'approver') {
    return {
      formAssetOptions: props.formAssetOptions,
      formFieldCatalog: props.formFieldCatalog,
    }
  }
  if (!['condition', 'parallel', 'inclusive'].includes(draftNode.value?.nodeType))
    return {}
  return {
    outgoingEdges: props.outgoingEdges,
    nodes: props.nodes,
    formFieldCatalog: props.formFieldCatalog,
    focusEdgeId: props.focusEdgeId,
  }
})

const useIntegratedBasicConfig = computed(() => draftNode.value?.nodeType === 'approver')

const headerIcon = computed(() => {
  const map = {
    start: 'i-mdi-flag-variant-outline',
    end: 'i-mdi-flag-checkered',
    approver: 'i-mdi-account-check',
    carbonCopy: 'i-mdi-email-outline',
    condition: 'i-mdi-source-branch',
    parallel: 'i-mdi-call-split',
    inclusive: 'i-mdi-set-merge',
    service: 'i-mdi-cog-outline',
    script: 'i-mdi-code-tags',
    subProcess: 'i-mdi-sitemap-outline',
    callActivity: 'i-mdi-phone-forward-outline',
    advanced: 'i-mdi-shield-alert-outline',
  }
  return map[draftNode.value?.nodeType] || 'i-mdi-square-outline'
})

function handleClose() {
  emit('update:visible', false)
}

function buildDraftPatch() {
  if (!draftNode.value)
    return null
  return {
    name: draftNode.value.name,
    config: draftNode.value.config,
  }
}

function commitDraft() {
  if (props.readonly)
    return false
  const patch = buildDraftPatch()
  if (!patch || !draftNode.value)
    return false
  emit('save', patch, draftNode.value.id)
  return true
}

function handleSave() {
  if (!commitDraft())
    return
  emit('update:visible', false)
}

function updateConfig(partial) {
  if (!draftNode.value)
    return
  draftNode.value.config = { ...draftNode.value.config, ...partial }
}

function updateNode(nextNode) {
  draftNode.value = nextNode
}

function updateEdge(edgeId, patch) {
  emit('update:edge', edgeId, patch)
}

function cloneDeep(v) {
  return JSON.parse(JSON.stringify(v))
}

defineExpose({
  commitDraft,
})
</script>

<template>
  <n-drawer
    :show="visible"
    :width="width"
    placement="right"
    :mask-closable="!readonly"
    @update:show="$emit('update:visible', $event)"
  >
    <n-drawer-content
      class="node-config-drawer"
      :native-scrollbar="false"
      :closable="false"
    >
      <FlowPropertyPanelShell
        :title="draftNode?.name || (readonly ? translateUiText(&quot;查看节点&quot;) : translateUiText(&quot;配置节点&quot;))"
        :description="translateUiText(&quot;节点属性配置&quot;)"
        :icon="headerIcon"
        :empty="!draftNode"
        :empty-text="translateUiText(&quot;请先选择一个节点。&quot;)"
        @close="handleClose"
      >
        <BasicConfig
          v-if="draftNode && !useIntegratedBasicConfig"
          v-model:node="draftNode"
          :readonly="readonly"
        />
        <component
          :is="ConfigComponent"
          v-if="draftNode && ConfigComponent"
          :node="draftNode"
          v-bind="configExtraProps"
          :readonly="readonly"
          @update:config="updateConfig"
          @update:node="updateNode"
          @update:edge="updateEdge"
        />
        <div v-else-if="draftNode" class="node-config-empty">
          {{ translateUiText("该节点类型暂无可配置项。") }}
        </div>

        <template #footer>
          <div class="flex justify-end gap-2">
            <n-button @click="handleClose">
              {{ readonly ? translateUiText("关闭") : translateUiText("取消") }}
            </n-button>
            <n-button v-if="!readonly" type="primary" @click="handleSave">
              {{ translateUiText("保存") }}
            </n-button>
          </div>
        </template>
      </FlowPropertyPanelShell>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
:deep(.node-config-drawer .n-drawer-body-content-wrapper) {
  height: 100%;
  padding: 0;
}

.node-config-empty {
  margin: 18px;
  border-radius: 8px;
  padding: 16px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 13px;
}
</style>
