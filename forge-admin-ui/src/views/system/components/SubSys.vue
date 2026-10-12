<template>
  <div class="sub-sys-management">
    <NSpace vertical>
      <NSpace>
        <NButton type="primary" class="add-subsys-btn" @click="handleAdd">
          {{ translateUiText("新增子系统") }}
        </NButton>
      </NSpace>

      <n-data-table
        :columns="translateUiMetadata(columns)"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :remote="true"
        size="small"
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
          <n-form-item :label="translateUiText(&quot;系统名称&quot;)" path="name">
            <n-input v-model:value="formValue.name" :placeholder="translateUiText(&quot;请输入系统名称&quot;)" />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;系统ID&quot;)" path="id">
            <n-input v-model:value="formValue.id" :placeholder="translateUiText(&quot;请输入系统ID&quot;)" />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;别名&quot;)" path="alias">
            <n-input v-model:value="formValue.alias" :placeholder="translateUiText(&quot;请输入别名&quot;)" />
          </n-form-item>

          <n-form-item label="URL" path="url">
            <n-input v-model:value="formValue.url" :placeholder="translateUiText(&quot;请输入URL&quot;)" />
          </n-form-item>

          <n-form-item :label="translateUiText(&quot;图标&quot;)" path="icon">
            <IconSelector v-model="formValue.icon" />
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

import { NButton, NSpace } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import IconSelector from '@/components/IconSelector.vue'
import { request } from '@/utils'

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
  alias: '',
  url: '',
  icon: '',
})

// 表单验证规则
const formRules = {
  name: {
    required: true,
    message: '请输入系统名称',
    trigger: 'blur',
  },
  id: {
    required: true,
    message: '请输入系统ID',
    trigger: 'blur',
  },
  alias: {
    required: true,
    message: '请输入别名',
    trigger: 'blur',
  },
  url: {
    required: true,
    message: '请输入URL',
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
    title: '系统名称',
    key: 'name',
    width: 200,
  },
  {
    title: '系统ID',
    key: 'id',
    width: 150,
  },
  {
    title: '别名',
    key: 'alias',
    width: 150,
  },
  {
    title: 'URL',
    key: 'url',
    width: 200,
  },
  {
    title: '图标',
    key: 'icon',
    width: 150,
    align: 'center',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; justify-content: center;' }, [
        row.icon
          ? h(IconRenderer, {
              icon: row.icon,
              customStyle: 'font-size: 18px; margin-right: 5px;',
            })
          : null,
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

// 处理图标变化
let iconChangeTimer = null
function handleIconChange(row, value) {
  row.icon = value
  if (iconChangeTimer) {
    clearTimeout(iconChangeTimer)
  }
  iconChangeTimer = setTimeout(async () => {
    try {
      await request.put('/subApp', row)
      message.success(translateUiText("图标更新成功"))
      loadData()
      // 通知父组件刷新树
      emit('refresh')
    }
    catch (error) {
      message.error(formatUiText("图标更新失败: {0}", [error.message || error]))
    }
  }, 300)
}

// 获取数据
async function loadData() {
  try {
    loading.value = true
    // 调用实际的API接口，保持与旧版本一致
    const res = await request.get('/subApp', {
      params: {
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
  catch (error) {
    message.error(formatUiText("获取数据失败: {0}", [error.message || error]))
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
  drawerTitle.value = '新增子系统'
  status.value = 'add'
  formValue.value = {
    _id: '', // 使用 _id 字段
    id: '',
    name: '',
    alias: '',
    url: '',
    icon: '',
  }
  showDrawer.value = true
}

// 编辑
function handleEdit(row) {
  drawerTitle.value = '编辑子系统'
  status.value = 'edit'
  formValue.value = { ...row }
  showDrawer.value = true
}

// 删除
function handleDelete(row) {
  window.$dialog?.warning({
    title: translateUiText("提示"),
    content: formatUiText("确定要删除子系统 \"{0}\" 吗？", [row.name]),
    positiveText: translateUiText("确定"),
    negativeText: translateUiText("取消"),
    onPositiveClick: async () => {
      try {
        // 调用实际的API接口，使用 _id 或 id 字段
        await request.delete(`/subApp`, {
          data: [row.id],
        })
        message.success(translateUiText("删除成功"))
        loadData()
        // 通知父组件刷新树
        emit('refresh')
      }
      catch (error) {
        message.error(formatUiText("删除失败: {0}", [error.message]))
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
      // 根据是否有 _id 判断是编辑还是新增
      if (status.value === 'edit') {
        // 编辑 - 使用 PUT
        await request.put('/subApp', formValue.value)
        message.success(translateUiText("更新成功"))
      }
      else {
        // 新增 - 使用 POST
        await request.post('/subApp', formValue.value)
        message.success(translateUiText("新增成功"))
      }
      showDrawer.value = false
      loadData()
      // 通知父组件刷新树
      emit('refresh')
    }
    catch (error) {
      message.error(formatUiText("保存失败: {0}", [error.message]))
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

// 初始化
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.sub-sys-management {
  padding: 16px 0;
}
</style>
