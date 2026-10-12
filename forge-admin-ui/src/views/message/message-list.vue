<template>
  <div class="message-list-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-batch-delete="true"
      @selection-change="handleSelectionChange"
    >
      <!-- 自定义顶部工具栏 -->
      <template #toolbar-end>
        <n-button
          v-if="selectedRowKeys.length > 0"
          @click="handleBatchMarkRead"
        >
          <template #icon>
            <i class="i-material-symbols:mark-email-read-outline" />
          </template>
          {{ localizeLabel('批量标记已读') }}
        </n-button>
        <n-button @click="handleMarkAllRead">
          <template #icon>
            <i class="i-material-symbols:done-all" />
          </template>
          {{ localizeLabel('全部已读') }}
        </n-button>
      </template>
    </AiCrudPage>

    <!-- 消息详情抽屉 -->
    <Teleport to="body">
      <n-drawer
        v-model:show="showDetail"
        :width="600"
        placement="right"
      >
        <n-drawer-content :title="currentMessage?.title" closable>
          <div class="message-detail">
            <div class="detail-meta">
              <n-space>
                <DictTag dict-type="sys_message_type" :value="currentMessage?.type" force-tag />
                <DictTag dict-type="sys_message_read_status" :value="currentMessage?.readFlag" force-tag />
              </n-space>
              <div class="text-12 mt-8 text-gray-500">
                {{ currentMessage?.createTime }}
              </div>
            </div>
            <n-divider />
            <div class="message-content" v-html="currentMessage?.content" />
            <n-divider v-if="currentMessage?.bizType && currentMessage?.bizKey" />
            <div v-if="currentMessage?.bizType && currentMessage?.bizKey" class="biz-info">
              <n-space align="center" justify="space-between">
                <div>
                  <span class="text-gray-500">{{ localizeLabel('关联业务：') }}</span>
                  <span>{{ getBizTypeName(currentMessage?.bizType) }} ({{ currentMessage?.bizKey }})</span>
                </div>
                <n-button type="primary" size="small" @click="handleJumpToBiz(currentMessage)">
                  {{ localizeLabel('查看详情') }}
                </n-button>
              </n-space>
            </div>
          </div>
        </n-drawer-content>
      </n-drawer>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'

import { useRouter } from 'vue-router'
import messageApi from '@/api/message'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { mergeMessageNavigationTarget } from '@/layouts/components/message-notification-utils'
import { localizeLabel } from '@/locales'
import { request } from '@/utils'
import { toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'MessageList' })

const props = defineProps({
  todoRoute: {
    type: [String, Object],
    default: '',
  },
})

const router = useRouter()
const crudRef = ref(null)
const showDetail = ref(false)
const currentMessage = ref(null)
const selectedRowKeys = ref([])
const bizTypeOptions = ref([])
const { dict } = useDict('sys_message_type', 'sys_message_read_status')
const messageTypeOptions = computed(() => dict.value.sys_message_type || [])
const readStatusOptions = computed(() => toNumberDictOptions(dict.value.sys_message_read_status))

const apiConfig = {
  list: 'get@/api/message/page',
}

const searchSchema = computed(() => [
  {
    field: 'type',
    get label() { return localizeLabel('消息类型') },
    type: 'select',
    props: {
      get placeholder() { return localizeLabel('请选择消息类型') },
      clearable: true,
      options: messageTypeOptions.value,
    },
  },
  {
    field: 'readFlag',
    get label() { return localizeLabel('阅读状态') },
    type: 'select',
    props: {
      get placeholder() { return localizeLabel('请选择阅读状态') },
      clearable: true,
      options: readStatusOptions.value,
    },
  },
  {
    field: 'keyword',
    get label() { return localizeLabel('关键词') },
    type: 'input',
    props: {
      get placeholder() { return localizeLabel('搜索标题或内容') },
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'title',
    get label() { return localizeLabel('消息标题') },
    ellipsis: { tooltip: true },
    render: (row) => {
      return h('div', { class: 'flex items-center' }, [
        row.readFlag === 0
          ? h('span', {
              class: 'w-6 h-6 bg-red-500 rounded-full mr-8',
            })
          : null,
        h('span', {
          class: row.readFlag === 0 ? 'font-bold' : '',
          style: { cursor: 'pointer', color: '#18a058' },
          onClick: () => handleViewDetail(row),
        }, row.title),
      ])
    },
  },
  {
    prop: 'type',
    get label() { return localizeLabel('消息类型') },
    width: 100,
    render: row => h(DictTag, { options: messageTypeOptions.value, value: row.type, forceTag: true }),
  },
  {
    prop: 'readFlag',
    get label() { return localizeLabel('状态') },
    width: 80,
    render: row => h(DictTag, { options: readStatusOptions.value, value: row.readFlag, forceTag: true }),
  },
  {
    prop: 'createTime',
    get label() { return localizeLabel('接收时间') },
    width: 180,
  },
  {
    prop: 'action',
    get label() { return localizeLabel('操作') },
    width: 150,
    fixed: 'right',
    actions: [
      { get label() { return localizeLabel('查看') }, key: 'view', onClick: handleViewDetail },
      { get label() { return localizeLabel('查看详情') }, key: 'viewBiz', onClick: handleJumpToBiz, visible: row => !!(row.bizType && row.bizKey) },
      { get label() { return localizeLabel('标记已读') }, key: 'markRead', type: 'warning', onClick: row => handleMarkRead(row.id), visible: row => row.readFlag === 0 },
    ],
  },
])

function handleSelectionChange(keys) {
  selectedRowKeys.value = keys
}

async function handleViewDetail(row) {
  try {
    const res = await request.get(`/api/message/${row.id}`)
    if (res.code === 200) {
      currentMessage.value = res.data
      showDetail.value = true
      if (row.readFlag === 0) {
        await request.post(`/api/message/${row.id}/read`)
        crudRef.value?.refresh()
      }
    }
  }
  catch (error) {
    console.error('获取消息详情失败:', error)
    window.$message.error(localizeLabel('获取详情失败'))
  }
}

async function handleMarkRead(id) {
  try {
    await request.post(`/api/message/${id}/read`)
    window.$message.success(localizeLabel('已标记为已读'))
    crudRef.value?.refresh()
  }
  catch {
    window.$message.error(localizeLabel('操作失败'))
  }
}

async function handleBatchMarkRead() {
  if (selectedRowKeys.value.length === 0) {
    window.$message.warning(localizeLabel('请选择要操作的消息'))
    return
  }
  try {
    await request.post('/api/message/read/batch', selectedRowKeys.value)
    window.$message.success(localizeLabel('已批量标记为已读'))
    selectedRowKeys.value = []
    crudRef.value?.refresh()
  }
  catch {
    window.$message.error(localizeLabel('操作失败'))
  }
}

async function handleMarkAllRead() {
  window.$dialog.warning({
    get title() { return localizeLabel('确认') },
    get content() { return localizeLabel('确定要将所有未读消息标记为已读吗？') },
    get positiveText() { return localizeLabel('确定') },
    get negativeText() { return localizeLabel('取消') },
    onPositiveClick: async () => {
      try {
        await request.post('/api/message/read/all')
        window.$message.success(localizeLabel('已全部标记为已读'))
        crudRef.value?.refresh()
      }
      catch {
        window.$message.error(localizeLabel('操作失败'))
      }
    },
  })
}

async function loadBizTypes() {
  try {
    const res = await messageApi.getBizTypeListEnabled()
    if (res.code === 200 && res.data) {
      bizTypeOptions.value = res.data
    }
  }
  catch (error) {
    console.error('加载业务类型失败:', error)
  }
}

function getBizTypeName(bizType) {
  const item = bizTypeOptions.value.find(opt => opt.bizType === bizType)
  return item ? item.bizName : bizType
}

function isFlowTodoMessage(message) {
  return message?.bizType === 'FLOW_TODO'
}

function handleJumpToBiz(message) {
  if (isFlowTodoMessage(message)) {
    showDetail.value = false
    router.push(mergeMessageNavigationTarget(props.todoRoute || '/flow/todo', {
      taskId: message.bizKey,
      source: 'message',
      t: Date.now(),
    }))
    return
  }

  const bizConfig = bizTypeOptions.value.find(opt => opt.bizType === message.bizType)
  if (bizConfig && bizConfig.jumpUrl) {
    let jumpUrl = bizConfig.jumpUrl
    jumpUrl = jumpUrl
      .replace(/\$\{bizKey\}/g, message.bizKey)
      .replace(/\$\{messageId\}/g, message.id)

    if (bizConfig.jumpTarget === '_blank') {
      window.open(jumpUrl, '_blank')
    }
    else {
      router.push(jumpUrl)
    }
  }
}

loadBizTypes()
</script>

<style scoped>
.message-list-page {
  height: 100%;
}

.message-detail {
  padding: 16px 0;
}

.biz-info {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.detail-meta {
  margin-bottom: 16px;
}

.message-content {
  line-height: 1.8;
  color: #333;
}

.message-content :deep(p) {
  margin: 8px 0;
}
</style>
