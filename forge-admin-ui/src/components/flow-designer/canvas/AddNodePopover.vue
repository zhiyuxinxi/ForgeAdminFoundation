<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * AddNodePopover — 节点类型选择弹窗内容
 *
 * 使用 NODE_MENU_GROUPS 渲染分组列表。
 * 点击某项时 emit('select', type)；外层负责关闭 Popover 与调用 useFlowDesigner.addNode。
 *
 * Props:
 *   - allowTypes  数组，限制可见节点类型（不在数组内的隐藏）。默认 null = 全部可用
 */
import { computed } from 'vue'
import { NODE_MENU_GROUPS } from '../constants/node-menu.js'

const props = defineProps({
  allowTypes: { type: Array, default: null },
})

const emit = defineEmits(['select'])

const groups = computed(() => {
  if (!props.allowTypes)
    return NODE_MENU_GROUPS
  return NODE_MENU_GROUPS
    .map(g => ({
      ...g,
      items: g.items.filter(it => props.allowTypes.includes(it.type)),
    }))
    .filter(g => g.items.length > 0)
})

function handleClick(type) {
  emit('select', type)
}
</script>

<template>
  <div class="add-node-popover">
    <div v-for="group in groups" :key="group.label" class="add-node-group">
      <div class="add-node-group-title">
        {{ translateUiText(group.label, group?.labelI18n) }}
      </div>
      <div class="add-node-menu-grid">
        <button
          v-for="item in group.items"
          :key="item.type"
          class="add-node-menu-item"
          :data-type="item.type"
          @click.stop="handleClick(item.type)"
        >
          <span class="add-node-menu-icon">
            <i :class="[item.icon, item.color]" />
          </span>
          <span class="add-node-menu-label">
            {{ translateUiText(item.label, item?.labelI18n) }}
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.add-node-popover {
  width: 360px;
  color: #1f2937;
}

.add-node-group {
  margin-bottom: 14px;
}

.add-node-group:last-child {
  margin-bottom: 0;
}

.add-node-group-title {
  padding: 0 2px 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.add-node-menu-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.add-node-menu-item {
  display: flex;
  min-width: 0;
  min-height: 46px;
  box-sizing: border-box;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 7px;
  appearance: none;
  background: #fff;
  color: #1f2937;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease;
}

.add-node-menu-item:hover {
  border-color: rgba(22, 93, 255, 0.12);
  background: #f7faff;
}

.add-node-menu-item:focus-visible {
  outline: 2px solid rgba(22, 93, 255, 0.24);
  outline-offset: 2px;
}

.add-node-menu-icon {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  background: #f2f5f8;
}

.add-node-menu-icon :deep(i) {
  width: 15px;
  height: 15px;
  font-size: 15px;
}

.add-node-menu-label {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (prefers-reduced-motion: reduce) {
  .add-node-menu-item {
    transition: none;
  }
}
</style>
