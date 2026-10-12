<template>
  <div class="section-fields">
    <NFormItem :label="translateUiText(&quot;任务名称&quot;)" path="jobName">
      <NInput
        :value="form.jobName"
        :disabled="editing"
        :placeholder="translateUiText(&quot;例如：库存日结&quot;)"
        maxlength="200"
        show-count
        @update:value="update('jobName', $event)"
      />
    </NFormItem>
    <NFormItem :label="translateUiText(&quot;任务分组&quot;)" path="jobGroup">
      <NAutoComplete
        :value="form.jobGroup"
        :disabled="editing"
        :options="groupOptions"
        :placeholder="translateUiText(&quot;例如：INVENTORY&quot;)"
        @update:value="update('jobGroup', $event)"
      />
    </NFormItem>
    <NFormItem :label="translateUiText(&quot;任务说明&quot;)" path="description" class="full-field">
      <NInput
        :value="form.description"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-count
        :placeholder="translateUiText(&quot;简要说明这个任务负责什么&quot;)"
        @update:value="update('description', $event)"
      />
    </NFormItem>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { NAutoComplete, NFormItem, NInput } from 'naive-ui'

defineProps({
  form: {
    type: Object,
    required: true,
  },
  editing: {
    type: Boolean,
    default: false,
  },
  groupOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['updateField'])

function update(field, value) {
  emit('updateField', { field, value })
}
</script>

<style scoped>
.section-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 18px;
}

.full-field {
  grid-column: 1 / -1;
}

@media (max-width: 680px) {
  .section-fields {
    grid-template-columns: 1fr;
  }

  .full-field {
    grid-column: auto;
  }
}
</style>
