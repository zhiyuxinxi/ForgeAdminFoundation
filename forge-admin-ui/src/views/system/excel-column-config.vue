<template>
  <div class="excel-column-config">
    <div class="mb-16 flex items-center justify-between">
      <div class="text-16 font-bold">
        {{ configName }} {{ translateUiText("- 列配置") }}
        <span class="text-13 ml-8 text-gray-500 font-normal">{{ configTypeLabel }}</span>
      </div>
      <div class="flex gap-8">
        <NButton type="primary" size="small" @click="handleAdd">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          {{ translateUiText("添加列") }}
        </NButton>
        <NButton type="success" size="small" @click="handleSave">
          <template #icon>
            <i class="i-material-symbols:save" />
          </template>
          {{ translateUiText("保存配置") }}
        </NButton>
      </div>
    </div>

    <NDataTable
      :columns="translateUiMetadata(columns)"
      :data="dataList"
      :row-key="row => row.key"
      :pagination="false"
      :max-height="500"
      size="small"
    />

    <!-- 编辑弹窗 -->
    <NModal
      v-model:show="showEditModal"
      preset="dialog"
      :title="translateUiText(&quot;编辑列配置&quot;)"
      :positive-text="translateUiText(&quot;确定&quot;)"
      :negative-text="translateUiText(&quot;取消&quot;)"
      :style="{ width: '720px' }"
      @positive-click="handleConfirmEdit"
    >
      <NForm
        ref="editFormRef"
        :model="editForm"
        label-placement="left"
        label-width="100"
      >
        <NFormItem
          :label="translateUiText(&quot;字段名&quot;)"
          path="fieldName"
          :rule="{ required: true, message: '请输入字段名', trigger: 'blur' }"
        >
          <NInput
            v-model:value="editForm.fieldName"
            :placeholder="translateUiText(&quot;实体属性名，如：userId&quot;)"
          />
        </NFormItem>
        <NFormItem
          :label="translateUiText(&quot;列名&quot;)"
          path="columnName"
          :rule="{ required: true, message: '请输入列名', trigger: 'blur' }"
        >
          <NInput
            v-model:value="editForm.columnName"
            :placeholder="translateUiText(&quot;Excel表头名称，如：用户ID&quot;)"
          />
        </NFormItem>
        <NFormItem :label="translateUiText(&quot;英文列名 / English header&quot;)" path="columnNameEn">
          <NInput v-model:value="editForm.columnNameEn" placeholder="English header (optional)" :maxlength="255" />
        </NFormItem>
        <NFormItem :label="translateUiText(&quot;列宽&quot;)" path="width">
          <NInputNumber
            v-model:value="editForm.width"
            :placeholder="translateUiText(&quot;列宽&quot;)"
            :min="5"
            :max="100"
            style="width: 100%"
          />
        </NFormItem>
        <NFormItem :label="translateUiText(&quot;排序&quot;)" path="orderNum">
          <NInputNumber
            v-model:value="editForm.orderNum"
            :placeholder="translateUiText(&quot;排序值，越小越靠前&quot;)"
            :min="0"
            style="width: 100%"
          />
        </NFormItem>
        <NFormItem v-if="exportEnabled" :label="translateUiText(&quot;是否导出&quot;)" path="export">
          <NSwitch v-model:value="editForm.export" :disabled="!exportEnabled" />
        </NFormItem>
        <NFormItem v-if="importEnabled" :label="translateUiText(&quot;是否可导入&quot;)" path="importable">
          <NSwitch v-model:value="editForm.importable" :disabled="!importEnabled" />
        </NFormItem>
        <NFormItem v-if="importEnabled" :label="translateUiText(&quot;导入必填&quot;)" path="required">
          <NSwitch v-model:value="editForm.required" :disabled="!importEnabled" />
        </NFormItem>
        <NFormItem :label="translateUiText(&quot;日期格式&quot;)" path="dateFormat">
          <NInput
            v-model:value="editForm.dateFormat"
            :placeholder="translateUiText(&quot;如：yyyy-MM-dd HH:mm:ss&quot;)"
          />
        </NFormItem>
        <NFormItem :label="translateUiText(&quot;数字格式&quot;)" path="numberFormat">
          <NInput
            v-model:value="editForm.numberFormat"
            :placeholder="translateUiText(&quot;如：#,##0.00&quot;)"
          />
        </NFormItem>
        <NFormItem :label="translateUiText(&quot;字典类型&quot;)" path="dictType">
          <NInput
            v-model:value="editForm.dictType"
            :placeholder="translateUiText(&quot;字典类型，如：user_status&quot;)"
          />
        </NFormItem>
        <NFormItem v-if="importEnabled" :label="translateUiText(&quot;示例值&quot;)" path="exampleValue">
          <NInput
            v-model:value="editForm.exampleValue"
            :disabled="!importEnabled"
            :placeholder="translateUiText(&quot;导入模板示例值&quot;)"
          />
        </NFormItem>
        <NFormItem v-if="importEnabled" :label="translateUiText(&quot;校验规则&quot;)" path="validationRule">
          <NInput
            v-model:value="editForm.validationRule"
            :disabled="!importEnabled"
            :placeholder="translateUiText(&quot;正则表达式，如：^.{1,64}$&quot;)"
          />
        </NFormItem>
        <NFormItem v-if="importEnabled" :label="translateUiText(&quot;校验提示&quot;)" path="validationMessage">
          <NInput
            v-model:value="editForm.validationMessage"
            :disabled="!importEnabled"
            :placeholder="translateUiText(&quot;校验失败提示信息&quot;)"
          />
        </NFormItem>
      </NForm>
    </NModal>
  </div>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { formatUiText } from '@/locales'

import { localizeLabel as translateUiText } from '@/locales'

import { NButton, NDataTable, NForm, NFormItem, NInput, NInputNumber, NModal, NSwitch } from 'naive-ui'
import { computed, h, onMounted, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

const props = defineProps({
  configKey: {
    type: String,
    required: true,
  },
  configName: {
    type: String,
    default: '',
  },
  configType: {
    type: String,
    default: 'BOTH',
  },
})

const YES_NO_DICT = 'sys_yes_no'
const CONFIG_TYPE_DICT = 'sys_excel_config_type'

const dataList = ref([])
const showEditModal = ref(false)
const editFormRef = ref(null)
const { dict, getLabel } = useDict(YES_NO_DICT, CONFIG_TYPE_DICT)
const yesNoOptions = computed(() => dict.value[YES_NO_DICT] || [])
const exportEnabled = computed(() => props.configType !== 'IMPORT')
const importEnabled = computed(() => props.configType !== 'EXPORT')
const configTypeLabel = computed(() => getLabel(CONFIG_TYPE_DICT, props.configType || 'BOTH') || props.configType || translateUiText("导入导出"))
const editForm = ref({
  key: '',
  id: null,
  fieldName: '',
  columnName: '',
  columnNameEn: '',
  width: 20,
  orderNum: 0,
  export: true,
  importable: true,
  required: false,
  dateFormat: '',
  numberFormat: '',
  dictType: '',
  exampleValue: '',
  validationRule: '',
  validationMessage: '',
})
let editingIndex = -1

// 表格列定义
const columns = computed(() => {
  const columnList = [
    {
      title: '排序',
      key: 'orderNum',
      width: 80,
      align: 'center',
    },
    {
      title: '字段名',
      key: 'fieldName',
      width: 150,
    },
    {
      title: '列名',
      key: 'columnName',
      width: 150,
    },
    {
      title: '列宽',
      key: 'width',
      width: 80,
      align: 'center',
    },
  ]

  if (exportEnabled.value) {
    columnList.push({
      title: '是否导出',
      key: 'export',
      width: 100,
      align: 'center',
      render: row => h(DictTag, { options: yesNoOptions.value, value: boolToDictValue(row.export), size: 'small' }),
    })
  }

  if (importEnabled.value) {
    columnList.push(
      {
        title: '是否导入',
        key: 'importable',
        width: 100,
        align: 'center',
        render: row => h(DictTag, { options: yesNoOptions.value, value: boolToDictValue(row.importable), size: 'small' }),
      },
      {
        title: '导入必填',
        key: 'required',
        width: 100,
        align: 'center',
        render: row => h(DictTag, { options: yesNoOptions.value, value: boolToDictValue(row.required), size: 'small' }),
      },
    )
  }

  columnList.push(
    {
      title: '日期格式',
      key: 'dateFormat',
      width: 150,
    },
    {
      title: '数字格式',
      key: 'numberFormat',
      width: 120,
    },
    {
      title: '字典类型',
      key: 'dictType',
      width: 120,
    },
  )

  if (importEnabled.value) {
    columnList.push({
      title: '示例值',
      key: 'exampleValue',
      width: 140,
      ellipsis: {
        tooltip: true,
      },
    })
  }

  columnList.push({
    title: '操作',
    key: 'action',
    width: 180,
    align: 'center',
    fixed: 'right',
    render: (row, index) => {
      return h('div', { class: 'flex items-center justify-center gap-8' }, [
        h('a', {
          class: 'text-primary cursor-pointer hover:text-primary-hover',
          onClick: () => handleEdit(row, index),
        }, translateUiText("编辑")),
        h('span', { class: 'text-gray-300' }, '|'),
        h('a', {
          class: 'text-warning cursor-pointer hover:text-warning-hover',
          onClick: () => handleMoveUp(index),
        }, translateUiText("上移")),
        h('span', { class: 'text-gray-300' }, '|'),
        h('a', {
          class: 'text-warning cursor-pointer hover:text-warning-hover',
          onClick: () => handleMoveDown(index),
        }, translateUiText("下移")),
        h('span', { class: 'text-gray-300' }, '|'),
        h('a', {
          class: 'text-error cursor-pointer hover:text-error-hover',
          onClick: () => handleDelete(index),
        }, translateUiText("删除")),
      ])
    },
  })

  return columnList
})

// 加载数据
async function loadData() {
  try {
    const res = await request.get('/system/excel/column-config/list', {
      params: { configKey: props.configKey },
    })

    if (res.code === 200) {
      dataList.value = (res.data || []).map((item, index) => ({
        ...item,
        export: exportEnabled.value && item.export !== false,
        importable: importEnabled.value && item.importable !== false,
        required: importEnabled.value && item.required === true,
        key: `${item.id || index}_${Date.now()}`,
      }))
    }
  }
  catch {
    window.$message.error(translateUiText("加载列配置失败"))
  }
}

// 添加
function handleAdd() {
  const maxOrder = dataList.value.length > 0
    ? Math.max(...dataList.value.map(d => d.orderNum || 0))
    : 0

  editForm.value = {
    key: `new_${Date.now()}`,
    id: null,
    fieldName: '',
    columnName: '',
    columnNameEn: '',
    width: 20,
    orderNum: maxOrder + 1,
    export: exportEnabled.value,
    importable: importEnabled.value,
    required: false,
    dateFormat: '',
    numberFormat: '',
    dictType: '',
    exampleValue: '',
    validationRule: '',
    validationMessage: '',
  }
  editingIndex = -1
  showEditModal.value = true
}

// 编辑
function handleEdit(row, index) {
  editForm.value = normalizeColumnForMode({ ...row })
  editingIndex = index
  showEditModal.value = true
}

// 确认编辑
async function handleConfirmEdit() {
  try {
    await editFormRef.value?.validate()

    if (editingIndex >= 0) {
      // 更新
      dataList.value[editingIndex] = normalizeColumnForMode({ ...editForm.value })
    }
    else {
      // 新增
      dataList.value.push(normalizeColumnForMode({ ...editForm.value }))
    }

    // 重新排序
    dataList.value.forEach((item, index) => {
      item.orderNum = index + 1
    })

    showEditModal.value = false
    window.$message.success(translateUiText("操作成功"))
  }
  catch (error) {
    if (error?.message !== '验证失败') {
      window.$message.error(translateUiText("操作失败"))
    }
  }
}

// 删除
function handleDelete(index) {
  window.$dialog.warning({
    title: translateUiText("确认删除"),
    content: translateUiText("确定要删除这条列配置吗？"),
    positiveText: translateUiText("确定"),
    negativeText: translateUiText("取消"),
    onPositiveClick: () => {
      dataList.value.splice(index, 1)
      // 重新排序
      dataList.value.forEach((item, idx) => {
        item.orderNum = idx + 1
      })
      window.$message.success(translateUiText("删除成功"))
    },
  })
}

// 上移
function handleMoveUp(index) {
  if (index === 0) {
    window.$message.warning(translateUiText("已经是第一条了"))
    return
  }

  const temp = dataList.value[index]
  dataList.value[index] = dataList.value[index - 1]
  dataList.value[index - 1] = temp

  // 重新排序
  dataList.value.forEach((item, idx) => {
    item.orderNum = idx + 1
  })
}

// 下移
function handleMoveDown(index) {
  if (index === dataList.value.length - 1) {
    window.$message.warning(translateUiText("已经是最后一条了"))
    return
  }

  const temp = dataList.value[index]
  dataList.value[index] = dataList.value[index + 1]
  dataList.value[index + 1] = temp

  // 重新排序
  dataList.value.forEach((item, idx) => {
    item.orderNum = idx + 1
  })
}

// 保存配置
async function handleSave() {
  try {
    // 准备数据
    const columns = dataList.value.map(item => ({
      id: item.id,
      configKey: props.configKey,
      fieldName: item.fieldName,
      columnName: item.columnName,
      columnNameEn: item.columnNameEn,
      width: item.width,
      orderNum: item.orderNum,
      export: item.export,
      importable: item.importable,
      required: item.required,
      dateFormat: item.dateFormat || null,
      numberFormat: item.numberFormat || null,
      dictType: item.dictType || null,
      exampleValue: item.exampleValue || null,
      validationRule: item.validationRule || null,
      validationMessage: item.validationMessage || null,
    }))

    const res = await request.post('/system/excel/column-config/batch', columns, {
      params: { configKey: props.configKey },
    })

    if (res.code === 200) {
      window.$message.success(translateUiText("保存成功"))
      await loadData()
    }
    else {
      window.$message.error(res.respMsg || '保存失败')
    }
  }
  catch (error) {
    window.$message.error(formatUiText("保存失败：{0}", [error.message || translateUiText("未知错误")]))
  }
}

onMounted(() => {
  loadData()
})

watch(() => props.configKey, () => {
  loadData()
})

function boolToDictValue(value) {
  return value ? '1' : '0'
}

function normalizeColumnForMode(column) {
  return {
    ...column,
    export: exportEnabled.value ? column.export !== false : false,
    importable: importEnabled.value ? column.importable !== false : false,
    required: importEnabled.value ? column.required === true : false,
    exampleValue: importEnabled.value ? column.exampleValue : '',
    validationRule: importEnabled.value ? column.validationRule : '',
    validationMessage: importEnabled.value ? column.validationMessage : '',
  }
}
</script>

<style scoped>
.excel-column-config {
  padding: 16px;
}
</style>
