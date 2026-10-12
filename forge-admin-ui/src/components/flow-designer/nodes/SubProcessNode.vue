<script setup>
/**
 * SubProcessNode — 子流程卡片
 */
import { computed } from 'vue'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
})
defineEmits(['click', 'delete', 'context-menu'])

const subtitle = computed(() => props.node?.config?.documentation || '内嵌子流程')
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly"
    icon="i-mdi-sitemap-outline" color-var="info" :subtitle="subtitle"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('context-menu', $event)"
  />
</template>
