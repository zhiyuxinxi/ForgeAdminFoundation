<template>
  <n-select
    :value="value"
    :options="translateUiMetadata(dataTypeOptions)"
    size="small"
    @update:value="$emit('update:value', $event)"
  />
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { dataTypeOptions } from '../model/model-schema'

defineProps({
  value: {
    type: String,
    default: 'varchar',
  },
})

defineEmits(['update:value'])
</script>
