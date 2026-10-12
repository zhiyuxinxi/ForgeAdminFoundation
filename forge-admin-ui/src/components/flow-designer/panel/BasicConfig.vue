<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * BasicConfig — 节点基础属性（id 只读 / name / documentation）
 *
 * 与 NodePropertiesPanel.vue:109-128 字段对齐：节点名称 / 节点描述
 * 通过 v-model:node 双向绑定到 NodeConfigDrawer 的 draftNode。
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:node'])

const name = computed({
  get: () => props.node?.name || '',
  set: (v) => {
    emit('update:node', { ...props.node, name: v })
  },
})

const documentation = computed({
  get: () => props.node?.config?.documentation || '',
  set: (v) => {
    emit('update:node', {
      ...props.node,
      config: { ...props.node.config, documentation: v },
    })
  },
})
</script>

<template>
  <div class="basic-config">
    <n-form-item :label="translateUiText(&quot;节点名称&quot;)" label-placement="top" required :show-feedback="false">
      <n-input v-model:value="name" :disabled="readonly" :placeholder="translateUiText(&quot;请输入节点名称&quot;)" />
    </n-form-item>
    <n-form-item :label="translateUiText(&quot;节点描述&quot;)" label-placement="top" :show-feedback="false">
      <n-input
        v-model:value="documentation"
        type="textarea"
        :disabled="readonly"
        :autosize="{ minRows: 2, maxRows: 4 }"
        :placeholder="translateUiText(&quot;可选：用于说明节点用途&quot;)"
      />
    </n-form-item>
  </div>
</template>
