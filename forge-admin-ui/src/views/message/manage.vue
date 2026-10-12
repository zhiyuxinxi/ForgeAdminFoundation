<template>
  <div class="message-manage-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :before-search="handleBeforeSearch"
    >
      <!-- 自定义顶部工具栏 -->
      <template #toolbar-end>
        <n-button type="primary" @click="showSendModal = true">
          <template #icon>
            <i class="i-material-symbols:send" />
          </template>
          {{ translateUiText("发送测试消息") }}
        </n-button>
      </template>
    </AiCrudPage>

    <!-- 消息发送测试弹窗 -->
    <n-modal v-model:show="showSendModal" preset="card" :title="translateUiText(&quot;消息发送测试&quot;)" style="width: 600px">
      <n-form :model="sendForm" label-width="100">
        <n-form-item :label="translateUiText(&quot;发送渠道&quot;)" path="channel">
          <n-select
            v-model:value="sendForm.channel"
            :placeholder="translateUiText(&quot;选择发送渠道&quot;)"
            :options="channelOptions"
          />
        </n-form-item>
        <n-form-item v-if="sendForm.channel === 'COLLABORATION'" :label="translateUiText(&quot;协同连接&quot;)" path="connectionId">
          <n-select
            v-model:value="sendForm.connectionId"
            :placeholder="translateUiText(&quot;选择协同连接（决定发往企业微信/钉钉/飞书）&quot;)"
            :options="connectionOptions"
            clearable
            filterable
          />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;消息标题&quot;)" path="title">
          <n-input v-model:value="sendForm.title" :placeholder="translateUiText(&quot;请输入消息标题&quot;)" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;消息内容&quot;)" path="content">
          <n-input
            v-model:value="sendForm.content"
            type="textarea"
            :placeholder="translateUiText(&quot;请输入消息内容&quot;)"
            :rows="4"
          />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;模版编码&quot;)" path="templateCode">
          <n-input v-model:value="sendForm.templateCode" :placeholder="translateUiText(&quot;请输入模版编码&quot;)" />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;发送范围&quot;)" path="sendScope">
          <n-select
            v-model:value="sendForm.sendScope"
            :placeholder="translateUiText(&quot;选择发送范围&quot;)"
            :options="scopeOptions"
          />
        </n-form-item>
        <n-form-item v-if="sendForm.sendScope === 'USERS'" :label="translateUiText(&quot;指定用户&quot;)" path="userIds">
          <UserSelectPicker
            v-model:model-value="sendForm.userIds"
            v-model:label-value="sendForm.userLabels"
            multiple
            :placeholder="translateUiText(&quot;点击选择接收用户&quot;)"
            :title="translateUiText(&quot;选择接收用户&quot;)"
          />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;消息类型&quot;)" path="type">
          <n-select
            v-model:value="sendForm.type"
            :placeholder="translateUiText(&quot;选择消息类型&quot;)"
            :options="typeOptions"
          />
        </n-form-item>
        <n-form-item :label="translateUiText(&quot;业务类型&quot;)" path="bizType">
          <n-select
            v-model:value="sendForm.bizType"
            :placeholder="translateUiText(&quot;选择业务类型（可选）&quot;)"
            :options="bizTypeOptions"
            clearable
          />
        </n-form-item>
        <n-form-item v-if="sendForm.bizType" :label="translateUiText(&quot;业务主键&quot;)" path="bizKey">
          <n-input
            v-model:value="sendForm.bizKey"
            :placeholder="translateUiText(&quot;请输入业务主键（如：订单ID、流程实例ID）&quot;)"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showSendModal = false">
            {{ translateUiText("取消") }}
          </n-button>
          <n-button type="primary" :loading="sending" @click="handleSend">
            {{ translateUiText("发送") }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 消息详情弹窗 -->
    <n-modal v-model:show="showDetail" preset="card" :title="translateUiText(&quot;消息详情&quot;)" style="width: 800px">
      <n-descriptions label-placement="left" :column="2" bordered>
        <n-descriptions-item :label="translateUiText(&quot;消息标题&quot;)">
          {{ currentDetail?.message?.title }}
        </n-descriptions-item>
        <n-descriptions-item :label="translateUiText(&quot;消息类型&quot;)">
          <DictTag dict-type="sys_message_type" :value="currentDetail?.message?.type" />
        </n-descriptions-item>
        <n-descriptions-item :label="translateUiText(&quot;发送渠道&quot;)">
          <DictTag dict-type="sys_message_channel" :value="currentDetail?.message?.sendChannel" />
        </n-descriptions-item>
        <n-descriptions-item v-if="currentDetail?.message?.platform" :label="translateUiText(&quot;协同平台&quot;)">
          <DictTag dict-type="sys_collab_platform" :value="currentDetail?.message?.platform" />
        </n-descriptions-item>
        <n-descriptions-item :label="translateUiText(&quot;发送时间&quot;)">
          {{ currentDetail?.message?.createTime }}
        </n-descriptions-item>
        <n-descriptions-item :label="translateUiText(&quot;发送状态&quot;)">
          <DictTag dict-type="sys_message_send_status" :value="currentDetail?.sendRecord?.status" />
        </n-descriptions-item>
        <n-descriptions-item :label="translateUiText(&quot;接收人数&quot;)">
          {{ currentDetail?.sendRecord?.receiverCount }}
        </n-descriptions-item>
        <n-descriptions-item :label="translateUiText(&quot;成功数&quot;)">
          {{ currentDetail?.sendRecord?.successCount }}
        </n-descriptions-item>
        <n-descriptions-item :label="translateUiText(&quot;失败数&quot;)">
          {{ currentDetail?.sendRecord?.failCount }}
        </n-descriptions-item>
        <n-descriptions-item v-if="currentDetail?.sendRecord?.errorMsg" :label="translateUiText(&quot;错误信息&quot;)" :span="2">
          <n-text type="error">
            {{ currentDetail?.sendRecord?.errorMsg }}
          </n-text>
        </n-descriptions-item>
        <n-descriptions-item v-if="currentDetail?.message?.bizType" :label="translateUiText(&quot;业务类型&quot;)">
          {{ getBizTypeName(currentDetail?.message?.bizType) }}
        </n-descriptions-item>
        <n-descriptions-item v-if="currentDetail?.message?.bizKey" :label="translateUiText(&quot;业务主键&quot;)">
          {{ currentDetail?.message?.bizKey }}
        </n-descriptions-item>
      </n-descriptions>

      <n-divider>{{ translateUiText("消息内容") }}</n-divider>
      <div class="message-content" v-html="currentDetail?.message?.content" />

      <n-divider>{{ translateUiText("接收人列表") }}</n-divider>
      <n-data-table
        :columns="translateUiMetadata(receiverColumns)"
        :data="currentDetail?.receivers || []"
        :max-height="300"
      />
    </n-modal>
  </div>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

import { computed, h, ref } from 'vue'
import { fetchConnectionOptions } from '@/api/collaboration'
import messageApi from '@/api/message'
import { AiCrudPage } from '@/components/ai-form'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'MessageManage' })

const MESSAGE_TYPE_DICT = 'sys_message_type'
const MESSAGE_CHANNEL_DICT = 'sys_message_channel'
const COLLAB_PLATFORM_DICT = 'sys_collab_platform'
const MESSAGE_SEND_SCOPE_DICT = 'sys_message_send_scope'
const MESSAGE_SEND_STATUS_DICT = 'sys_message_send_status'
const MESSAGE_READ_STATUS_DICT = 'sys_message_read_status'

const crudRef = ref(null)
const showSendModal = ref(false)
const sending = ref(false)
const showDetail = ref(false)
const currentDetail = ref(null)
const bizTypeOptions = ref([])
const connectionOptions = ref([])

const { dict } = useDict(
  MESSAGE_TYPE_DICT,
  MESSAGE_CHANNEL_DICT,
  MESSAGE_SEND_SCOPE_DICT,
  MESSAGE_SEND_STATUS_DICT,
  MESSAGE_READ_STATUS_DICT,
  COLLAB_PLATFORM_DICT,
)

const typeOptions = computed(() => dict.value[MESSAGE_TYPE_DICT] || [])
const channelOptions = computed(() => dict.value[MESSAGE_CHANNEL_DICT] || [])
const scopeOptions = computed(() => dict.value[MESSAGE_SEND_SCOPE_DICT] || [])
const sendStatusOptions = computed(() => toNumberOptions(dict.value[MESSAGE_SEND_STATUS_DICT]))

const sendForm = ref({
  channel: 'WEB',
  title: '',
  content: '',
  templateCode: '',
  sendScope: 'USERS',
  userIds: [],
  userLabels: [],
  type: 'SYSTEM',
  bizType: null,
  bizKey: '',
  connectionId: null,
})

const apiConfig = {
  list: 'post@/api/message/manage/page',
  detail: 'get@/api/message/manage/{id}/detail',
}

const searchSchema = computed(() => [
  {
    field: 'type',
    label: '消息类型',
    type: 'select',
    props: {
      placeholder: '请选择消息类型',
      clearable: true,
      options: typeOptions.value,
    },
  },
  {
    field: 'channel',
    label: '发送渠道',
    type: 'select',
    props: {
      placeholder: '请选择发送渠道',
      clearable: true,
      options: channelOptions.value,
    },
  },
  {
    field: 'status',
    label: '发送状态',
    type: 'select',
    props: {
      placeholder: '请选择发送状态',
      clearable: true,
      options: sendStatusOptions.value,
    },
  },
  {
    field: 'keyword',
    label: '关键词',
    type: 'input',
    props: {
      placeholder: '搜索标题或内容',
    },
  },
  {
    field: 'timeRange',
    label: '发送时间',
    type: 'daterange',
    props: {
      type: 'datetimerange',
      clearable: true,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'title',
    label: '消息标题',
    ellipsis: { tooltip: true },
  },
  {
    prop: 'type',
    label: '消息类型',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: MESSAGE_TYPE_DICT, value: row.type, size: 'small' })
    },
  },
  {
    prop: 'channel',
    label: '发送渠道',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: MESSAGE_CHANNEL_DICT, value: row.channel, size: 'small' })
    },
  },
  {
    prop: 'platform',
    label: '协同平台',
    width: 110,
    render: (row) => {
      return row.platform
        ? h(DictTag, { dictType: COLLAB_PLATFORM_DICT, value: row.platform, size: 'small' })
        : '-'
    },
  },
  {
    prop: 'status',
    label: '发送状态',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: MESSAGE_SEND_STATUS_DICT, value: row.status, size: 'small' })
    },
  },
  {
    prop: 'receiverCount',
    label: '接收人数',
    width: 100,
  },
  {
    prop: 'readStatus',
    label: '已读/未读',
    width: 120,
    render: row => `${row.readCount}/${row.unreadCount}`,
  },
  {
    prop: 'createTime',
    label: '发送时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 80,
    fixed: 'right',
    actions: [
      { label: '详情', key: 'detail', onClick: handleViewDetail },
    ],
  },
])

const receiverColumns = [
  { title: '用户名', key: 'userName' },
  { title: '组织', key: 'orgName' },
  {
    title: '阅读状态',
    key: 'readFlag',
    width: 100,
    render: row => h(DictTag, { dictType: MESSAGE_READ_STATUS_DICT, value: row.readFlag, size: 'small' }),
  },
  { title: '阅读时间', key: 'readTime', width: 180 },
]

function handleBeforeSearch(params) {
  const result = { ...params }

  if (params.timeRange && params.timeRange.length === 2) {
    const [start, end] = params.timeRange
    result.startTime = new Date(start).toISOString().slice(0, 19).replace('T', ' ')
    result.endTime = new Date(end).toISOString().slice(0, 19).replace('T', ' ')
    delete result.timeRange
  }

  return result
}

async function handleSend() {
  if (!sendForm.value.title || !sendForm.value.content) {
    window.$message.warning(translateUiText("请填写消息标题和内容"))
    return
  }

  if (sendForm.value.sendScope === 'USERS' && sendForm.value.userIds.length === 0) {
    window.$message.warning(translateUiText("请选择接收用户"))
    return
  }

  if (sendForm.value.channel === 'COLLABORATION' && !sendForm.value.connectionId) {
    window.$message.warning(translateUiText("企业协同渠道需选择协同连接以指定发送平台"))
    return
  }

  try {
    sending.value = true
    const res = await messageApi.sendMessage(sendForm.value)
    if (res.code === 200) {
      window.$message.success(translateUiText("消息发送成功"))
      showSendModal.value = false
      handleResetSendForm()
      crudRef.value?.refresh()
    }
    else {
      window.$message.error(res.msg || '发送失败')
    }
  }
  catch (error) {
    console.error('发送消息失败:', error)
    window.$message.error(translateUiText("发送失败"))
  }
  finally {
    sending.value = false
  }
}

function handleResetSendForm() {
  sendForm.value = {
    channel: 'WEB',
    title: '',
    content: '',
    templateCode: '',
    sendScope: 'USERS',
    userIds: [],
    type: 'SYSTEM',
    bizType: null,
    bizKey: '',
    connectionId: null,
  }
}

async function loadConnections() {
  try {
    connectionOptions.value = await fetchConnectionOptions()
  }
  catch (error) {
    console.error('加载协同连接失败:', error)
  }
}

async function loadBizTypes() {
  try {
    const res = await messageApi.getBizTypeListEnabled()
    if (res.code === 200 && res.data) {
      bizTypeOptions.value = res.data.map(item => ({
        label: item.bizName,
        value: item.bizType,
      }))
    }
  }
  catch (error) {
    console.error('加载业务类型失败:', error)
  }
}

function getBizTypeName(bizType) {
  const item = bizTypeOptions.value.find(opt => opt.value === bizType)
  return item ? item.label : bizType
}

async function handleViewDetail(row) {
  try {
    const res = await messageApi.getMessageManageDetail(row.id)
    if (res.code === 200) {
      currentDetail.value = res.data
      showDetail.value = true
    }
  }
  catch (error) {
    console.error('获取消息详情失败:', error)
    window.$message.error(translateUiText("获取详情失败"))
  }
}

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

loadBizTypes()
loadConnections()
</script>

<style scoped>
.message-manage-page {
  height: 100%;
}

.message-content {
  line-height: 1.8;
  color: #333;
  padding: 16px;
}
</style>
