<template>
  <div class="condition-rule-page">
    <!-- 搜索和操作区 -->
    <n-card size="small" class="mb-4">
      <n-form :model="searchForm" inline label-placement="left">
        <n-form-item :label="translateUiText(&quot;规则名称&quot;)">
          <n-input v-model:value="searchForm.ruleName" :placeholder="translateUiText(&quot;请输入规则名称&quot;)" clearable style="width: 200px" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;规则类型&quot;)">
          <n-select
            v-model:value="searchForm.ruleType"
            :options="ruleTypeOptions"
            :placeholder="translateUiText(&quot;请选择类型&quot;)"
            clearable
            style="width: 150px"
          />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;状态&quot;)">
          <n-select
            v-model:value="searchForm.status"
            :options="statusOptions"
            :placeholder="translateUiText(&quot;请选择状态&quot;)"
            clearable
            style="width: 120px"
          />
        </n-form-item>
        <n-form-item>
          <NSpace>
            <NButton type="primary" @click="handleSearch">
              <template #icon>
                <i class="i-mdi:magnify" />
              </template>
              {{ translateUiText("查询") }}
            </NButton>
            <NButton @click="handleReset">
              <template #icon>
                <i class="i-mdi:refresh" />
              </template>
              {{ translateUiText("重置") }}
            </NButton>
          </NSpace>
        </n-form-item>
      </n-form>
    </n-card>

    <!-- 表格 -->
    <n-card size="small">
      <template #header>
        <span>{{ translateUiText("条件规则列表") }}</span>
      </template>
      <template #header-extra>
        <NButton type="primary" @click="handleAdd">
          <template #icon>
            <i class="i-mdi:plus" />
          </template>
          {{ translateUiText("新增规则") }}
        </NButton>
      </template>

      <n-data-table
        :columns="translateUiMetadata(columns)"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        @update:page="handlePageChange"
      />
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="modalVisible" preset="card" :title="modalTitle" :style="{ width: '800px' }">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item :label="translateUiText(&quot;规则名称&quot;)" path="ruleName">
          <n-input v-model:value="formData.ruleName" :placeholder="translateUiText(&quot;请输入规则名称&quot;)" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;规则编码&quot;)" path="ruleCode">
          <n-input v-model:value="formData.ruleCode" :placeholder="translateUiText(&quot;请输入规则编码&quot;)" :disabled="isEdit" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;规则类型&quot;)" path="ruleType">
          <n-select v-model:value="formData.ruleType" :options="ruleTypeOptions" :placeholder="translateUiText(&quot;请选择规则类型&quot;)" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;关联流程&quot;)" path="modelId">
          <n-select
            v-model:value="formData.modelId"
            :options="modelOptions"
            :placeholder="translateUiText(&quot;请选择关联流程&quot;)"
            filterable
            clearable
          />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;关联节点&quot;)" path="nodeId">
          <n-input v-model:value="formData.nodeId" :placeholder="translateUiText(&quot;请输入关联节点ID（可选）&quot;)" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;条件表达式&quot;)" path="conditionExpression">
          <n-input
            v-model:value="formData.conditionExpression"
            type="textarea"
            :rows="3"
            :placeholder="translateUiText(&quot;请输入SpEL条件表达式，如：${amount > 10000 &amp;&amp; deptLevel == 'A'}&quot;)"
          />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;条件项&quot;)">
          <div class="condition-items">
            <div v-for="(item, index) in formData.conditionItems" :key="index" class="condition-item">
              <n-select
                v-model:value="item.fieldName"
                :options="fieldOptions"
                :placeholder="translateUiText(&quot;字段&quot;)"
                style="width: 150px"
              />
              <n-select
                v-model:value="item.operator"
                :options="operatorOptions"
                :placeholder="translateUiText(&quot;操作符&quot;)"
                style="width: 100px"
              />
              <n-input v-model:value="item.fieldValue" :placeholder="translateUiText(&quot;值&quot;)" style="width: 150px" />
              <NButton text type="error" @click="removeConditionItem(index)">
                <i class="i-mdi:delete" />
              </NButton>
            </div>
            <NButton dashed block @click="addConditionItem">
              <template #icon>
                <i class="i-mdi:plus" />
              </template>
              {{ translateUiText("添加条件项") }}
            </NButton>
          </div>
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;优先级&quot;)" path="priority">
          <n-input-number v-model:value="formData.priority" :min="0" :max="999" :placeholder="translateUiText(&quot;数值越小优先级越高&quot;)" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;状态&quot;)" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>
              {{ translateUiText("启用") }}
            </template>
            <template #unchecked>
              {{ translateUiText("禁用") }}
            </template>
          </n-switch>
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;备注&quot;)" path="remark">
          <n-input v-model:value="formData.remark" type="textarea" :rows="2" :placeholder="translateUiText(&quot;请输入备注&quot;)" />
        </n-form-item>
      </n-form>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="modalVisible = false">
            {{ translateUiText("取消") }}
          </NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ translateUiText("确定") }}
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 测试规则弹窗 -->
    <n-modal v-model:show="testModalVisible" preset="card" :title="translateUiText(&quot;测试规则&quot;)" :style="{ width: '600px' }">
      <n-form :model="testForm" label-placement="left" label-width="80">
        <n-form-item :label="translateUiText(&quot;测试数据&quot;)">
          <n-input
            v-model:value="testForm.testData"
            type="textarea"
            :rows="6"
            :placeholder="translateUiText(&quot;请输入JSON格式的测试数据&quot;)"
          />
        </n-form-item>
      </n-form>

      <n-divider>{{ translateUiText("测试结果") }}</n-divider>

      <n-alert v-if="testResult !== null" :type="testResult ? 'success' : 'warning'">
        {{ testResult ? translateUiText("条件满足，规则匹配成功") : translateUiText("条件不满足，规则匹配失败") }}
      </n-alert>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="testModalVisible = false">
            {{ translateUiText("关闭") }}
          </NButton>
          <NButton type="primary" :loading="testLoading" @click="handleTest">
            {{ translateUiText("执行测试") }}
          </NButton>
        </NSpace>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

// 引入必要的组件
import { NButton, NSpace } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'

import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'FlowConditionRule' })

const message = window.$message
const { dict } = useDict('flow_rule_type', 'flow_rule_operator', 'sys_enable_disable')

// 搜索表单
const searchForm = reactive({
  ruleName: '',
  ruleType: null,
  status: null,
})

// 选项
const ruleTypeOptions = computed(() => dict.value.flow_rule_type || [])
const statusOptions = computed(() => toNumberOptions(dict.value.sys_enable_disable))
const operatorOptions = computed(() => dict.value.flow_rule_operator || [])

const fieldOptions = ref([
  { label: '金额', value: 'amount' },
  { label: '部门级别', value: 'deptLevel' },
  { label: '请假天数', value: 'leaveDays' },
  { label: '处理状态', value: 'approvalStatus' },
])

const modelOptions = ref([])

// 表格数据
const tableData = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

// 表格列定义
const columns = [
  {
    title: '规则名称',
    key: 'ruleName',
    ellipsis: { tooltip: true },
  },
  {
    title: '规则编码',
    key: 'ruleCode',
    width: 150,
  },
  {
    title: '规则类型',
    key: 'ruleType',
    width: 100,
    render(row) {
      return h(DictTag, { dictType: 'flow_rule_type', value: row.ruleType })
    },
  },
  {
    title: '优先级',
    key: 'priority',
    width: 80,
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(DictTag, { dictType: 'sys_enable_disable', value: row.status })
    },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { text: true, type: 'primary', onClick: () => handleEdit(row) }, { default: () => translateUiText("编辑") }),
          h(NButton, { text: true, type: 'info', onClick: () => handleTestRule(row) }, { default: () => translateUiText("测试") }),
          h(NButton, { text: true, type: 'error', onClick: () => handleDelete(row) }, { default: () => translateUiText("删除") }),
        ],
      })
    },
  },
]

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('新增规则')
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)

const formData = reactive({
  id: null,
  ruleName: '',
  ruleCode: '',
  ruleType: null,
  modelId: null,
  nodeId: '',
  conditionExpression: '',
  conditionItems: [],
  priority: 100,
  status: 1,
  remark: '',
})

const formRules = {
  ruleName: { required: true, message: '请输入规则名称', trigger: 'blur' },
  ruleCode: { required: true, message: '请输入规则编码', trigger: 'blur' },
  ruleType: { required: true, message: '请选择规则类型', trigger: 'change' },
}

// 测试弹窗
const testModalVisible = ref(false)
const testLoading = ref(false)
const testForm = reactive({
  testData: '',
})
const testResult = ref(null)
const currentTestRule = ref(null)

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

// 加载流程模型列表
async function loadModels() {
  try {
    const res = await request.get('/api/flow/model/list')
    if (res.code === 200) {
      modelOptions.value = (res.data || []).map(item => ({
        label: item.modelName,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('加载流程模型失败', error)
  }
}

// 加载表格数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...searchForm,
    }
    const res = await request.get('/api/flow/conditionRule/page', { params })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  }
  catch (error) {
    console.error('加载数据失败', error)
  }
  finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  Object.assign(searchForm, {
    ruleName: '',
    ruleType: null,
    status: null,
  })
  handleSearch()
}

// 分页变化
function handlePageChange(page) {
  pagination.page = page
  loadData()
}

// 新增
function handleAdd() {
  modalTitle.value = '新增规则'
  isEdit.value = false
  Object.assign(formData, {
    id: null,
    ruleName: '',
    ruleCode: '',
    ruleType: null,
    modelId: null,
    nodeId: '',
    conditionExpression: '',
    conditionItems: [],
    priority: 100,
    status: 1,
    remark: '',
  })
  modalVisible.value = true
}

// 编辑
function handleEdit(row) {
  modalTitle.value = '编辑规则'
  isEdit.value = true
  Object.assign(formData, {
    ...row,
    conditionItems: row.conditionItems || [],
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(row) {
  try {
    const res = await request.delete(`/api/flow/conditionRule/${row.id}`)
    if (res.code === 200) {
      message.success(translateUiText("删除成功"))
      loadData()
    }
  }
  catch {
    message.error(translateUiText("删除失败"))
  }
}

// 提交
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }

  submitLoading.value = true
  try {
    const url = isEdit.value ? '/api/flow/conditionRule' : '/api/flow/conditionRule'
    const method = isEdit.value ? 'put' : 'post'
    const res = await request[method](url, formData)
    if (res.code === 200) {
      message.success(isEdit.value ? '修改成功' : '新增成功')
      modalVisible.value = false
      loadData()
    }
  }
  catch {
    message.error(translateUiText("操作失败"))
  }
  finally {
    submitLoading.value = false
  }
}

// 添加条件项
function addConditionItem() {
  formData.conditionItems.push({
    fieldName: '',
    operator: '',
    fieldValue: '',
  })
}

// 移除条件项
function removeConditionItem(index) {
  formData.conditionItems.splice(index, 1)
}

// 测试规则
function handleTestRule(row) {
  currentTestRule.value = row
  testForm.testData = ''
  testResult.value = null
  testModalVisible.value = true
}

// 执行测试
async function handleTest() {
  if (!testForm.testData) {
    message.warning(translateUiText("请输入测试数据"))
    return
  }

  testLoading.value = true
  try {
    const variables = JSON.parse(testForm.testData)
    const res = await request.post('/api/flow/conditionRule/test', {
      ruleId: currentTestRule.value.id,
      variables,
    })
    if (res.code === 200) {
      testResult.value = res.data
    }
  }
  catch {
    message.error(translateUiText("测试失败，请检查数据格式"))
  }
  finally {
    testLoading.value = false
  }
}

onMounted(() => {
  loadModels()
  loadData()
})
</script>

<style scoped>
.condition-rule-page {
  padding: 16px;
}

.mb-4 {
  margin-bottom: 16px;
}

.condition-items {
  width: 100%;
}

.condition-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>
