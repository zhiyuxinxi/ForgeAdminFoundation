<template>
  <AiForm
    class="expand-form-panel"
    :value="record"
    :schema="readonlySchema"
    :grid-cols="formConfig.gridCols || 2"
    :label-width="formConfig.labelWidth || 'auto'"
    :label-placement="formConfig.labelPlacement || 'left'"
    :size="formConfig.size || 'small'"
    :show-actions="false"
    :show-feedback="false"
    readonly
  />
</template>

<script setup>
import { computed } from 'vue'
import AiForm from '../AiForm.vue'

const props = defineProps({
  panel: { type: Object, required: true },
  data: { type: Object, default: () => ({}) },
  row: { type: Object, default: () => ({}) },
})

const formConfig = computed(() => props.panel.form || {})
const record = computed(() => props.data && !Array.isArray(props.data) ? props.data : props.row)
const readonlySchema = computed(() => (formConfig.value.schema || []).map(field => ({
  ...field,
  readonly: true,
  disabled: true,
  props: {
    ...(field.props || {}),
    readonly: true,
    disabled: true,
  },
})))
</script>

<style scoped>
.expand-form-panel {
  padding-top: 2px;
}
</style>
