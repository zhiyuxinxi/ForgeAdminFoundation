<template>
  <section class="dynamic-form" :style="rootStyle">
    <h3>{{ option.title }}</h3>
    <label v-for="field in option.fields" :key="field.field">
      <span>{{ field.label }}<em v-if="field.required">*</em></span>
      <textarea v-if="field.type === 'textarea'" v-model="form[field.field]"></textarea>
      <select v-else-if="field.type === 'select'" v-model="form[field.field]">
        <option v-for="item in field.options || []" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <input v-else v-model="form[field.field]" :type="field.type === 'number' ? 'number' : field.type === 'date' ? 'date' : 'text'" />
    </label>
    <button :disabled="submitting" @click="submit">{{ submitting ? $ui("提交中") : $ui("提交") }}</button>
  </section>
</template>

<script setup lang="ts">
import { uiText } from '@/i18n/display'
import { computed, onMounted, PropType, reactive, ref, watch } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { post, put } from '@/api/http'
import { option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const form = reactive<Record<string, any>>({})
const submitting = ref(false)
const option = computed(() => ({
  ...defaultOption,
  ...(props.chartConfig.option || {}),
  style: { ...defaultOption.style, ...(props.chartConfig.option?.style || {}) },
  fields: props.chartConfig.option?.fields || []
}))
const rootStyle = computed(() => ({
  '--form-accent': option.value.style.accentColor,
  '--form-text': option.value.style.textColor,
  '--form-muted': option.value.style.mutedColor,
  '--form-panel': option.value.style.panelColor,
  '--form-border': option.value.style.borderColor
}))

const initForm = () => {
  option.value.fields.forEach(field => {
    if (!(field.field in form)) form[field.field] = ''
  })
}

watch(() => option.value.fields, initForm, { deep: true })
onMounted(initForm)

const submit = async () => {
  const invalid = option.value.fields.find(field => field.required && !form[field.field])
  if (invalid) {
    window['$message']?.warning(uiText("请填写{0}", { 0: (invalid.label) }))
    return
  }
  if (!option.value.submitUrl) {
    window['$message']?.warning('请先配置提交地址')
    return
  }
  submitting.value = true
  try {
    const method = option.value.submitMethod === 'put' ? put : post
    await method(option.value.submitUrl, form)
    window['$message']?.success('提交成功')
    window.dispatchEvent(new CustomEvent('forge-report-refresh', { detail: { source: 'dynamic-form' } }))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.dynamic-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  height: 100%;
  padding: 16px;
  color: var(--form-text);
  border: 1px solid var(--form-border);
  border-radius: 8px;
  background: var(--form-panel);
}

h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 5px;

  span {
    color: var(--form-muted);
    font-size: 12px;
  }

  em {
    color: #fb7185;
    font-style: normal;
  }
}

input,
select,
textarea {
  min-height: 30px;
  padding: 0 10px;
  color: var(--form-text);
  border: 1px solid var(--form-border);
  border-radius: 5px;
  outline: none;
  background: rgba(3, 12, 24, 0.76);
}

textarea {
  min-height: 70px;
  padding: 8px 10px;
  resize: none;
}

button {
  height: 34px;
  margin-top: auto;
  color: #00131f;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
  background: var(--form-accent);
}
</style>
