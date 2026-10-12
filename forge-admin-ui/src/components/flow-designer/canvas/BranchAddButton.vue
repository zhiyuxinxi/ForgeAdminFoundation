<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * BranchAddButton — 画布上的网关添加分支按钮
 *
 * 放在分支连线区域下方，保持结构编辑行为停留在画布上下文。
 */
const props = defineProps({
  position: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])

function handleClick() {
  if (props.readonly)
    return
  emit('click')
}
</script>

<template>
  <div
    class="branch-add-button-wrap absolute z-20"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, -50%)',
    }"
  >
    <button
      type="button"
      class="branch-add-btn flex items-center justify-center bg-white transition-all duration-200"
      :class="{ 'opacity-40 cursor-not-allowed': readonly }"
      :disabled="readonly"
      data-test="canvas-add-branch"
      :aria-label="translateUiText(&quot;添加条件分支&quot;)"
      :title="translateUiText(&quot;添加条件分支&quot;)"
      @click.stop="handleClick"
    >
      <i class="i-mdi-plus text-xl" />
    </button>
  </div>
</template>

<style scoped>
.branch-add-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(32, 178, 170, 0.32);
  border-radius: 999px;
  color: #159a9a;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.branch-add-btn:hover:not(:disabled) {
  border-color: #20b2aa;
  color: #0f8b8b;
  box-shadow: 0 10px 24px rgba(32, 178, 170, 0.16);
  transform: translateY(-1px);
}

.branch-add-btn:disabled {
  cursor: not-allowed;
}

@media (prefers-reduced-motion: reduce) {
  .branch-add-btn {
    transition: none;
  }

  .branch-add-btn:hover:not(:disabled) {
    transform: none;
  }
}
</style>
