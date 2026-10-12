<template>
  <div class="business-advanced-config">
    <div class="advanced-head">
      <div>
        <h3>{{ translateUiText("高级配置") }}</h3>
        <p>{{ translateUiText("运行配置、数据表和页面协议只在授权开发者模式展示。") }}</p>
      </div>
      <n-switch
        :value="developerMode"
        :disabled="!canAdvanced"
        @update:value="$emit('update:developerMode', $event)"
      >
        <template #checked>
          {{ translateUiText("开发者") }}
        </template>
        <template #unchecked>
          {{ translateUiText("普通") }}
        </template>
      </n-switch>
    </div>

    <div class="advanced-body">
      <n-alert v-if="!canAdvanced" type="warning" :bordered="false">
        {{ translateUiText("高级配置需要 ai:businessObject:advanced 权限。普通模式不会展示表名、页面协议或运行配置键。") }}
      </n-alert>

      <template v-else-if="!developerMode">
        <section class="advanced-safe-card">
          <h4>{{ translateUiText("普通模式") }}</h4>
          <p>{{ translateUiText("当前只展示业务语言配置。技术细节仅授权开发者可见。") }}</p>
          <div class="safe-summary">
            <div>
              <span>{{ translateUiText("业务单元") }}</span>
              <strong>{{ draft.objectName || draft.objectCode || '-' }}</strong>
            </div>
            <div>
              <span>{{ translateUiText("字段数量") }}</span>
              <strong>{{ fieldCount }}</strong>
            </div>
            <div>
              <span>{{ translateUiText("页面区域") }}</span>
              <strong>{{ zoneCount }}</strong>
            </div>
            <div>
              <span>{{ translateUiText("发布状态") }}</span>
              <strong>{{ draft.lastPublishVersion ? translateUiText("已发布") : translateUiText("未发布") }}</strong>
            </div>
          </div>
        </section>
      </template>

      <template v-else>
        <section class="advanced-grid">
          <div>
            <span>{{ translateUiText("对象编码") }}</span>
            <code>{{ draft.objectCode || '-' }}</code>
          </div>
          <div>
            <span>{{ translateUiText("模型编码") }}</span>
            <code>{{ modelCode || '-' }}</code>
          </div>
          <div>
            <span>{{ translateUiText("数据表名") }}</span>
            <code>{{ tableName || '-' }}</code>
          </div>
          <div>
            <span>{{ translateUiText("运行配置键") }}</span>
            <code>{{ configKey || '-' }}</code>
          </div>
        </section>

        <n-tabs type="line" animated>
          <n-tab-pane name="fields" :tab="translateUiText(&quot;字段技术属性&quot;)">
            <div class="advanced-table-wrap">
              <table class="advanced-table">
                <thead>
                  <tr>
                    <th>{{ translateUiText("字段") }}</th>
                    <th>{{ translateUiText("编码") }}</th>
                    <th>{{ translateUiText("列名") }}</th>
                    <th>{{ translateUiText("数据类型") }}</th>
                    <th>{{ translateUiText("控件") }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="field in modelFields" :key="field.field">
                    <td>{{ field.label || field.comment || field.field }}</td>
                    <td><code>{{ field.field || '-' }}</code></td>
                    <td><code>{{ field.columnName || '-' }}</code></td>
                    <td>{{ field.dataType || '-' }}</td>
                    <td>{{ field.componentType || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </n-tab-pane>

          <n-tab-pane name="schema" :tab="translateUiText(&quot;Schema 预览&quot;)">
            <div class="schema-grid">
              <section>
                <h4>modelSchema</h4>
                <pre>{{ formatJson(draft.modelSchema) }}</pre>
              </section>
              <section>
                <h4>pageSchema</h4>
                <pre>{{ formatJson(draft.pageSchema) }}</pre>
              </section>
            </div>
          </n-tab-pane>

          <n-tab-pane name="api" :tab="translateUiText(&quot;接口说明&quot;)">
            <div class="api-list">
              <div v-for="item in apiConfig" :key="item.label">
                <span>{{ translateUiText(item.label, item?.labelI18n) }}</span>
                <code>{{ item.value }}</code>
              </div>
            </div>
          </n-tab-pane>
        </n-tabs>
      </template>
    </div>
  </div>
</template>

<script setup>
import { localizeLabel as translateUiText } from '@/locales'

import { computed } from 'vue'

const props = defineProps({
  draft: {
    type: Object,
    default: () => ({}),
  },
  developerMode: {
    type: Boolean,
    default: false,
  },
  canAdvanced: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['update:developerMode', 'openDeveloper'])

const modelFields = computed(() => props.draft?.modelSchema?.fields || props.draft?.fields || [])
const fieldCount = computed(() => modelFields.value.length)
const zoneCount = computed(() => props.draft?.pageSchema?.zones?.length || 0)
const modelCode = computed(() => props.draft?.modelSchema?.object?.code || props.draft?.modelSchema?.modelCode || props.draft?.objectCode || '')
const tableName = computed(() => props.draft?.modelSchema?.tableName || '')
const configKey = computed(() => props.draft?.configKey || props.draft?.designerOptions?.configKey || '')
const businessApiBase = computed(() => props.draft?.designerOptions?.businessApiBase || defaultBusinessApiBase())
const apiConfig = computed(() => {
  return [
    { label: '在线运行', value: '由平台托管接口自动承载，发布后业务页面可直接使用。' },
    { label: '下载代码', value: `默认业务接口前缀：${businessApiBase.value}` },
  ]
})

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2)
}

function defaultBusinessApiBase() {
  const suite = String(props.draft?.suiteCode || props.draft?.domainCode || 'business')
    .toLowerCase()
    .replace(/_/g, '-')
  const object = String(props.draft?.objectCode || 'object')
    .toLowerCase()
    .replace(/_/g, '-')
  return `/${suite}/${object}`
}
</script>

<style scoped>
.business-advanced-config {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.advanced-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.advanced-head h3,
.advanced-safe-card h4,
.schema-grid h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.advanced-head p,
.advanced-safe-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.advanced-body {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.advanced-safe-card,
.advanced-grid div,
.schema-grid section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.safe-summary,
.advanced-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.safe-summary div {
  border-radius: 6px;
  background: #f1f5f9;
  padding: 12px;
}

.safe-summary span,
.advanced-grid span,
.api-list span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.safe-summary strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 16px;
}

.advanced-grid code,
.api-list code {
  display: block;
  overflow: hidden;
  margin-top: 6px;
  color: #111827;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.developer-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin: 14px 0;
}

.developer-actions button {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #fff;
  color: #1d4ed8;
  cursor: pointer;
  padding: 14px;
  text-align: left;
}

.developer-actions button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.developer-actions strong {
  display: block;
  color: #111827;
  font-size: 14px;
}

.developer-actions span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.advanced-table-wrap {
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.advanced-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.advanced-table th,
.advanced-table td {
  border-bottom: 1px solid #eef2f7;
  color: #334155;
  padding: 10px 12px;
  text-align: left;
  white-space: nowrap;
}

.advanced-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.schema-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 12px;
}

.schema-grid pre {
  max-height: 420px;
  overflow: auto;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.6;
  margin: 10px 0 0;
  padding: 12px;
}

.api-list {
  display: grid;
  gap: 10px;
}

.api-list div {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}
</style>
