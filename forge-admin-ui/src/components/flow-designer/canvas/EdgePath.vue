<script setup>
import { localizeLabel as translateUiText, formatUiText } from '@/locales'

/**
 * EdgePath — 单条 SVG 边
 *
 * Props:
 *   - edge       flowJson edge 对象（含 condition / isDefault）
 *   - path       { points: [{x,y}], type } — 来自 layoutFlow
 *   - status     节点/边状态：completed / running / pending / rejected / skipped
 *   - pathType   覆盖 path.type，可选 'rounded' / 'bezier' / 'orthogonal' / 'straight'
 *   - showLabel  是否显示条件文本（默认按 edge.condition 判断）
 *
 * 通过 marker-end 添加箭头；条件文本贴在路径中点。
 */
import { computed } from 'vue'
import {
  buildPathD,
  getEdgeColor,
  getEdgeDashArray,
  getEdgeMidpoint,
} from '../utils/path-builder.js'

const props = defineProps({
  edge: { type: Object, required: true },
  path: { type: Object, required: true },
  status: { type: String, default: null },
  pathType: { type: String, default: null }, // 覆盖 path.type
  showLabel: { type: Boolean, default: null },
})

const finalType = computed(() => props.pathType || props.path?.type || 'orthogonal')

const pathD = computed(() => buildPathD(props.path?.points || [], finalType.value))
const strokeColor = computed(() => getEdgeColor(props.edge, props.status))
const dashArray = computed(() => getEdgeDashArray(props.status))
const midpoint = computed(() => getEdgeMidpoint(props.path?.points || []))

const labelVisible = computed(() => {
  if (props.showLabel != null)
    return props.showLabel
  if (props.edge?.branchId)
    return false
  return Boolean(props.edge?.condition || props.edge?.isDefault)
})

const labelText = computed(() => {
  if (props.edge?.isDefault)
    return translateUiText("默认")
  if (!props.edge?.condition)
    return ''
  const ruleCount = Array.isArray(props.edge?.conditionRules) ? props.edge.conditionRules.length : 0
  return ruleCount > 1 ? formatUiText("{0} 条条件", [ruleCount]) : translateUiText("条件已设")
})

const labelWidth = computed(() => Math.min(Math.max(labelText.value.length * 8 + 22, 58), 176))

// 选取与状态一致的 marker
const markerId = computed(() => {
  switch (props.status) {
    case 'completed': return 'arrow-completed'
    case 'rejected': return 'arrow-rejected'
    case 'running': return 'arrow-running'
    default: return props.edge?.isDefault ? 'arrow-default-branch' : 'arrow-default'
  }
})
</script>

<template>
  <g class="edge-path">
    <path
      :d="pathD"
      :stroke="strokeColor"
      stroke-width="1.7"
      fill="none"
      :stroke-dasharray="dashArray || undefined"
      :marker-end="`url(#${markerId})`"
    />
    <g v-if="labelVisible && labelText" :transform="`translate(${midpoint.x}, ${midpoint.y})`">
      <rect
        :x="`${-labelWidth / 2}`" y="-10" :width="labelWidth" height="20"
        rx="6" ry="6"
        fill="#fff"
        :stroke="strokeColor"
        stroke-width="1"
      />
      <text
        x="0" y="0"
        text-anchor="middle"
        dominant-baseline="middle"
        font-size="11"
        :fill="strokeColor"
      >
        {{ labelText }}
      </text>
    </g>
  </g>
</template>
