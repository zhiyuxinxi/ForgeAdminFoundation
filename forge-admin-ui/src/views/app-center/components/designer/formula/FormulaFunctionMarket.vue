<template>
  <n-modal
    :show="show"
    preset="card"
    class="formula-function-market-modal"
    :title="translateUiText(&quot;公式函数市场&quot;)"
    :bordered="false"
    @update:show="emit('update:show', $event)"
  >
    <section class="function-market">
      <header class="market-toolbar">
        <n-input
          v-model:value="query.keyword"
          clearable
          :placeholder="translateUiText(&quot;搜索函数编码、名称或说明&quot;)"
          @keyup.enter="handleSearch"
        />
        <n-select
          v-model:value="query.category"
          :options="categoryOptions"
          clearable
          :placeholder="translateUiText(&quot;分类&quot;)"
        />
        <n-button secondary :loading="loading" @click="handleSearch">
          <template #icon>
            <n-icon><SearchOutline /></n-icon>
          </template>
          {{ translateUiText("搜索") }}
        </n-button>
        <n-button type="primary" @click="openRegisterModal">
          <template #icon>
            <n-icon><AddOutline /></n-icon>
          </template>
          {{ translateUiText("注册函数") }}
        </n-button>
      </header>

      <div class="market-workbench">
        <section class="market-list-panel">
          <div v-if="loading" class="market-loading">
            <span />
          </div>
          <n-empty
            v-else-if="!records.length"
            :description="translateUiText(&quot;暂无函数&quot;)"
          />
          <div v-else class="market-function-list">
            <div
              v-for="item in records"
              :key="item.functionCode"
              role="button"
              tabindex="0"
              class="market-function-row"
              :class="{ active: selected?.functionCode === item.functionCode }"
              @click="selectFunction(item)"
              @keydown.enter="selectFunction(item)"
            >
              <span class="row-main">
                <strong>{{ item.functionCode }}</strong>
                <em>{{ item.displayName || item.description || '-' }}</em>
              </span>
              <span class="row-tags">
                <n-tag size="small" :bordered="false">
                  {{ item.category || 'Other' }}
                </n-tag>
                <n-tag size="small" :type="item.enabled ? 'success' : 'warning'" :bordered="false">
                  {{ installStatusLabel(item) }}
                </n-tag>
              </span>
              <span class="row-actions">
                <n-button
                  v-if="!isInstalled(item)"
                  size="tiny"
                  type="primary"
                  secondary
                  :loading="actingCode === item.functionCode"
                  @click.stop="installFunction(item)"
                >
                  {{ translateUiText("安装") }}
                </n-button>
                <n-button
                  v-else-if="item.enabled"
                  size="tiny"
                  secondary
                  :loading="actingCode === item.functionCode"
                  @click.stop="disableFunction(item)"
                >
                  {{ translateUiText("禁用") }}
                </n-button>
                <n-button
                  v-else
                  size="tiny"
                  type="success"
                  secondary
                  :loading="actingCode === item.functionCode"
                  @click.stop="enableFunction(item)"
                >
                  {{ translateUiText("启用") }}
                </n-button>
              </span>
            </div>
          </div>

          <n-pagination
            v-if="total > query.pageSize"
            v-model:page="query.pageNum"
            v-model:page-size="query.pageSize"
            :item-count="total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            @update:page="loadPage"
            @update:page-size="handlePageSizeChange"
          />
        </section>

        <aside class="market-detail-panel">
          <n-empty v-if="!selected" :description="translateUiText(&quot;选择函数查看详情&quot;)" />
          <template v-else>
            <div class="detail-head">
              <div>
                <span>{{ selected.category || 'Other' }}</span>
                <strong>{{ selected.displayName || selected.functionCode }}</strong>
              </div>
              <n-tag :type="selected.enabled ? 'success' : 'warning'" :bordered="false">
                {{ installStatusLabel(selected) }}
              </n-tag>
            </div>

            <dl class="detail-grid">
              <div>
                <dt>{{ translateUiText("函数编码") }}</dt>
                <dd>{{ selected.functionCode }}</dd>
              </div>
              <div>
                <dt>{{ translateUiText("来源") }}</dt>
                <dd>{{ selected.sourceType || '-' }}</dd>
              </div>
              <div>
                <dt>{{ translateUiText("当前版本") }}</dt>
                <dd>{{ selected.currentVersion || '-' }}</dd>
              </div>
              <div>
                <dt>{{ translateUiText("已安装版本") }}</dt>
                <dd>{{ selected.installedVersion || '-' }}</dd>
              </div>
              <div>
                <dt>{{ translateUiText("返回类型") }}</dt>
                <dd>{{ selected.returnType || '-' }}</dd>
              </div>
              <div>
                <dt>{{ translateUiText("最新版本") }}</dt>
                <dd>{{ selected.latestVersion || '-' }}</dd>
              </div>
              <div>
                <dt>{{ translateUiText("实现方式") }}</dt>
                <dd>{{ selected.implementationType || '-' }}</dd>
              </div>
              <div>
                <dt>{{ translateUiText("Bean 方法") }}</dt>
                <dd>{{ formatBeanMethod(selected) }}</dd>
              </div>
            </dl>

            <div class="detail-section">
              <span>{{ translateUiText("说明") }}</span>
              <p>{{ selected.description || '-' }}</p>
            </div>
            <div class="detail-section">
              <span>{{ translateUiText("参数 Schema") }}</span>
              <code>{{ selected.argumentSchema || '[]' }}</code>
            </div>
            <div class="detail-section">
              <span>{{ translateUiText("示例") }}</span>
              <code>{{ selected.example || '-' }}</code>
            </div>

            <div class="detail-actions">
              <n-button
                v-if="!isInstalled(selected)"
                type="primary"
                :loading="actingCode === selected.functionCode"
                @click="installFunction(selected)"
              >
                {{ translateUiText("安装函数") }}
              </n-button>
              <n-button
                v-else-if="selected.enabled"
                secondary
                :loading="actingCode === selected.functionCode"
                @click="disableFunction(selected)"
              >
                {{ translateUiText("禁用函数") }}
              </n-button>
              <n-button
                v-else
                type="success"
                secondary
                :loading="actingCode === selected.functionCode"
                @click="enableFunction(selected)"
              >
                {{ translateUiText("启用函数") }}
              </n-button>
            </div>
          </template>
        </aside>
      </div>
    </section>
  </n-modal>

  <n-modal
    v-model:show="registerVisible"
    preset="card"
    class="function-register-modal"
    :title="translateUiText(&quot;注册自定义函数&quot;)"
    :bordered="false"
    :mask-closable="!registering"
  >
    <n-form label-placement="top" size="small" :show-feedback="false">
      <n-grid :cols="2" :x-gap="12">
        <n-form-item-gi :label="translateUiText(&quot;函数编码&quot;)">
          <n-input v-model:value="registerForm.functionCode" :placeholder="translateUiText(&quot;例如：finance.discountRate&quot;)" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;展示名称&quot;)">
          <n-input v-model:value="registerForm.displayName" :placeholder="translateUiText(&quot;例如：折扣率计算&quot;)" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;分类&quot;)">
          <n-input v-model:value="registerForm.category" :placeholder="translateUiText(&quot;例如：Finance&quot;)" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;返回类型&quot;)">
          <n-select v-model:value="registerForm.returnType" :options="translateUiMetadata(returnTypeOptions)" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;Spring Bean 名称&quot;)">
          <n-input v-model:value="registerForm.beanName" :placeholder="translateUiText(&quot;例如：financeFormulaFunctionProvider&quot;)" />
        </n-form-item-gi>
        <n-form-item-gi :label="translateUiText(&quot;方法名称&quot;)">
          <n-input v-model:value="registerForm.methodName" :placeholder="translateUiText(&quot;例如：discountRate&quot;)" />
        </n-form-item-gi>
      </n-grid>
      <n-form-item :label="translateUiText(&quot;函数说明&quot;)">
        <n-input v-model:value="registerForm.description" type="textarea" :rows="2" :placeholder="translateUiText(&quot;说明函数用途、边界和适用场景&quot;)" />
      </n-form-item>
      <n-form-item :label="translateUiText(&quot;参数 Schema&quot;)">
        <n-input v-model:value="registerForm.argumentSchema" type="textarea" :rows="5" />
      </n-form-item>
      <n-form-item :label="translateUiText(&quot;调用示例&quot;)">
        <n-input v-model:value="registerForm.example" :placeholder="translateUiText(&quot;例如：finance.discountRate(amount, level)&quot;)" />
      </n-form-item>
    </n-form>
    <template #footer>
      <div class="register-footer">
        <n-checkbox v-model:checked="registerForm.enabled">
          {{ translateUiText("注册后立即启用") }}
        </n-checkbox>
        <div>
          <n-button :disabled="registering" @click="registerVisible = false">
            {{ translateUiText("取消") }}
          </n-button>
          <n-button type="primary" :loading="registering" @click="registerFunction">
            {{ translateUiText("保存注册") }}
          </n-button>
        </div>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

import { AddOutline, SearchOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import {
  disableFormulaFunction,
  enableFormulaFunction,
  getFormulaFunctionMarketDetail,
  getFormulaFunctionMarketPage,
  installFormulaFunction,
  registerFormulaFunction,
} from '@/api/formula'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show'])
const message = useMessage()

const loading = ref(false)
const actingCode = ref('')
const registering = ref(false)
const registerVisible = ref(false)
const records = ref([])
const total = ref(0)
const selected = ref(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  category: null,
})
const registerForm = reactive(createDefaultRegisterForm())

const returnTypeOptions = [
  { label: '任意', value: 'ANY' },
  { label: '数字', value: 'NUMBER' },
  { label: '文本', value: 'STRING' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '日期', value: 'DATE' },
  { label: '集合', value: 'COLLECTION' },
  { label: '映射', value: 'MAP' },
]

const categoryOptions = computed(() => {
  const categories = Array.from(new Set(records.value
    .map(item => item.category)
    .filter(Boolean)))
    .sort((a, b) => a.localeCompare(b))
  return categories.map(item => ({ label: item, value: item }))
})

watch(
  () => props.show,
  (visible) => {
    if (visible)
      loadPage()
  },
)

async function loadPage() {
  loading.value = true
  try {
    const res = await getFormulaFunctionMarketPage({ ...query })
    const page = res?.data ?? res ?? {}
    records.value = Array.isArray(page.records) ? page.records : []
    total.value = Number(page.total || records.value.length || 0)
    if (!selected.value && records.value.length)
      await selectFunction(records.value[0])
    else if (selected.value)
      syncSelectedFromRecords()
  }
  catch (e) {
    records.value = []
    total.value = 0
    selected.value = null
    message.error(e?.message || '函数市场加载失败')
  }
  finally {
    loading.value = false
  }
}

async function selectFunction(item) {
  if (!item?.functionCode)
    return
  selected.value = item
  try {
    const res = await getFormulaFunctionMarketDetail(item.functionCode)
    selected.value = res?.data ?? res ?? item
  }
  catch {
    selected.value = item
  }
}

function syncSelectedFromRecords() {
  const matched = records.value.find(item => item.functionCode === selected.value.functionCode)
  if (matched)
    selected.value = matched
}

function handleSearch() {
  query.pageNum = 1
  loadPage()
}

function handlePageSizeChange() {
  query.pageNum = 1
  loadPage()
}

async function installFunction(item) {
  await runAction(item, async () => {
    await installFormulaFunction({
      functionCode: item.functionCode,
      version: item.latestVersion || item.currentVersion || '1.0.0',
      enabled: true,
    })
    message.success(translateUiText("函数已安装"))
  })
}

async function enableFunction(item) {
  await runAction(item, async () => {
    await enableFormulaFunction(item.functionCode)
    message.success(translateUiText("函数已启用"))
  })
}

async function disableFunction(item) {
  await runAction(item, async () => {
    await disableFormulaFunction(item.functionCode)
    message.success(translateUiText("函数已禁用"))
  })
}

function openRegisterModal() {
  Object.assign(registerForm, createDefaultRegisterForm())
  registerVisible.value = true
}

async function registerFunction() {
  if (!registerForm.functionCode?.trim()) {
    message.warning(translateUiText("请输入函数编码"))
    return
  }
  if (!registerForm.displayName?.trim()) {
    message.warning(translateUiText("请输入展示名称"))
    return
  }
  if (!registerForm.beanName?.trim() || !registerForm.methodName?.trim()) {
    message.warning(translateUiText("请输入 Java Bean 名称和方法名称"))
    return
  }
  try {
    JSON.parse(registerForm.argumentSchema || '[]')
  }
  catch {
    message.warning(translateUiText("参数 Schema 必须是 JSON 数组"))
    return
  }
  registering.value = true
  try {
    await registerFormulaFunction({ ...registerForm })
    registerVisible.value = false
    message.success(translateUiText("自定义函数已注册"))
    query.pageNum = 1
    query.keyword = registerForm.functionCode
    query.category = null
    await loadPage()
    const current = records.value.find(row => row.functionCode === registerForm.functionCode)
    if (current)
      await selectFunction(current)
  }
  catch (e) {
    message.error(e?.message || '自定义函数注册失败')
  }
  finally {
    registering.value = false
  }
}

async function runAction(item, action) {
  if (!item?.functionCode)
    return
  actingCode.value = item.functionCode
  try {
    await action()
    await loadPage()
    const current = records.value.find(row => row.functionCode === item.functionCode)
    if (current)
      await selectFunction(current)
  }
  catch (e) {
    message.error(e?.message || '函数操作失败')
  }
  finally {
    actingCode.value = ''
  }
}

function isInstalled(item = {}) {
  return String(item.installStatus || '').toUpperCase() === 'INSTALLED'
}

function installStatusLabel(item = {}) {
  if (!isInstalled(item))
    return '未安装'
  return item.enabled ? '已启用' : '已禁用'
}

function formatBeanMethod(item = {}) {
  if (!item.beanName && !item.methodName)
    return '-'
  return `${item.beanName || '-'}#${item.methodName || '-'}`
}

function createDefaultRegisterForm() {
  return {
    functionCode: '',
    displayName: '',
    category: 'Custom',
    description: '',
    argumentSchema: '[{\"name\":\"value\",\"type\":\"NUMBER\",\"required\":true}]',
    returnType: 'ANY',
    example: '',
    version: '1.0.0',
    beanName: '',
    methodName: '',
    releaseNote: '初始版本',
    enabled: true,
  }
}
</script>

<style scoped>
.function-market {
  display: grid;
  gap: 14px;
}

.market-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px auto auto;
  gap: 10px;
  align-items: center;
}

.market-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.85fr);
  gap: 14px;
  min-height: 520px;
}

.market-list-panel,
.market-detail-panel {
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
}

.market-list-panel {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 12px;
}

.market-function-list {
  display: grid;
  align-content: start;
  gap: 8px;
  overflow-y: auto;
  padding-right: 2px;
}

.market-function-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  min-height: 68px;
  border: 1px solid #dbe3ef;
  border-radius: 7px;
  background: #fff;
  cursor: pointer;
  padding: 10px;
  text-align: left;
}

.market-function-row:hover,
.market-function-row.active {
  border-color: #93c5fd;
  background: #f8fbff;
}

.row-main {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.row-main strong {
  overflow: hidden;
  color: #111827;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-main em {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-tags,
.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.market-detail-panel {
  display: grid;
  align-content: start;
  gap: 14px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 12px;
}

.detail-head div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.detail-head span,
.detail-section span {
  color: #64748b;
  font-size: 12px;
}

.detail-head strong {
  color: #111827;
  font-size: 18px;
  line-height: 1.35;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.detail-grid div {
  min-width: 0;
  border-radius: 7px;
  background: #fff;
  padding: 9px 10px;
}

.detail-grid dt {
  color: #64748b;
  font-size: 12px;
}

.detail-grid dd {
  overflow: hidden;
  margin: 4px 0 0;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-section {
  display: grid;
  gap: 6px;
}

.detail-section p,
.detail-section code {
  margin: 0;
  border-radius: 7px;
  background: #fff;
  color: #334155;
  font-size: 13px;
  line-height: 1.6;
  padding: 10px;
}

.detail-section code {
  overflow-x: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  white-space: pre;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.market-loading {
  display: grid;
  place-items: center;
}

.market-loading span {
  width: 24px;
  height: 24px;
  border: 2px solid #d6dde8;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

:global(.formula-function-market-modal) {
  width: min(1120px, calc(100vw - 32px));
}

:global(.function-register-modal) {
  width: min(720px, calc(100vw - 32px));
}

.register-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.register-footer > div {
  display: flex;
  gap: 8px;
}

@media (max-width: 900px) {
  .market-toolbar,
  .market-workbench {
    grid-template-columns: 1fr;
  }

  .market-workbench {
    min-height: auto;
  }

  .market-function-row {
    grid-template-columns: minmax(0, 1fr);
    align-items: stretch;
  }

  .row-tags,
  .row-actions {
    justify-content: flex-start;
  }
}
</style>
