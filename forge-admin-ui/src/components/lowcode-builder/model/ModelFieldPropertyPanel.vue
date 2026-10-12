<template>
  <aside class="property-panel">
    <div class="panel-head">
      <div>
        <div class="panel-title">
          {{ translateUiText("字段属性") }}
        </div>
        <div class="panel-subtitle">
          {{ field?.field || translateUiText("未选择字段") }}
        </div>
      </div>
    </div>

    <n-empty v-if="!field" :description="translateUiText(&quot;请选择中间字段行&quot;)" />
    <n-form v-else label-placement="top" size="small" class="field-form" :show-feedback="false">
      <n-alert v-if="readonlyField" type="info" :bordered="false" class="readonly-alert">
        {{ translateUiText("系统字段由平台维护，仅用于展示真实表结构，不能手动修改。") }}
      </n-alert>
      <section class="form-section">
        <div class="section-title">{{ translateUiText('英文显示元数据') }}</div>
        <n-form-item :label="translateUiText('字段英文名称')">
          <n-input :value="field.labelI18n?.['en-US'] || ''" :disabled="readonlyField"
            @update:value="updateFieldProp('labelI18n', { ...field.labelI18n, 'en-US': $event })" />
        </n-form-item>
        <n-form-item :label="translateUiText('英文输入提示')">
          <n-input :value="field.placeholderI18n?.['en-US'] || ''" :disabled="readonlyField"
            @update:value="updateFieldProp('placeholderI18n', { ...field.placeholderI18n, 'en-US': $event })" />
        </n-form-item>
      </section>
      <section class="form-section">
        <div class="section-title">
          {{ translateUiText("数据库映射") }}
        </div>
        <n-form-item :label="translateUiText(&quot;数据库列名&quot;)">
          <n-input
            :value="field.columnName"
            :disabled="readonlyField"
            placeholder="lower_snake"
            @update:value="updateFieldProp('columnName', $event)"
          />
        </n-form-item>
        <div class="switch-grid">
          <div class="switch-item">
            <span>{{ translateUiText("主键字段") }}</span>
            <n-switch
              :value="field.primaryKey || false"
              :disabled="readonlyField"
              size="small"
              @update:value="updateFieldProp('primaryKey', $event)"
            />
          </div>
          <div class="switch-item">
            <span>{{ translateUiText("系统字段") }}</span>
            <n-switch
              :value="field.systemField || false"
              :disabled="readonlyField"
              size="small"
              @update:value="updateFieldProp('systemField', $event)"
            />
          </div>
        </div>
      </section>

      <section class="form-section">
        <div class="section-title">
          {{ translateUiText("字典与安全") }}
        </div>
        <div v-if="dictSuggestion || securitySuggestion" class="recommend-box">
          <div v-if="dictSuggestion" class="recommend-line">
            <span>{{ translateUiText("推荐字典：") }}{{ dictSuggestion.dictType }}</span>
            <n-button text size="tiny" type="primary" @click="applyDictSuggestion">
              {{ translateUiText("应用") }}
            </n-button>
          </div>
          <div v-if="securitySuggestion" class="recommend-line">
            <span>{{ translateUiText("推荐脱敏：") }}{{ securitySuggestion.sensitiveType || 'NONE' }}</span>
            <n-button text size="tiny" type="primary" @click="applySecuritySuggestion">
              {{ translateUiText("应用") }}
            </n-button>
          </div>
        </div>
        <n-form-item :label="translateUiText(&quot;字典类型&quot;)">
          <DictTypeSelect
            :value="field.dictType"
            :fields="fields"
            :disabled="readonlyField"
            @update:value="updateFieldProp('dictType', $event)"
          />
        </n-form-item>
        <n-form-item :label="translateUiText('字典英文标签（JSON）')" :feedback="dictTranslationsError ? translateUiText('请填写业务值到语言标签的 JSON 对象') : undefined" :validation-status="dictTranslationsError ? 'error' : undefined">
          <n-input v-model:value="dictTranslationsDraft" type="textarea" :disabled="readonlyField" placeholder='{"WAITING":{"en-US":"Pending"}}' @blur="saveDictTranslations" />
        </n-form-item>
        <n-grid :cols="2" :x-gap="10">
          <n-form-item-gi :label="translateUiText(&quot;敏感类型&quot;)">
            <n-select
              :value="field.sensitiveType"
              :options="translateUiMetadata(sensitiveTypeOptions)"
              :disabled="readonlyField"
              size="small"
              @update:value="updateFieldProp('sensitiveType', $event)"
            />
          </n-form-item-gi>
          <n-form-item-gi :label="translateUiText(&quot;加密算法&quot;)">
            <n-select
              :value="field.encryptAlgorithm"
              clearable
              :disabled="readonlyField"
              size="small"
              :options="translateUiMetadata(encryptOptions)"
              @update:value="updateFieldProp('encryptAlgorithm', $event || '')"
            />
          </n-form-item-gi>
        </n-grid>
      </section>
    </n-form>
  </aside>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

import { computed, ref, watch } from 'vue'
import DictTypeSelect from '../shared/DictTypeSelect.vue'
import {
  isLockedSystemField,
  sensitiveTypeOptions,
} from './model-schema'

const props = defineProps({
  field: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  domain: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:field'])

const encryptOptions = [
  { label: '不加密', value: '' },
  { label: 'SM4', value: 'SM4' },
  { label: 'AES', value: 'AES' },
]
const domainSchema = computed(() => props.domain?.domainSchema || {})
const dictSuggestion = computed(() => findRecommendation(domainSchema.value.dictRecommendations || []))
const securitySuggestion = computed(() => findRecommendation(domainSchema.value.securityPolicies || []))
const readonlyField = computed(() => isLockedSystemField(props.field))
const dictTranslationsDraft = ref('')
const dictTranslationsError = ref(false)
watch(() => [props.field?.field, props.field?.dictLabelsI18n], () => {
  dictTranslationsDraft.value = props.field?.dictLabelsI18n ? JSON.stringify(props.field.dictLabelsI18n, null, 2) : ''
  dictTranslationsError.value = false
}, { immediate: true })
function saveDictTranslations() {
  if (readonlyField.value) return
  try {
    const labels = JSON.parse(dictTranslationsDraft.value || '{}')
    if (!labels || Array.isArray(labels) || typeof labels !== 'object'
      || Object.values(labels).some(value => !value || Array.isArray(value) || typeof value !== 'object'
        || Object.values(value).some(text => typeof text !== 'string'))) throw new Error('Invalid display metadata')
    dictTranslationsError.value = false
    patchField({ dictLabelsI18n: labels })
  } catch {
    dictTranslationsError.value = true
  }
}

function updateFieldProp(key, value) {
  if (readonlyField.value)
    return
  if (key === 'dictType' && value) {
    patchField({ dictType: value, componentType: 'select' })
    return
  }
  patchField({ [key]: value })
}

function applyDictSuggestion() {
  if (!dictSuggestion.value || readonlyField.value)
    return
  patchField({ dictType: dictSuggestion.value.dictType, componentType: 'select' })
}

function applySecuritySuggestion() {
  if (!securitySuggestion.value || readonlyField.value)
    return
  patchField({
    sensitiveType: securitySuggestion.value.sensitiveType || 'NONE',
    encryptAlgorithm: securitySuggestion.value.encryptAlgorithm || '',
  })
}

function patchField(patch) {
  if (!props.field || readonlyField.value)
    return
  emit('update:field', {
    ...props.field,
    ...patch,
  })
}

function findRecommendation(items) {
  if (!props.field)
    return null
  const text = `${props.field.field || ''} ${props.field.columnName || ''} ${props.field.label || ''}`.toLowerCase()
  return items.find((item) => {
    const pattern = String(item.fieldPattern || '').toLowerCase()
    return pattern && text.includes(pattern)
  }) || null
}
</script>

<style scoped>
.property-panel {
  height: 100%;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.panel-head {
  display: flex;
  min-height: 50px;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid #d8dee8;
  background: #f8fafc;
}

.panel-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.panel-subtitle {
  margin-top: 2px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.field-form {
  display: grid;
  max-height: calc(100% - 50px);
  gap: 12px;
  overflow: auto;
  padding: 12px;
}

.readonly-alert {
  grid-column: 1 / -1;
}

.form-section {
  display: grid;
  gap: 10px;
  border-bottom: 1px solid #eef2f7;
  padding-bottom: 12px;
}

.form-section:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.section-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  color: #475569;
  font-size: 12px;
}

.recommend-box {
  display: grid;
  gap: 6px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  padding: 8px;
}

.recommend-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #1e40af;
  font-size: 12px;
}
</style>
