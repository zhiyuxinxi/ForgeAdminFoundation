<template>
  <div class="trigger-action-config">
    <template v-if="actionType === 'BUSINESS_ACTION'">
      <n-form-item :label="translateUiText(&quot;对象动作&quot;)">
        <n-select
          v-model:value="config.actionCode"
          :options="actionOptions"
          clearable
          filterable
          :placeholder="translateUiText(&quot;选择要执行的对象动作&quot;)"
          @update:value="emitConfig"
        />
      </n-form-item>
    </template>

    <template v-else-if="actionType === 'START_FLOW'">
      <n-alert type="info" :bordered="false">
        {{ translateUiText("使用“流程与自动化”中配置的主流程。触发条件满足后自动发起，不需要在这里再次选择流程。") }}
      </n-alert>
    </template>

    <template v-else-if="actionType === 'SEND_MESSAGE'">
      <n-form-item :label="translateUiText(&quot;消息模板&quot;)">
        <n-input v-model:value="config.templateCode" :placeholder="translateUiText(&quot;消息模板编码&quot;)" @update:value="emitConfig" />
      </n-form-item>
      <n-form-item :label="translateUiText(&quot;接收人规则&quot;)">
        <n-select
          v-model:value="config.receiverRule"
          :options="receiverRuleOptions"
          clearable
          :placeholder="translateUiText(&quot;选择接收人规则&quot;)"
          @update:value="emitConfig"
        />
      </n-form-item>
      <n-form-item v-if="config.receiverRule === 'USERS'" :label="translateUiText(&quot;指定用户ID&quot;)">
        <n-input v-model:value="config.receiverIds" :placeholder="translateUiText(&quot;多个用逗号分隔&quot;)" @update:value="emitConfig" />
      </n-form-item>
    </template>

    <template v-else-if="actionType === 'CREATE_RECORD'">
      <n-form-item :label="translateUiText(&quot;目标对象&quot;)">
        <n-select
          v-model:value="config.targetObjectCode"
          :options="objectOptions"
          clearable
          filterable
          :placeholder="translateUiText(&quot;选择目标业务单元&quot;)"
          @update:value="emitConfig"
        />
      </n-form-item>
      <n-form-item :label="translateUiText(&quot;字段映射&quot;)">
        <div class="mapping-list">
          <div v-for="(item, index) in config.fieldMapping" :key="item.clientKey" class="mapping-row">
            <n-select
              v-model:value="item.sourceField"
              :options="fieldOptions"
              clearable
              filterable
              :placeholder="translateUiText(&quot;源字段&quot;)"
              @update:value="emitConfig"
            />
            <span>→</span>
            <n-input v-model:value="item.targetField" :placeholder="translateUiText(&quot;目标字段&quot;)" @update:value="emitConfig" />
            <n-button quaternary circle size="small" @click="removeFieldMapping(index)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </div>
          <n-button dashed size="small" @click="addFieldMapping">
            {{ translateUiText("添加字段映射") }}
          </n-button>
        </div>
      </n-form-item>
    </template>

    <template v-else-if="actionType === 'UPDATE_FIELD'">
      <n-form-item :label="translateUiText(&quot;更新字段&quot;)">
        <n-select
          v-model:value="config.targetField"
          :options="fieldOptions"
          clearable
          filterable
          :placeholder="translateUiText(&quot;选择字段&quot;)"
          @update:value="emitConfig"
        />
      </n-form-item>
      <n-form-item :label="translateUiText(&quot;更新值&quot;)">
        <n-input v-model:value="config.valueTemplate" :placeholder="translateUiText(&quot;固定值或 ${fieldCode}&quot;)" @update:value="emitConfig" />
      </n-form-item>
    </template>

    <template v-else-if="actionType === 'WEBHOOK'">
      <CallApiStepConfigPanel
        :model-value="config"
        :field-options="fieldOptions"
        context-mode="TRIGGER"
        @update:model-value="updateWebhookConfig"
      />
    </template>

    <n-empty v-else :description="translateUiText(&quot;请选择动作类型&quot;)" />
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { TrashOutline } from '@vicons/ionicons5'
import { reactive, watch } from 'vue'
import { normalizeCallApiStepConfig } from './designer/call-api-step-config'
import CallApiStepConfigPanel from './designer/CallApiStepConfigPanel.vue'

const props = defineProps({
  actionType: {
    type: String,
    default: '',
  },
  modelValue: {
    type: String,
    default: '',
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
  objectOptions: {
    type: Array,
    default: () => [],
  },
  receiverRuleOptions: {
    type: Array,
    default: () => [],
  },
  actionOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const config = reactive(normalizeConfig(props.actionType, props.modelValue))

watch(() => [props.actionType, props.modelValue], ([actionType, value]) => {
  const next = normalizeConfig(actionType, value)
  if (JSON.stringify(configPayload(config, actionType)) !== JSON.stringify(configPayload(next, actionType)))
    Object.assign(config, next)
}, { deep: true })

function addFieldMapping() {
  config.fieldMapping.push(createMapping({ sourceField: null, targetField: '' }))
  emitConfig()
}

function removeFieldMapping(index) {
  config.fieldMapping.splice(index, 1)
  emitConfig()
}

function emitConfig() {
  emit('update:modelValue', JSON.stringify(configPayload(config, props.actionType)))
}

function updateWebhookConfig(value) {
  Object.assign(config, normalizeCallApiStepConfig(value))
  emitConfig()
}

function normalizeConfig(actionType, value) {
  const source = safeParse(value)
  return {
    useMainFlow: source.useMainFlow !== false,
    templateCode: source.templateCode || '',
    receiverRule: source.receiverRule || null,
    receiverIds: source.receiverIds || '',
    actionCode: source.actionCode || source.businessActionCode || '',
    targetObjectCode: source.targetObjectCode || null,
    fieldMapping: normalizeFieldMapping(source.fieldMapping || []),
    targetField: source.targetField || '',
    valueTemplate: source.valueTemplate ?? source.value ?? '',
    channelConfigRef: source.channelConfigRef || '',
    eventName: source.eventName || '',
    ...normalizeCallApiStepConfig(source),
    actionType,
  }
}

function configPayload(value, actionType) {
  if (actionType === 'BUSINESS_ACTION') {
    return {
      actionCode: value.actionCode || '',
    }
  }
  if (actionType === 'START_FLOW') {
    return {
      useMainFlow: true,
    }
  }
  if (actionType === 'SEND_MESSAGE') {
    return {
      templateCode: value.templateCode || '',
      receiverRule: value.receiverRule || '',
      receiverIds: value.receiverIds || '',
    }
  }
  if (actionType === 'CREATE_RECORD') {
    return {
      targetObjectCode: value.targetObjectCode || '',
      fieldMapping: (value.fieldMapping || [])
        .map(item => ({
          sourceField: item.sourceField || '',
          targetField: item.targetField || '',
        }))
        .filter(item => item.sourceField && item.targetField),
    }
  }
  if (actionType === 'UPDATE_FIELD') {
    return {
      targetField: value.targetField || '',
      valueTemplate: value.valueTemplate ?? '',
    }
  }
  if (actionType === 'WEBHOOK') {
    const normalized = normalizeCallApiStepConfig(value)
    delete normalized.todo
    delete normalized.channelConfigRef
    delete normalized.eventName
    return normalized
  }
  return safeParse(props.modelValue)
}

function normalizeFieldMapping(list = []) {
  return list.map(item => createMapping({
    sourceField: item.sourceField || item.formField || item.field || null,
    targetField: item.targetField || '',
  }))
}

function createMapping(values = {}) {
  return {
    clientKey: `mapping_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    ...values,
  }
}

function safeParse(value) {
  if (!value)
    return {}
  try {
    return JSON.parse(value)
  }
  catch {
    return {}
  }
}
</script>

<style scoped>
.trigger-action-config {
  display: block;
  width: 100%;
}

.mapping-list {
  display: grid;
  gap: 8px;
  width: 100%;
}

.mapping-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 24px minmax(140px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

.mapping-row span {
  color: #64748b;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 720px) {
  .mapping-row {
    grid-template-columns: 1fr;
  }
}
</style>
