<template>
  <div class="system-notice-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/notice"
      :api-config="{
        list: 'get@/system/notice/page',
        detail: 'post@/system/notice/getById',
        add: 'post@/system/notice/add',
        update: 'post@/system/notice/edit',
        delete: 'post@/system/notice/remove',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="noticeId"
      :add-button-text="translateUiText(&quot;新增公告&quot;)"
      :load-detail-on-edit="true"
      :edit-grid-cols="1"
      modal-width="1000px"
    >
      <!-- 富文本编辑器插槽 -->
      <template #form-noticeContent="{ value, updateValue }">
        <n-input
          :value="value"
          type="textarea"
          :placeholder="localizeLabel('请输入公告内容')"
          :rows="10"
          @update:value="updateValue"
        />
      </template>

      <!-- 发布范围插槽 -->
      <template #form-publishScope="{ value, updateValue }">
        <n-radio-group :value="value" @update:value="updateValue">
          <n-radio :value="0">
            {{ localizeLabel('全部组织') }}
          </n-radio>
          <n-radio :value="1">
            {{ localizeLabel('指定组织') }}
          </n-radio>
        </n-radio-group>
      </template>

      <!-- 组织选择器插槽 -->
      <template #form-orgIds="{ value, updateValue }">
        <n-tree-select
          :value="value"
          :options="orgTreeOptions"
          multiple
          cascade
          checkable
          :placeholder="localizeLabel('请选择组织')"
          clearable
          check-strategy="child"
          @update:value="updateValue"
        />
      </template>
    </AiCrudPage>

    <!-- 已读/未读统计弹窗 -->
    <n-modal
      v-model:show="showStatisticsModal"
      :title="localizeLabel('已读/未读统计')"
      preset="card"
      style="width: 900px"
      :segmented="{ content: 'soft' }"
    >
      <div v-if="currentStatistics" class="statistics-content">
        <n-space vertical size="large">
          <!-- 统计数据 -->
          <n-card :title="localizeLabel('阅读情况')" size="small">
            <n-space>
              <NStatistic :label="localizeLabel('应读人数')" :value="currentStatistics.totalUserCount" />
              <NStatistic :label="localizeLabel('已读人数')" :value="currentStatistics.readCount">
                <template #suffix>
                  <NTag type="success" size="small">
                    {{ currentStatistics.readRate }}%
                  </NTag>
                </template>
              </NStatistic>
              <NStatistic :label="localizeLabel('未读人数')" :value="currentStatistics.unreadCount">
                <template #suffix>
                  <NTag type="warning" size="small">
                    {{ (100 - currentStatistics.readRate).toFixed(2) }}%
                  </NTag>
                </template>
              </NStatistic>
            </n-space>
          </n-card>

          <!-- 已读用户列表 -->
          <n-card :title="localizeLabel('已读用户列表')" size="small">
            <n-data-table
              :columns="translateUiMetadata(readUserColumns)"
              :data="readUserList"
              :pagination="{ pageSize: 5 }"
              :max-height="300"
              size="small"
            />
          </n-card>

          <!-- 未读用户列表 -->
          <n-card :title="localizeLabel('未读用户列表')" size="small">
            <n-data-table
              :columns="translateUiMetadata(unreadUserColumns)"
              :data="unreadUserList"
              :pagination="{ pageSize: 5 }"
              :max-height="300"
              size="small"
            />
          </n-card>
        </n-space>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'

import { localizeLabel as translateUiText } from '@/locales'

import { NStatistic, NTag } from 'naive-ui'

import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { localizeLabel, t } from '@/locales'
import { request } from '@/utils'

defineOptions({ name: 'SystemNotice' })

const NOTICE_TYPE_DICT = 'sys_notice_type'
const NOTICE_STATUS_DICT = 'sys_notice_status'
const YES_NO_DICT = 'sys_yes_no'

const crudRef = ref(null)
const showStatisticsModal = ref(false)
const currentStatistics = ref(null)
const readUserList = ref([])
const unreadUserList = ref([])
const orgTreeOptions = ref([])

const { dict } = useDict(NOTICE_TYPE_DICT, NOTICE_STATUS_DICT, YES_NO_DICT)

const noticeTypeOptions = computed(() => dict.value[NOTICE_TYPE_DICT] || [])
const publishStatusOptions = computed(() => toNumberOptions(dict.value[NOTICE_STATUS_DICT]))
const isTopOptions = computed(() => toNumberOptions(dict.value[YES_NO_DICT]))

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'noticeTitle',
    get label() { return localizeLabel('公告标题') },
    type: 'input',
    props: {
      get placeholder() { return localizeLabel('请输入公告标题') },
    },
  },
  {
    field: 'noticeType',
    get label() { return localizeLabel('公告类型') },
    type: 'select',
    props: {
      get placeholder() { return localizeLabel('请选择公告类型') },
      options: noticeTypeOptions.value,
    },
  },
  {
    field: 'publishStatus',
    get label() { return localizeLabel('发布状态') },
    type: 'select',
    props: {
      get placeholder() { return localizeLabel('请选择发布状态') },
      options: publishStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'noticeId',
    get label() { return localizeLabel('公告ID') },
    width: 100,
  },
  {
    prop: 'noticeTitle',
    get label() { return localizeLabel('公告标题') },
    width: 250,
    render: (row) => {
      return h('div', { class: 'flex items-center' }, [
        row.isTop === 1 ? h(NTag, { type: 'error', size: 'small', style: { marginRight: '8px' } }, { default: () => localizeLabel('置顶') }) : null,
        h('span', row.noticeTitle),
      ])
    },
  },
  {
    prop: 'noticeType',
    get label() { return localizeLabel('公告类型') },
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: NOTICE_TYPE_DICT, value: row.noticeType, size: 'small' })
    },
  },
  {
    prop: 'publishStatus',
    get label() { return localizeLabel('发布状态') },
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: NOTICE_STATUS_DICT, value: row.publishStatus, size: 'small' })
    },
  },
  {
    prop: 'publisherName',
    get label() { return localizeLabel('发布人') },
    width: 120,
  },
  {
    prop: 'publishTime',
    get label() { return localizeLabel('发布时间') },
    width: 180,
  },
  {
    prop: 'readCount',
    get label() { return localizeLabel('阅读次数') },
    width: 100,
  },
  {
    prop: 'effectiveTime',
    get label() { return localizeLabel('生效时间') },
    width: 180,
  },
  {
    prop: 'expirationTime',
    get label() { return localizeLabel('失效时间') },
    width: 180,
  },
  {
    prop: 'createTime',
    get label() { return localizeLabel('创建时间') },
    width: 180,
  },
  {
    prop: 'action',
    get label() { return localizeLabel('操作') },
    width: 150,
    fixed: 'right',
    actions: [
      { get label() { return localizeLabel('统计') }, key: 'statistics', type: 'info', onClick: handleViewStatistics, visible: row => row.publishStatus === 1 },
      { get label() { return localizeLabel('发布') }, key: 'publish', type: 'success', onClick: handlePublish, visible: row => row.publishStatus === 0 },
      { get label() { return localizeLabel('撤回') }, key: 'revoke', type: 'warning', onClick: handleRevoke, visible: row => row.publishStatus === 1 },
      { get label() { return localizeLabel('置顶') }, key: 'top', onClick: handleTop, visible: row => row.isTop !== 1 },
      { get label() { return localizeLabel('取消置顶') }, key: 'untop', type: 'warning', onClick: handleTop, visible: row => row.isTop === 1 },
      { get label() { return localizeLabel('编辑') }, key: 'edit', onClick: handleEdit },
      { get label() { return localizeLabel('删除') }, key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    type: 'divider',
    get label() { return localizeLabel('基础信息') },
    props: {
      titlePlacement: 'left',
    },
  },
  {
    field: 'noticeTitle',
    get label() { return localizeLabel('公告标题') },
    type: 'input',
    rules: [{ required: true, get message() { return localizeLabel('请输入公告标题') }, trigger: 'blur' }],
    props: {
      get placeholder() { return localizeLabel('请输入公告标题') },
    },
  },
  {
    field: 'noticeType',
    get label() { return localizeLabel('公告类型') },
    type: 'select',
    defaultValue: 'NOTICE',
    rules: [{ required: true, get message() { return localizeLabel('请选择公告类型') }, trigger: 'change' }],
    props: {
      get placeholder() { return localizeLabel('请选择公告类型') },
      options: noticeTypeOptions.value,
    },
  },
  {
    field: 'noticeContent',
    get label() { return localizeLabel('公告内容') },
    type: 'slot',
    slotName: 'noticeContent',
    rules: [{ required: true, get message() { return localizeLabel('请输入公告内容') }, trigger: 'blur' }],
  },
  {
    type: 'divider',
    get label() { return localizeLabel('发布设置') },
    props: {
      titlePlacement: 'left',
    },
  },
  {
    field: 'publishScope',
    get label() { return localizeLabel('发布范围') },
    type: 'slot',
    slotName: 'publishScope',
    defaultValue: 0,
    props: {
      get placeholder() { return localizeLabel('请选择发布范围') },
    },
  },
  {
    field: 'orgIds',
    get label() { return localizeLabel('指定组织') },
    type: 'slot',
    slotName: 'orgIds',
    vIf: formData => formData.publishScope === 1,
    rules: [
      {
        type: 'array',
        required: true,
        get message() { return localizeLabel('请选择组织') },
        trigger: 'change',
      },
    ],
  },
  {
    field: 'attachmentIds',
    get label() { return localizeLabel('附件') },
    type: 'fileUpload',
    props: {
      multiple: true,
      limit: 10,
      fileSize: 50,
      accept: '*',
      businessType: 'notice',
      valueType: 'string',
    },
  },
  {
    field: 'effectiveTime',
    get label() { return localizeLabel('生效时间') },
    type: 'datetime',
    props: {
      get placeholder() { return localizeLabel('请选择生效时间') },
      type: 'datetime',
      clearable: true,
    },
  },
  {
    field: 'expirationTime',
    get label() { return localizeLabel('失效时间') },
    type: 'datetime',
    props: {
      get placeholder() { return localizeLabel('请选择失效时间') },
      type: 'datetime',
      clearable: true,
    },
  },
  {
    field: 'isTop',
    get label() { return localizeLabel('是否置顶') },
    type: 'radio',
    defaultValue: 0,
    props: {
      options: isTopOptions.value,
    },
  },
  {
    field: 'topSort',
    get label() { return localizeLabel('置顶排序') },
    type: 'number',
    defaultValue: 0,
    props: {
      get placeholder() { return localizeLabel('数字越大越靠前') },
      min: 0,
    },
    vIf: formData => formData.isTop === 1,
  },
  {
    field: 'remark',
    get label() { return localizeLabel('备注') },
    type: 'textarea',
    props: {
      get placeholder() { return localizeLabel('请输入备注') },
      rows: 3,
    },
  },
])

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

// 已读用户列表表格列
const readUserColumns = [
  { get title() { return localizeLabel('用户姓名') }, key: 'userName', width: 120 },
  { get title() { return localizeLabel('组织名称') }, key: 'orgName', width: 150 },
  { get title() { return localizeLabel('手机号') }, key: 'phone', width: 120 },
  { get title() { return localizeLabel('阅读时间') }, key: 'readTime', width: 180 },
]

// 未读用户列表表格列
const unreadUserColumns = [
  { get title() { return localizeLabel('用户姓名') }, key: 'userName', width: 120 },
  { get title() { return localizeLabel('组织名称') }, key: 'orgName', width: 150 },
  { get title() { return localizeLabel('手机号') }, key: 'phone', width: 120 },
  { get title() { return localizeLabel('邮箱') }, key: 'email', ellipsis: { tooltip: true } },
]

// 加载组织树数据
async function loadOrgTree() {
  try {
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      orgTreeOptions.value = transformOrgTree(res.data)
    }
  }
  catch (error) {
    console.error('加载组织树失败:', error)
  }
}

// 转换组织树数据格式
function transformOrgTree(data) {
  if (!data || data.length === 0)
    return []
  return data.map(item => ({
    key: item.id,
    label: item.orgName,
    children: item.children ? transformOrgTree(item.children) : undefined,
  }))
}

// 查看统计
async function handleViewStatistics(row) {
  try {
    // 加载统计数据
    const statsRes = await request.get(`/system/notice/statistics/${row.noticeId}`)
    if (statsRes.code === 200) {
      currentStatistics.value = statsRes.data
    }

    // 加载已读用户列表
    const readRes = await request.get(`/system/notice/read-users/${row.noticeId}`)
    if (readRes.code === 200) {
      readUserList.value = readRes.data || []
    }

    // 加载未读用户列表
    const unreadRes = await request.get(`/system/notice/unread-users/${row.noticeId}`)
    if (unreadRes.code === 200) {
      unreadUserList.value = unreadRes.data || []
    }

    showStatisticsModal.value = true
  }
  catch {
    window.$message.error(localizeLabel('加载统计数据失败'))
  }
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    get title() { return localizeLabel('确认删除') },
    get content() { return localizeLabel('确定要删除该公告吗？删除后将无法恢复！') },
    get positiveText() { return localizeLabel('确定') },
    get negativeText() { return localizeLabel('取消') },
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/remove', null, {
          params: { noticeId: row.noticeId },
        })
        if (res.code === 200) {
          window.$message.success(localizeLabel('删除成功'))
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error(localizeLabel('删除失败'))
      }
    },
  })
}

// 发布公告
function handlePublish(row) {
  window.$dialog.info({
    get title() { return localizeLabel('确认发布') },
    get content() { return localizeLabel('确定要发布该公告吗？') },
    get positiveText() { return localizeLabel('确定') },
    get negativeText() { return localizeLabel('取消') },
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/publish', null, {
          params: { noticeId: row.noticeId },
        })
        if (res.code === 200) {
          window.$message.success(localizeLabel('发布成功'))
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error(localizeLabel('发布失败'))
      }
    },
  })
}

// 撤回公告
function handleRevoke(row) {
  window.$dialog.warning({
    get title() { return localizeLabel('确认撤回') },
    get content() { return localizeLabel('确定要撤回该公告吗？') },
    get positiveText() { return localizeLabel('确定') },
    get negativeText() { return localizeLabel('取消') },
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/revoke', null, {
          params: { noticeId: row.noticeId },
        })
        if (res.code === 200) {
          window.$message.success(localizeLabel('撤回成功'))
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error(localizeLabel('撤回失败'))
      }
    },
  })
}

// 置顶/取消置顶
function handleTop(row) {
  const isTop = row.isTop === 1 ? 0 : 1
  const title = isTop === 1 ? localizeLabel('置顶公告') : localizeLabel('取消置顶')

  window.$dialog.info({
    title: t('business.admin.confirmAction', { value0: title }),
    content: t('business.admin.announcementAction', { value0: title }),
    get positiveText() { return localizeLabel('确定') },
    get negativeText() { return localizeLabel('取消') },
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/top', null, {
          params: {
            noticeId: row.noticeId,
            isTop,
            topSort: isTop === 1 ? (row.topSort || 0) : 0,
          },
        })
        if (res.code === 200) {
          window.$message.success(t('business.admin.actionSucceeded', { value0: title }))
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error(t('business.admin.actionFailed', { value0: title }))
      }
    },
  })
}

onMounted(() => {
  loadOrgTree()
})
</script>

<style scoped>
.system-notice-page {
  height: 100%;
}
</style>
