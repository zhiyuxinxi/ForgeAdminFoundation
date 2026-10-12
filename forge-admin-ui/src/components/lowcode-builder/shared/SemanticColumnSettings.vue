<template>
  <div v-if="['entity', 'relations'].includes(renderType)" class="semantic-column-settings">
    <label>
      <span>{{ translateUiText('主显示字段') }}</span>
      <NSelect :value="setting.titleField || null" :options="fields" clearable filterable size="tiny" :placeholder="translateUiText('默认当前字段')" @update:value="emit('update', { titleField: $event || '' })" />
    </label>
    <label v-if="renderType === 'entity'">
      <span>{{ translateUiText('辅助标识字段') }}</span>
      <NSelect :value="setting.subtitleField || null" :options="fields" clearable filterable size="tiny" :placeholder="translateUiText('不显示辅助标识')" @update:value="emit('update', { subtitleField: $event || '' })" />
    </label>
    <label v-if="renderType === 'entity'" class="semantic-column-toggle">
      <span>{{ translateUiText('显示首字标识') }}</span>
      <NSwitch :value="setting.avatar === true" size="small" @update:value="emit('update', { avatar: $event })" />
    </label>
  </div>
</template>

<script setup>
import { NSelect, NSwitch } from 'naive-ui'
import { localizeLabel as translateUiText } from '@/locales'

defineProps({
  renderType: { type: String, default: '' },
  setting: { type: Object, default: () => ({}) },
  fields: { type: Array, default: () => [] },
})
const emit = defineEmits(['update'])
</script>

<style scoped>
.semantic-column-settings {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.semantic-column-settings label {
  display: grid;
  min-width: 0;
  gap: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.semantic-column-settings .semantic-column-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
