<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * BranchNode — 网关节点（condition / parallel / inclusive 共享样式）
 *
 * 网关是分支锚点，不是审批办理节点。画布布局仍给它保留标准节点尺寸用于连线，
 * 视觉上只在中心渲染轻量锚点，避免被误认为普通任务卡。
 */
import { computed } from 'vue'
import { localizeLabel, t as translate } from '@/locales'

const props = defineProps({
  node: { type: Object, required: true },
  outgoingCount: { type: Number, default: 0 },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
})

const emit = defineEmits(['click', 'delete', 'context-menu'])

const META = {
  condition: { icon: 'i-mdi-close', color: 'warning', label: '条件分支' },
  parallel: { icon: 'i-mdi-call-split', color: 'info', label: '并行分支' },
  inclusive: { icon: 'i-mdi-set-merge', color: 'success', label: '包容分支' },
}

const meta = computed(() => META[props.node?.nodeType] || META.condition)

const subtitle = computed(() => {
  const t = props.node?.nodeType
  const n = props.outgoingCount || 0
  if (props.readonly) {
    const keys = { condition: 'conditionBranches', parallel: 'parallelBranches', inclusive: 'inclusiveBranches' }
    return n > 0 && keys[t] ? translate(`workflowViewer.${keys[t]}`, { count: n }) : ''
  }
  if (t === 'condition')
    return n > 0 ? `${n} 条条件分支` : '点击配置分支条件'
  if (t === 'parallel')
    return n > 0 ? `${n} 条并行分支（同时执行）` : '点击配置并行分支'
  if (t === 'inclusive')
    return n > 0 ? `${n} 条包容分支` : '点击配置包容分支'
  return ''
})

const canDelete = computed(() => !props.readonly)

function handleClick() {
  emit('click', props.node)
}

function handleDelete(event) {
  event.stopPropagation()
  if (canDelete.value)
    emit('delete', props.node)
}

function handleContextMenu(event) {
  event.preventDefault()
  emit('context-menu', { event, node: props.node })
}
</script>

<template>
  <div
    class="flow-node-card branch-node-shell"
    :class="[
      selected ? 'is-selected' : '',
      readonly ? 'is-readonly' : '',
      `is-${meta.color}`,
    ]"
    :data-node-id="node?.id"
    :data-node-type="node?.nodeType"
    :title="`${node?.name || localizeLabel(meta.label)}: ${subtitle}`"
    @click="handleClick"
    @contextmenu="handleContextMenu"
  >
    <div class="branch-node-diamond">
      <span class="branch-node-icon">
        <i :class="meta.icon" class="text-base" />
      </span>
      <span class="branch-node-assistive">
        {{ node?.name || localizeLabel(meta.label) }} {{ localizeLabel(meta.label) }} {{ subtitle }}
      </span>
      <button
        v-if="canDelete"
        class="branch-node-delete"
        :aria-label="translateUiText(&quot;删除网关&quot;)"
        @click.stop="handleDelete"
      >
        <i class="i-mdi-close text-sm" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.branch-node-shell {
  position: relative;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  box-shadow: none;
  cursor: pointer;
}

.branch-node-diamond {
  position: relative;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
  border-radius: 8px;
  background: #ff7a1a;
  box-shadow:
    0 0 0 1px rgba(255, 122, 26, 0.28),
    0 8px 18px rgba(255, 122, 26, 0.24);
  transform: rotate(45deg);
  transition:
    box-shadow 160ms ease,
    transform 160ms ease;
}

.branch-node-shell.is-info .branch-node-diamond {
  background: #3b82f6;
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.28),
    0 8px 18px rgba(59, 130, 246, 0.22);
}

.branch-node-shell.is-success .branch-node-diamond {
  background: #20b2aa;
  box-shadow:
    0 0 0 1px rgba(32, 178, 170, 0.28),
    0 8px 18px rgba(32, 178, 170, 0.22);
}

.branch-node-shell:not(.is-readonly):hover .branch-node-diamond {
  box-shadow:
    0 0 0 3px rgba(255, 122, 26, 0.14),
    0 12px 24px rgba(255, 122, 26, 0.28);
  transform: rotate(45deg) scale(1.04);
}

.branch-node-shell.is-selected .branch-node-diamond {
  box-shadow:
    0 0 0 4px rgba(255, 122, 26, 0.18),
    0 12px 24px rgba(255, 122, 26, 0.3);
}

.branch-node-icon {
  display: inline-flex;
  color: #fff;
  line-height: 1;
  transform: rotate(-45deg);
}

.branch-node-delete {
  position: absolute;
  right: -14px;
  top: -14px;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(239, 68, 68, 0.16);
  border-radius: 999px;
  background: #fff;
  color: #ef4444;
  opacity: 0;
  transform: rotate(-45deg);
  transition:
    background 160ms ease,
    color 160ms ease;
}

.branch-node-shell:hover .branch-node-delete,
.branch-node-shell.is-selected .branch-node-delete {
  opacity: 1;
}

.branch-node-delete:hover {
  background: #fee2e2;
  color: #ef4444;
}

.branch-node-shell.is-readonly {
  cursor: default;
}

.branch-node-shell.is-readonly .branch-node-delete {
  display: none;
}

.branch-node-assistive {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

@media (prefers-reduced-motion: reduce) {
  .branch-node-diamond,
  .branch-node-delete {
    transition: none;
  }

  .branch-node-shell:not(.is-readonly):hover .branch-node-diamond {
    transform: rotate(45deg);
  }
}
</style>
