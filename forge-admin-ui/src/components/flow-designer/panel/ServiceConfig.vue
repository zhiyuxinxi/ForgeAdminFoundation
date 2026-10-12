<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

/**
 * ServiceConfig — 服务任务（class / expression / delegateExpression）
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const TYPE_OPTIONS = [
  { label: 'Java 类', value: 'class' },
  { label: '表达式', value: 'expression' },
  { label: '代理表达式', value: 'delegateExpression' },
]

function field(name, fallback = '') {
  return computed({
    get: () => props.node.config?.[name] ?? fallback,
    set: v => emit('update:config', { [name]: v }),
  })
}
const implementationType = field('implementationType', 'class')
const implementation = field('implementation')
const async = field('async', false)
</script>

<template>
  <div class="space-y-3">
    <n-form-item :label="translateUiText(&quot;实现类型&quot;)" label-placement="top" :show-feedback="false">
      <n-select v-model:value="implementationType" :options="translateUiMetadata(TYPE_OPTIONS)" :disabled="readonly" />
    </n-form-item>
    <n-form-item :label="translateUiText(&quot;实现值&quot;)" label-placement="top" :show-feedback="false">
      <n-input
        v-model:value="implementation"
        :disabled="readonly"
        :placeholder="implementationType === 'class' ? 'com.example.MyDelegate' : '${myExpression}'"
      />
    </n-form-item>
    <n-form-item :label="translateUiText(&quot;异步执行&quot;)" label-placement="top" :show-feedback="false">
      <n-switch v-model:value="async" :disabled="readonly" />
    </n-form-item>
  </div>
</template>
