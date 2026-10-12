<template>
  <n-modal
    v-model:show="visible"
    :title="translateUiText(&quot;AI 建表&quot;)"
    preset="card"
    style="width: 900px"
    :mask-closable="false"
  >
    <n-spin :show="loading">
      <div v-if="!schemaResult" style="padding: 20px 0;">
        <NInput
          v-model:value="description"
          type="textarea"
          :placeholder="translateUiText(&quot;请用自然语言描述你需要的数据表，例如：电商订单管理，包含订单号、金额、状态、收货地址&quot;)"
          :rows="4"
          :maxlength="2000"
          show-count
        />
        <div style="margin-top: 12px; text-align: right;">
          <n-button type="primary" :loading="loading" @click="handleGenerate">
            {{ translateUiText("生成 Schema") }}
          </n-button>
        </div>
      </div>

      <div v-else>
        <n-descriptions bordered :column="2" size="small" style="margin-bottom: 16px;">
          <n-descriptions-item :label="translateUiText(&quot;表名&quot;)">
            <NInput v-model:value="schemaResult.tableName" size="small" />
          </n-descriptions-item>
          <n-descriptions-item :label="translateUiText(&quot;表注释&quot;)">
            <NInput v-model:value="schemaResult.tableComment" size="small" />
          </n-descriptions-item>
        </n-descriptions>

        <n-data-table
          :columns="translateUiMetadata(schemaColumns)"
          :data="schemaResult.columns"
          max-height="350px"
          :scroll-x="1200"
          size="small"
        />

        <div style="margin-top: 12px; display: flex; justify-content: space-between;">
          <n-space>
            <n-button :loading="refineLoading" @click="handleRefine">
              {{ translateUiText("追问优化") }}
            </n-button>
            <n-button @click="handleRegenerate">
              {{ translateUiText("重新生成") }}
            </n-button>
          </n-space>
          <n-button type="primary" @click="handleConfirmImport">
            {{ translateUiText("确认导入") }}
          </n-button>
        </div>

        <n-modal
          v-model:show="showRefineModal"
          :title="translateUiText(&quot;追问优化&quot;)"
          preset="card"
          style="width: 500px"
        >
          <NInput
            v-model:value="refineMessage"
            type="textarea"
            :placeholder="translateUiText(&quot;请输入追问内容，例如：金额需要支持多币种&quot;)"
            :rows="3"
          />
          <template #footer>
            <n-space justify="end">
              <n-button @click="showRefineModal = false">
                {{ translateUiText("取消") }}
              </n-button>
              <n-button type="primary" :loading="refineLoading" @click="handleRefineSubmit">
                {{ translateUiText("提交") }}
              </n-button>
            </n-space>
          </template>
        </n-modal>
      </div>
    </n-spin>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">
          {{ translateUiText("关闭") }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

import { NInput, NSelect } from 'naive-ui'
import { h, ref, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'success'])

const visible = ref(props.show)
const loading = ref(false)
const refineLoading = ref(false)
const description = ref('')
const schemaResult = ref(null)
const showRefineModal = ref(false)
const refineMessage = ref('')

const htmlTypeOptions = [
  { label: '文本框', value: 'INPUT' },
  { label: '文本域', value: 'TEXTAREA' },
  { label: '下拉框', value: 'SELECT' },
  { label: '单选框', value: 'RADIO' },
  { label: '复选框', value: 'CHECKBOX' },
  { label: '日期控件', value: 'DATETIME' },
  { label: '图片上传', value: 'IMAGE' },
  { label: '文件上传', value: 'FILE' },
  { label: '富文本', value: 'EDITOR' },
]

const schemaColumns = [
  { title: '字段名', key: 'columnName', width: 130 },
  {
    title: '注释',
    key: 'columnComment',
    width: 130,
    render: row => h(NInput, {
      value: row.columnComment,
      size: 'small',
      onUpdateValue: (val) => { row.columnComment = val },
    }),
  },
  { title: '数据库类型', key: 'columnType', width: 110 },
  { title: 'Java类型', key: 'javaType', width: 100 },
  { title: 'Java字段', key: 'javaField', width: 110 },
  {
    title: '表单组件',
    key: 'htmlType',
    width: 110,
    render: row => h(NSelect, {
      value: row.htmlType,
      options: translateUiMetadata(htmlTypeOptions),
      size: 'small',
      onUpdateValue: (val) => { row.htmlType = val },
    }),
  },
  { title: '字典', key: 'dictType', width: 100 },
  { title: '必填', key: 'isRequired', width: 60, align: 'center', render: row => row.isRequired === 1 ? '是' : '否' },
]

watch(() => props.show, (val) => {
  visible.value = val
  if (val) {
    schemaResult.value = null
    description.value = ''
  }
}, { immediate: true })

watch(visible, (val) => {
  emit('update:show', val)
})

async function handleGenerate() {
  if (!description.value.trim()) {
    window.$message.warning(translateUiText("请输入描述"))
    return
  }
  try {
    loading.value = true
    const res = await request.post('/generator/ai/nlToSchema', {
      description: description.value,
    })
    if (res.code === 200 && res.data?.tableName) {
      schemaResult.value = res.data
    }
    else {
      window.$message.warning(res.data?.rawResponse || 'AI 不可用，请通过导入表方式创建')
    }
  }
  catch (error) {
    console.error('Schema生成失败:', error)
    window.$message.warning(translateUiText("AI 不可用，请通过导入表方式创建"))
  }
  finally {
    loading.value = false
  }
}

async function handleRefine() {
  refineMessage.value = ''
  showRefineModal.value = true
}

async function handleRefineSubmit() {
  if (!refineMessage.value.trim()) {
    window.$message.warning(translateUiText("请输入追问内容"))
    return
  }
  try {
    refineLoading.value = true
    const res = await request.post('/generator/ai/nlToSchemaRefine', {
      currentSchema: JSON.stringify(schemaResult.value),
      message: refineMessage.value,
    })
    if (res.code === 200 && res.data?.tableName) {
      schemaResult.value = res.data
      showRefineModal.value = false
      window.$message.success(translateUiText("Schema 已更新"))
    }
    else {
      window.$message.warning(translateUiText("追问优化失败，请重试"))
    }
  }
  catch (error) {
    console.error('追问优化失败:', error)
    window.$message.warning(translateUiText("追问优化失败"))
  }
  finally {
    refineLoading.value = false
  }
}

function handleRegenerate() {
  schemaResult.value = null
}

async function handleConfirmImport() {
  if (!schemaResult.value?.tableName) {
    window.$message.warning(translateUiText("Schema 无效"))
    return
  }
  try {
    loading.value = true
    const res = await request.post('/generator/ai/importSchema', schemaResult.value)
    if (res.code === 200) {
      window.$message.success(translateUiText("导入成功"))
      emit('success')
      handleClose()
    }
  }
  catch (error) {
    console.error('导入失败:', error)
    window.$message.error(translateUiText("导入失败"))
  }
  finally {
    loading.value = false
  }
}

function handleClose() {
  visible.value = false
}
</script>
