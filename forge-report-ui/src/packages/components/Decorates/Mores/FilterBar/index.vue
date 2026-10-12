<template>
  <section class="filter-bar" :style="rootStyle">
    <label v-for="field in option.fields" :key="field.field">
      <span>{{ field.label }}</span>
      <input v-if="field.type === 'input'" v-model="form[field.field]" />
      <input v-else-if="field.type === 'date'" v-model="form[field.field]" type="date" />
      <select v-else v-model="form[field.field]">
        <option v-for="item in field.options || []" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
    </label>
    <button @click="applyFilter">{{ $ui("筛选") }}</button>
    <button class="ghost" @click="resetFilter">{{ $ui("重置") }}</button>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, PropType, reactive, watch } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const chartEditStore = useChartEditStore()
const form = reactive<Record<string, any>>({})
const option = computed(() => ({
  ...defaultOption,
  ...(props.chartConfig.option || {}),
  style: { ...defaultOption.style, ...(props.chartConfig.option?.style || {}) },
  fields: props.chartConfig.option?.fields || []
}))
const rootStyle = computed(() => ({
  '--filter-accent': option.value.style.accentColor,
  '--filter-text': option.value.style.textColor,
  '--filter-muted': option.value.style.mutedColor,
  '--filter-panel': option.value.style.panelColor,
  '--filter-border': option.value.style.borderColor
}))

const initForm = () => {
  option.value.fields.forEach(field => {
    if (!(field.field in form)) form[field.field] = ''
  })
}
const applyFilter = () => {
  chartEditStore.setRuntimePageContext({ ...chartEditStore.getRuntimePageContext, ...form })
  window.dispatchEvent(new CustomEvent('forge-report-refresh', { detail: { source: 'filter-bar', context: { ...form } } }))
  window['$message']?.success('筛选条件已应用')
}
const resetFilter = () => {
  Object.keys(form).forEach(key => {
    form[key] = ''
  })
  applyFilter()
}

watch(() => option.value.fields, initForm, { deep: true })
onMounted(initForm)
</script>

<style scoped lang="scss">
.filter-bar {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  width: 100%;
  height: 100%;
  padding: 12px;
  color: var(--filter-text);
  border: 1px solid var(--filter-border);
  border-radius: 8px;
  background: var(--filter-panel);
}

label {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 5px;
  min-width: 120px;

  span {
    color: var(--filter-muted);
    font-size: 12px;
  }
}

input,
select,
button {
  height: 30px;
  min-width: 0;
  padding: 0 10px;
  color: var(--filter-text);
  border: 1px solid var(--filter-border);
  border-radius: 5px;
  background: rgba(3, 12, 24, 0.76);
}

button {
  min-width: 64px;
  color: #00131f;
  cursor: pointer;
  border-color: transparent;
  background: var(--filter-accent);
}

button.ghost {
  color: var(--filter-muted);
  border-color: var(--filter-border);
  background: transparent;
}
</style>
