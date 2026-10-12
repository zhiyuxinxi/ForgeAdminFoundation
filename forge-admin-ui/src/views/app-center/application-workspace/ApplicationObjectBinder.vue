<template>
  <n-modal
    :show="show"
    preset="card"
    :title="translateUiText(&quot;关联已有业务对象&quot;)"
    style="width: 560px"
    :mask-closable="false"
    @update:show="value => emit('update:show', value)"
  >
    <n-form label-placement="top">
      <n-form-item :label="translateUiText(&quot;业务对象&quot;)" required>
        <n-select
          v-model:value="form.objectId"
          filterable
          :loading="loading"
          :options="objectOptions"
          :placeholder="translateUiText(&quot;选择当前业务域中的对象&quot;)"
        />
      </n-form-item>
      <n-form-item :label="translateUiText(&quot;应用内角色&quot;)" required>
        <DictSelect
          v-model:value="form.objectRole"
          dict-type="ai_business_application_object_role"
          :clearable="false"
        />
      </n-form-item>
      <n-alert type="info" :bordered="false">
        {{ translateUiText("关联只建立应用编排关系，不会复制对象，也不会修改对象对应的数据库表。") }}
      </n-alert>
    </n-form>

    <template #footer>
      <n-space justify="end">
        <n-button @click="emit('update:show', false)">
          {{ translateUiText("取消") }}
        </n-button>
        <n-button type="primary" :disabled="!form.objectId" @click="confirm">
          {{ translateUiText("加入应用") }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { computed, reactive, watch } from 'vue'
import DictSelect from '@/components/DictSelect.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  objects: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  defaultRole: {
    type: String,
    default: 'SHARED',
  },
})

const emit = defineEmits(['update:show', 'confirm'])
const form = reactive({ objectId: null, objectRole: props.defaultRole })

const objectOptions = computed(() => props.objects.map(item => ({
  label: `${item.objectName || item.objectCode} · ${item.objectCode}`,
  value: item.id,
})))

watch(() => props.show, (visible) => {
  if (!visible)
    return
  form.objectId = null
  form.objectRole = props.defaultRole
})

function confirm() {
  if (!form.objectId)
    return
  emit('confirm', {
    objectId: form.objectId,
    objectRole: form.objectRole || props.defaultRole,
  })
}
</script>
