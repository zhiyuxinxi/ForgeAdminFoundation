<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

/**
 * ScriptConfig — 脚本任务
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
const emit = defineEmits(['update:config'])

const FORMAT_OPTIONS = [
  { label: 'JavaScript', value: 'javascript' },
  { label: 'Groovy', value: 'groovy' },
  { label: 'Python (Jython)', value: 'python' },
]

function field(name, fallback = '') {
  return computed({
    get: () => props.node.config?.[name] ?? fallback,
    set: v => emit('update:config', { [name]: v }),
  })
}
const scriptFormat = field('scriptFormat', 'javascript')
const script = field('script')
const async = field('async', false)
</script>

<template>
  <div class="space-y-3">
    <n-form-item :label="translateUiText(&quot;脚本语言&quot;)" label-placement="top" :show-feedback="false">
      <n-select v-model:value="scriptFormat" :options="translateUiMetadata(FORMAT_OPTIONS)" :disabled="readonly" />
    </n-form-item>
    <n-form-item :label="translateUiText(&quot;脚本内容&quot;)" label-placement="top" :show-feedback="false">
      <n-input
        v-model:value="script"
        type="textarea"
        :autosize="{ minRows: 6, maxRows: 14 }"
        :disabled="readonly"
        :placeholder="translateUiText(&quot;// 在此编写脚本&quot;)"
      />
    </n-form-item>
    <n-form-item :label="translateUiText(&quot;异步执行&quot;)" label-placement="top" :show-feedback="false">
      <n-switch v-model:value="async" :disabled="readonly" />
    </n-form-item>
  </div>
</template>
