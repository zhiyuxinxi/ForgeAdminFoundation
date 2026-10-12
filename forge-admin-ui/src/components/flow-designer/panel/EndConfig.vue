<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * EndConfig — 结束节点（normal / terminate）
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const END_OPTIONS = [
  { label: '正常结束', value: 'normal' },
  { label: '强制终止流程', value: 'terminate' },
]

const endType = computed({
  get: () => props.node.config?.endType || 'normal',
  set: v => emit('update:config', { endType: v }),
})
</script>

<template>
  <div class="space-y-3">
    <n-form-item :label="translateUiText(&quot;结束类型&quot;)" label-placement="top" :show-feedback="false">
      <n-radio-group v-model:value="endType" :disabled="readonly">
        <n-space>
          <n-radio v-for="opt in END_OPTIONS" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </n-radio>
        </n-space>
      </n-radio-group>
    </n-form-item>
    <div class="config-hint">
      <p>{{ translateUiText("· 正常结束：当前路径完成") }}</p>
      <p>{{ translateUiText("· 强制终止：直接结束整个流程实例（不等待其他并行分支）") }}</p>
    </div>
  </div>
</template>
