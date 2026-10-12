<template>
  <div class="sub-module-management">
    <NSpace vertical>
      <NSpace>
        <NButton type="primary" class="add-module-btn" @click="handleAdd">
          {{ translateUiText("新增模块") }}
        </NButton>
        <NButton
          type="error"
          :disabled="checkedRowKeys.length === 0"
          @click="handleBatchDelete"
        >
          {{ translateUiText("批量删除") }}  {{ checkedRowKeys.length ? `(${checkedRowKeys.length})` : '' }}
        </NButton>
      </NSpace>

      <n-data-table
        v-model:checked-row-keys="checkedRowKeys"
        size="small"
        :columns="translateUiMetadata(columns)"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        :row-key="(rowData) => rowData.id"
        @update:page="handlePageChange"
      />
    </NSpace>

    <!-- 编辑抽屉 -->
    <n-drawer v-model:show="showDrawer" :width="500" placement="right">
      <n-drawer-content :title="drawerTitle" closable>
        <n-form
          ref="formRef"
          :model="formValue"
          :rules="formRules"
          label-placement="left"
          label-width="100"
        >
          <n-form-item :label="translateUiText(&quot;模块名称&quot;)" path="name">
            <n-input v-model:value="formValue.name" :placeholder="translateUiText(&quot;请输入模块名称&quot;)" />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;模块ID&quot;)" path="id">
            <n-input v-model:value="formValue.id" :placeholder="translateUiText(&quot;请输入模块ID&quot;)" :disabled="status === 'edit'" />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;图标&quot;)" path="icon">
            <IconSelector v-model="formValue.icon" />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;排序&quot;)" path="sort">
            <NInputNumber v-model:value="formValue.sort" :min="0" />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;类型&quot;)" path="type">
            <n-select
              v-model:value="formValue.type"
              :options="translateUiMetadata(typeOptions)"
              :placeholder="translateUiText(&quot;请选择类型&quot;)"
            />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;描述&quot;)" path="remark">
            <n-input v-model:value="formValue.remark" type="textarea" :placeholder="translateUiText(&quot;请输入描述&quot;)" />
          </n-form-item>
        </n-form>

        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDrawer = false">
              {{ translateUiText("取消") }}
            </NButton>
            <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
              {{ translateUiText("保存") }}
            </NButton>
          </NSpace>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { formatUiText } from '@/locales'

import { localizeLabel as translateUiText } from '@/locales'

import { NButton, NInputNumber, NSpace } from 'naive-ui'
import { h, onMounted, ref, watch } from 'vue'
import IconSelector from '@/components/IconSelector.vue'
import { request } from '@/utils'

const props = defineProps({
  subAppId: {
    type: [String, Number],
    default: '',
  },
  currentItem: {
    type: Object,
    default: () => ({}),
  },
})

// 定义事件
const emit = defineEmits(['refresh'])

const message = window.$message

// 数据相关
const loading = ref(false)
const submitLoading = ref(false)
const showDrawer = ref(false)
const drawerTitle = ref('')
const tableData = ref([])
const formRef = ref(null)
const status = ref('')

// 表单数据
const formValue = ref({
  _id: '', // 使用 _id 字段判断是编辑还是新增
  id: '',
  name: '',
  icon: '',
  sort: 0,
  type: '1',
  remark: '',
})

// 类型选项
const typeOptions = [
  { label: 'PC', value: '1' },
  { label: 'APP', value: '2' },
  { label: '小程序', value: '3' },
]

const checkedRowKeysRef = ref([])
const checkedRowKeys = checkedRowKeysRef
// 表单验证规则
const formRules = {
  name: {
    required: true,
    message: '请输入模块名称',
    trigger: 'blur',
  },
  id: {
    required: true,
    message: '请输入模块ID',
    trigger: 'blur',
  },
}

// 分页配置
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: handlePageChange,
})

// 表格列配置
const columns = [
  {
    type: 'selection',
    width: 80,
  },
  {
    title: '模块名称',
    key: 'name',
    width: 150,
  },
  {
    title: '系统名称',
    key: 'subAppName',
    width: 150,
  },
  {
    title: '排序',
    key: 'showOrder',
    width: 110,
    align: 'center',
    render(row) {
      return h(NInputNumber, {
        value: row.showOrder,
        min: 0,
        onUpdateValue: value => handleOrderChange(row, value),
      })
    },
  },
  {
    title: '模块ID',
    key: 'id',
    width: 215,
  },
  {
    title: 'icon',
    key: 'icon',
    width: 150,
    align: 'center',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; justify-content: center;' }, [
        h(IconSelector, {
          'modelValue': row.icon,
          'onUpdate:modelValue': value => handleIconChange(row, value),
        }),
      ])
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    align: 'center',
    fixed: 'right',
    render(row) {
      return h(NSpace, { justify: 'center' }, {
        default: () => [
          h(NButton, {
            type: 'info',
            size: 'small',
            quaternary: true,
            onClick: () => handleEdit(row),
          }, { default: () => translateUiText("编辑") }),
          h(NButton, {
            type: 'error',
            size: 'small',
            quaternary: true,
            onClick: () => handleDelete(row),
          }, { default: () => translateUiText("删除") }),
        ],
      })
    },
  },
]

// 批量删除
function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) {
    message.warning(translateUiText("请先选择要删除的数据"))
    return
  }

  window.$dialog?.warning({
    title: translateUiText("批量删除"),
    content: formatUiText("确定要删除选中的 {0} 条模块吗?", [checkedRowKeys.value.length]),
    positiveText: translateUiText("确定"),
    negativeText: translateUiText("取消"),
    onPositiveClick: async () => {
      try {
        await request.delete(`/module`, {
          data: checkedRowKeys.value,
        })
        message.success(translateUiText("批量删除成功"))
        checkedRowKeys.value = []
        loadData()
        emit('refresh')
      }
      catch {
      }
    },
  })
}
// 处理图标变化
let iconChangeTimer = null
function handleIconChange(row, value) {
  row.icon = value
  if (iconChangeTimer) {
    clearTimeout(iconChangeTimer)
  }
  iconChangeTimer = setTimeout(async () => {
    try {
      await request.put('/module', row)
      loadData()
    }
    catch {
    }
  }, 300)
}

// 处理排序变化
let orderChangeTimer = null
function handleOrderChange(row, value) {
  row.showOrder = value
  if (orderChangeTimer) {
    clearTimeout(orderChangeTimer)
  }
  orderChangeTimer = setTimeout(async () => {
    try {
      await request.put('/module', row)
      loadData()
    }
    catch {
    }
  }, 300)
}

// 获取数据
async function loadData() {
  // 确保必要的参数都存在
  if (!props.subAppId) {
    return
  }

  try {
    loading.value = true
    // 调用实际的API接口，保持与旧版本一致
    const res = await request.get('/module', {
      params: {
        subAppId: props.subAppId,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize,
      },
    })
    // 兼容新旧数据结构
    const list = res.data?.list || res.data || []
    const total = res.data?.total || res.total || 0

    tableData.value = list
    // 确保 total是数字类型
    pagination.value.itemCount = Number(total)
  }
  catch {
  }
  finally {
    loading.value = false
  }
}

// 处理分页变化
function handlePageChange(page) {
  pagination.value.page = page
  loadData()
}

// 新增
function handleAdd() {
  drawerTitle.value = '新增模块'
  status.value = 'add'
  formValue.value = {
    _id: '', // 使用 _id 字段
    id: '',
    name: '',
    icon: '',
    sort: 0,
    type: '1',
    remark: '',
  }
  showDrawer.value = true
}

// 编辑
function handleEdit(row) {
  drawerTitle.value = '编辑模块'
  status.value = 'edit'
  formValue.value = { ...row, type: row.type.val }
  showDrawer.value = true
}

// 删除
function handleDelete(row) {
  window.$dialog?.warning({
    title: translateUiText("提示"),
    content: formatUiText("确定要删除模块 \"{0}\" 吗？", [row.name]),
    positiveText: translateUiText("确定"),
    negativeText: translateUiText("取消"),
    onPositiveClick: async () => {
      try {
        // 调用实际的API接口，使用 _id 或 id 字段
        await request.delete(`/module`, {
          data: [row.id],
        })
        loadData()
        // 通知父组件刷新
        emit('refresh')
      }
      catch {
      }
    },
  })
}

// 提交表单
function handleSubmit() {
  formRef.value?.validate(async (errors) => {
    if (errors) {
      message.error(translateUiText("请检查表单填写是否正确"))
      return
    }

    try {
      submitLoading.value = true
      // 添加子应用ID
      const formData = {
        ...formValue.value,
        subAppId: props.subAppId,
      }
      // 根据是否有 _id 判断是编辑还是新增
      if (status.value === 'edit') {
        // 编辑 - 使用 PUT
        await request.put('/module', formData)
        message.success(translateUiText("更新成功"))
      }
      else {
        // 新增 - 使用 POST
        await request.post('/module', formData)
        message.success(translateUiText("新增成功"))
      }
      showDrawer.value = false
      loadData()
      // 通知父组件刷新树
      emit('refresh')
    }
    catch {
    }
    finally {
      submitLoading.value = false
    }
  })
}

// 刷新数据
function refresh() {
  loadData()
}

// 暴露方法给父组件
defineExpose({
  refresh,
})

// 监听子应用ID变化
watch(() => props.subAppId, () => {
  if (props.subAppId) {
    loadData()
  }
}, { immediate: true })

// 初始化
onMounted(() => {
  if (props.subAppId) {
    loadData()
  }
})
</script>

<style scoped>
.sub-module-management {
  padding: 16px 0;
}
</style>
