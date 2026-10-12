<script setup>
/**
 * AddNodeButton — 节点之间的 "+" 添加按钮
 *
 * 样式参考钉钉：极简白色图标按钮
 * 点击后弹出 AddNodePopover 选择节点类型
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AddNodePopover from './AddNodePopover.vue'

const props = defineProps({
  position: { type: Object, required: true },
  allowTypes: { type: Array, default: null },
  readonly: { type: Boolean, default: false },
  label: { type: String, default: '添加' },
})

const emit = defineEmits(['select'])

const popoverVisible = ref(false)

function toggle() {
  if (props.readonly)
    return
  popoverVisible.value = !popoverVisible.value
}

function handleSelect(type) {
  popoverVisible.value = false
  emit('select', type)
}

function handleClickOutside(event) {
  if (!event.target.closest('.add-node-button-wrap'))
    popoverVisible.value = false
}

onMounted(() => {
  window.addEventListener('mousedown', handleClickOutside, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousedown', handleClickOutside, true)
})
</script>

<template>
  <div
    class="add-node-button-wrap absolute z-20"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, 0)',
    }"
  >
    <button
      class="add-node-btn flex items-center justify-center bg-white transition-all duration-200"
      :class="{ 'opacity-40 cursor-not-allowed': readonly }"
      :disabled="readonly"
      :aria-expanded="popoverVisible"
      :aria-label="`添加${label}节点`"
      :title="`添加${label}节点`"
      @click.stop="toggle"
    >
      <i class="i-mdi-plus text-xl" />
    </button>
    <div
      v-if="popoverVisible"
      class="add-node-popover-anchor absolute left-1/2 top-full z-30 mt-2 bg-white p-2 shadow-lg -translate-x-1/2"
    >
      <AddNodePopover :allow-types="allowTypes" @select="handleSelect" />
    </div>
  </div>
</template>

<style scoped>
.add-node-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(32, 178, 170, 0.32);
  border-radius: 999px;
  color: #159a9a;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.add-node-btn:hover:not(:disabled),
.add-node-btn[aria-expanded='true'] {
  border-color: #20b2aa;
  color: #0f8b8b;
  box-shadow: 0 10px 24px rgba(32, 178, 170, 0.16);
  transform: translateY(-1px);
}

.add-node-popover-anchor {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8px;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.14);
}

@media (prefers-reduced-motion: reduce) {
  .add-node-btn {
    transition: none;
  }

  .add-node-btn:hover:not(:disabled),
  .add-node-btn[aria-expanded='true'] {
    transform: none;
  }
}
</style>
