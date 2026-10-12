<script setup>
import { localizeLabel as translateUiText } from '@/locales'

/**
 * StartConfig — 开始节点配置
 *
 * 发起人变量由后端在流程启动时固定注入为 initiator，表单配置走流程模型全局配置。
 */
defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
defineEmits(['update:config'])
</script>

<template>
  <div class="space-y-3">
    <div class="border border-gray-100 rounded bg-gray-50 px-3 py-3">
      <div class="flex items-center justify-between gap-3">
        <span class="text-sm text-gray-600">{{ translateUiText("发起人") }}</span>
        <n-tag size="small" type="success" :bordered="false">
          {{ translateUiText("当前登录用户") }}
        </n-tag>
      </div>
      <div class="mt-2 flex items-center justify-between gap-3">
        <span class="text-sm text-gray-600">{{ translateUiText("变量名") }}</span>
        <code class="text-xs rounded bg-white px-2 py-0.5 text-gray-700">initiator</code>
      </div>
    </div>
    <div class="text-xs rounded bg-blue-50 px-3 py-2 text-blue-600">
      {{ translateUiText("发起人由系统自动记录，流程表单请在右侧流程信息的“表单配置”中维护。") }}
    </div>
  </div>
</template>
