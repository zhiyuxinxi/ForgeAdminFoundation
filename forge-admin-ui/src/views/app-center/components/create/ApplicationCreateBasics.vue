<template>
  <n-form ref="formRef" :model="modelValue" :rules="rules" label-placement="top" class="create-basics">
    <n-grid cols="1 m:2" :x-gap="16" responsive="screen">
      <n-form-item-gi :label="translateUiText(&quot;应用名称&quot;)" path="applicationName">
        <n-input
          :value="modelValue.applicationName"
          maxlength="64"
          show-count
          :placeholder="translateUiText(&quot;例如：客户经营&quot;)"
          @update:value="updateField('applicationName', $event)"
        />
      </n-form-item-gi>
      <n-form-item-gi :label="translateUiText(&quot;所属业务域&quot;)" path="suiteCode">
        <n-select
          :value="modelValue.suiteCode"
          :options="suiteOptions"
          filterable
          :placeholder="translateUiText(&quot;选择应用所属业务域&quot;)"
          @update:value="updateField('suiteCode', $event)"
        />
      </n-form-item-gi>
    </n-grid>

    <n-grid cols="1 m:2" :x-gap="16" responsive="screen">
      <n-form-item-gi :label="translateUiText(&quot;应用图标&quot;)">
        <IconSelector
          :model-value="modelValue.icon"
          @update:model-value="updateField('icon', $event)"
        />
      </n-form-item-gi>
      <n-form-item-gi :label="translateUiText(&quot;应用状态&quot;)">
        <div class="status-field">
          <n-switch
            :value="modelValue.status"
            :checked-value="1"
            :unchecked-value="0"
            @update:value="updateField('status', $event)"
          />
          <span>{{ Number(modelValue.status) === 1 ? translateUiText("创建后启用") : translateUiText("创建后停用") }}</span>
        </div>
      </n-form-item-gi>
    </n-grid>

    <n-form-item :label="translateUiText(&quot;应用说明&quot;)">
      <n-input
        :value="modelValue.description"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 4 }"
        maxlength="500"
        show-count
        :placeholder="translateUiText(&quot;说明应用解决什么问题、由谁使用&quot;)"
        @update:value="updateField('description', $event)"
      />
    </n-form-item>

    <n-collapse>
      <n-collapse-item name="identity" :title="translateUiText(&quot;高级设置&quot;)">
        <n-form-item :label="translateUiText(&quot;应用编码&quot;)" path="applicationCode">
          <n-input
            :value="modelValue.applicationCode"
            :placeholder="translateUiText(&quot;留空则由系统自动生成&quot;)"
            @update:value="updateField('applicationCode', $event)"
            @blur="updateField('applicationCode', normalizeCode(modelValue.applicationCode))"
          />
          <template #feedback>
            {{ translateUiText("用于路由和接口标识，系统会保证租户内唯一，创建后不可修改。") }}
          </template>
        </n-form-item>
      </n-collapse-item>
    </n-collapse>
  </n-form>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { computed, ref } from 'vue'
import IconSelector from '@/components/IconSelector.vue'

const props = defineProps({
  modelValue: { type: Object, required: true },
  suites: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue'])
const formRef = ref(null)

const suiteOptions = computed(() => props.suites.map(suite => ({
  label: suite.suiteName || suite.suiteCode,
  value: suite.suiteCode,
  disabled: Number(suite.status) !== 1,
})))

const rules = {
  applicationName: {
    required: true,
    message: '请输入应用名称',
    trigger: ['blur', 'input'],
  },
  suiteCode: {
    required: true,
    message: '请选择所属业务域',
    trigger: ['blur', 'change'],
  },
  applicationCode: {
    validator: (_, value) => !normalizeCode(value) || /^[a-z]\w{1,63}$/i.test(normalizeCode(value)),
    message: '应用编码需以字母开头，仅包含字母、数字和下划线（2-64字符）',
    trigger: ['blur', 'input'],
  },
}

function updateField(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

function normalizeCode(value) {
  return String(value || '').trim().replace(/\W/g, '_').replace(/_+/g, '_')
}

async function validate() {
  await formRef.value?.validate()
  return true
}

defineExpose({ validate })
</script>

<style scoped>
.create-basics {
  padding: 2px 0;
}

.status-field {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 10px;
  color: var(--n-text-color-3);
  font-size: 13px;
}
</style>
