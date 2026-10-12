<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * NodeCard — 节点卡片基类
 *
 * 统一布局：标题行图标 + 节点名称，下面展示摘要面板。
 * 视觉参考钉钉审批流卡片：轻阴影、浅边框、摘要灰底、选中态青绿色描边。
 *
 * Props:
 *   - node / selected / status / readonly
 *   - icon        顶部图标（i-mdi-xxx）
 *   - colorVar    配色变量：primary / success / warning / info / error / gray
 *   - subtitle    副标题（节点说明，浅灰小字）
 *   - width / height
 *   - showActions 是否显示删除按钮
 *   - deletable   是否可删除（start/end 不可）
 */
import { computed } from 'vue'
import { localizeLabel } from '@/locales'

const props = defineProps({
  node: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  status: { type: String, default: null },
  readonly: { type: Boolean, default: false },
  icon: { type: String, default: 'i-mdi-checkbox-blank-circle' },
  colorVar: { type: String, default: 'primary' },
  subtitle: { type: String, default: '' },
  showActions: { type: Boolean, default: true },
  deletable: { type: Boolean, default: true },
  width: { type: Number, default: 300 },
  height: { type: Number, default: 104 },
})

const emit = defineEmits(['click', 'delete', 'contextMenu'])

const COLOR_META = {
  primary: { color: '#356dff', soft: 'rgba(53, 109, 255, 0.1)', shadow: 'rgba(53, 109, 255, 0.22)' },
  success: { color: '#1aa6a6', soft: 'rgba(26, 166, 166, 0.1)', shadow: 'rgba(26, 166, 166, 0.22)' },
  warning: { color: '#f59e0b', soft: 'rgba(245, 158, 11, 0.12)', shadow: 'rgba(245, 158, 11, 0.2)' },
  info: { color: '#3b82f6', soft: 'rgba(59, 130, 246, 0.1)', shadow: 'rgba(59, 130, 246, 0.2)' },
  error: { color: '#ef4444', soft: 'rgba(239, 68, 68, 0.1)', shadow: 'rgba(239, 68, 68, 0.2)' },
  gray: { color: '#a3aab4', soft: 'rgba(148, 163, 184, 0.14)', shadow: 'rgba(148, 163, 184, 0.18)' },
}

const STATUS_BADGE = {
  completed: { label: '已完成', class: 'bg-green-100 text-green-700' },
  running: { label: '审批中', class: 'bg-blue-100 text-blue-700' },
  pending: { label: '待办', class: 'bg-gray-100 text-gray-500' },
  rejected: { label: '已驳回', class: 'bg-red-100 text-red-700' },
  skipped: { label: '已跳过', class: 'bg-gray-100 text-gray-400' },
}

const colorMeta = computed(() => COLOR_META[props.colorVar] || COLOR_META.primary)
const cardStyle = computed(() => ({
  'width': `${props.width}px`,
  'minHeight': `${props.height}px`,
  '--flow-node-color': colorMeta.value.color,
  '--flow-node-soft': colorMeta.value.soft,
  '--flow-node-shadow': colorMeta.value.shadow,
}))
const statusBadge = computed(() => STATUS_BADGE[props.status] || null)
const canDelete = computed(() => props.deletable && props.showActions && !props.readonly)

function handleClick() {
  emit('click', props.node)
}

function handleDelete(e) {
  e.stopPropagation()
  if (canDelete.value)
    emit('delete', props.node)
}

function handleContextMenu(e) {
  e.preventDefault()
  emit('contextMenu', { event: e, node: props.node })
}
</script>

<template>
  <div
    class="flow-node-card flex flex-col cursor-pointer gap-2.5 bg-white px-3.5 py-3 transition-all duration-200"
    :class="[
      selected ? 'is-selected' : '',
      readonly ? 'is-readonly' : '',
    ]"
    :style="cardStyle"
    :data-node-id="node?.id"
    :data-node-type="node?.nodeType"
    @click="handleClick"
    @contextmenu="handleContextMenu"
  >
    <div class="flow-node-title w-full flex items-center gap-2">
      <span
        class="flow-node-icon h-11 w-11 flex shrink-0 items-center justify-center text-white"
      >
        <i :class="icon" class="text-2xl" />
      </span>
      <div class="flow-node-main min-w-0 flex flex-col flex-1 gap-1">
        <div class="flow-node-heading min-w-0 flex items-center gap-1.5">
          <span class="flow-node-name text-sm truncate text-gray-800 font-semibold">
            {{ node?.name || localizeLabel('未命名节点') }}
          </span>
          <slot v-if="!readonly" name="title-extra" />
        </div>
        <div v-if="statusBadge" class="flow-node-meta flex items-center">
          <span
            class="flow-node-status shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium"
            :class="statusBadge.class"
          >{{ localizeLabel(statusBadge.label) }}</span>
        </div>
      </div>

      <button
        v-if="canDelete"
        class="shrink-0 rounded p-1 text-gray-300 transition-colors hover:bg-red-100 hover:text-red-500"
        :aria-label="translateUiText(&quot;删除节点&quot;)"
        @click.stop="handleDelete"
      >
        <i class="i-mdi-close text-base" />
      </button>
    </div>

    <div v-if="subtitle && !readonly" class="flow-node-summary text-xs w-full truncate text-gray-500">
      {{ subtitle }}
    </div>
    <div v-if="$slots.default && !readonly" class="flow-node-extra text-xs w-full truncate text-gray-500">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.flow-node-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.flow-node-card:not(.is-readonly):hover {
  border-color: rgba(53, 109, 255, 0.2);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.12);
  transform: translateY(-1px);
}

.flow-node-card.is-selected {
  border-color: #38b8b2;
  box-shadow:
    0 0 0 2px rgba(56, 184, 178, 0.18),
    0 12px 30px rgba(15, 23, 42, 0.12);
}

.flow-node-card.is-readonly {
  cursor: default;
}

.flow-node-icon {
  background: var(--flow-node-color);
  border-radius: 7px;
  box-shadow: 0 8px 18px var(--flow-node-shadow);
}

.flow-node-title {
  min-height: 46px;
}

.flow-node-summary {
  display: block;
  min-height: 34px;
  padding: 8px 10px;
  border: 1px solid rgba(15, 23, 42, 0.03);
  border-radius: 4px;
  background: linear-gradient(180deg, #fafbfc 0%, #f6f8fa 100%);
  line-height: 1.45;
  white-space: normal;
}

.flow-node-extra {
  color: #64748b;
  line-height: 1.45;
}

.flow-node-card.is-readonly {
  gap: 10px;
  padding: 13px 15px;
  justify-content: center;
}

.flow-node-card.is-readonly .flow-node-title {
  align-items: center;
  gap: 10px;
  min-height: 54px;
}

.flow-node-card.is-readonly .flow-node-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  box-shadow: 0 10px 22px var(--flow-node-shadow);
}

.flow-node-card.is-readonly .flow-node-icon i {
  font-size: 30px;
  line-height: 1;
}

.flow-node-card.is-readonly .flow-node-name {
  line-height: 1.35;
}

.flow-node-card.is-readonly .flow-node-summary {
  color: #475569;
  background: #f8fafc;
}

@media (prefers-reduced-motion: reduce) {
  .flow-node-card {
    transition: none;
  }

  .flow-node-card:not(.is-readonly):hover {
    transform: none;
  }
}
</style>
