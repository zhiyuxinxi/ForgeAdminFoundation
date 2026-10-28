<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="localizeLabel(title)"
    style="width: 800px; max-width: 90vw"
    :mask-closable="false"
    @after-leave="handleClose"
  >
    <div class="user-select-container">
      <!-- 搜索条件 -->
      <div class="search-bar">
        <n-space>
          <n-input
            v-model:value="searchForm.keyword"
            :placeholder="localizeLabel('用户名/姓名')"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
          <n-tree-select
            v-model:value="searchForm.deptId"
            :options="deptTreeOptions"
            :placeholder="localizeLabel('选择部门')"
            clearable
            style="width: 200px"
            :on-load="reference ? loadDepartmentChildren : undefined"
            @update:value="handleSearch"
          />
          <n-button type="primary" @click="handleSearch">
            <template #icon>
              <i class="i-material-symbols:search" />
            </template>
            {{ localizeLabel('搜索') }}
          </n-button>
          <n-button @click="handleReset">
            <template #icon>
              <i class="i-material-symbols:refresh" />
            </template>
            {{ localizeLabel('重置') }}
          </n-button>
        </n-space>
      </div>

      <n-alert v-if="requireDepartment || requirePost" type="info" :show-icon="true">
        {{ localizeLabel('未维护部门或岗位的人员不可选择，请先完善人员档案。') }}
      </n-alert>
      <n-alert v-if="lookupError" type="error" :show-icon="false">
        {{ lookupError }}
      </n-alert>

      <!-- 用户列表 -->
      <n-data-table
        :columns="translateUiMetadata(columns)"
        :data="userList"
        :loading="loading"
        :row-key="row => String(row.id)"
        :checked-row-keys="checkedKeys"
        :single-line="false"
        :max-height="400"
        @update:checked-row-keys="handleCheck"
      />

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <n-pagination
          v-if="!reference"
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          @update:page="loadUserList"
          @update:page-size="loadUserList"
        />
        <n-space v-else>
          <n-button :disabled="loading || pagination.page <= 1" @click="changeMemberPage(-1)">
            {{ localizeLabel('上一页') }}
          </n-button>
          <span>{{ pagination.page }}</span>
          <n-button :disabled="loading || !hasNextMemberPage" @click="changeMemberPage(1)">
            {{ localizeLabel('下一页') }}
          </n-button>
        </n-space>
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">
          {{ localizeLabel('取消') }}
        </n-button>
        <n-button type="primary" :disabled="checkedKeys.length === 0" @click="handleConfirm">
          {{ localizeLabel('确定') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { NTag } from 'naive-ui'

import { computed, h, reactive, ref, watch } from 'vue'

import { loadMemberPage, loadOrganizationBranch } from '@/components/common/identity-reference'
import { localizeLabel } from '@/locales'
import { localizeMetadata as translateUiMetadata } from '@/locales/metadata'
import { request } from '@/utils/http'

const props = defineProps({
  reference: { type: Object, default: null },
  show: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '选择用户',
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  selectedUsers: {
    type: Array,
    default: () => [],
  },
  requireDepartment: {
    type: Boolean,
    default: false,
  },
  requirePost: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'confirm'])

// 状态
const visible = ref(false)
const loading = ref(false)
const userList = ref([])
const checkedKeys = ref([])
const deptTreeOptions = ref([])
const deptMetaMap = ref(new Map())
const deptLabelMap = ref(new Map())
const lookupError = ref('')
const hasNextMemberPage = ref(false)
const memberCursors = ref([undefined])
const selectionCache = new Map()
let memberRequestSeq = 0
let departmentRequestSeq = 0

// 搜索条件
const searchForm = reactive({
  keyword: '',
  deptId: null,
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

// 表格列 - 使用 computed 确保响应式
const columns = computed(() => [
  {
    type: 'selection',
    multiple: props.multiple,
    disabled: row => !isEligible(row),
  },
  {
    get title() { return localizeLabel('用户名') },
    key: 'username',
    width: 120,
  },
  {
    get title() { return localizeLabel('姓名') },
    key: 'realName',
    width: 120,
    render: row => row.realName || row.name || row.nickname || '-',
  },
  {
    get title() { return localizeLabel('部门') },
    key: 'deptName',
    ellipsis: {
      tooltip: true,
    },
  },
  {
    get title() { return localizeLabel('岗位') },
    key: 'postName',
    width: 130,
    ellipsis: { tooltip: true },
    render: row => row.postName || '-',
  },
  {
    get title() { return localizeLabel('状态') },
    key: 'status',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.userStatus === 1 ? 'success' : 'default',
        size: 'small',
      }, {
        default: () => row.userStatus === 1 ? localizeLabel('正常') : localizeLabel('禁用'),
      })
    },
  },
])

// 监听显示状态
watch(() => props.show, async (val) => {
  visible.value = val
  if (val) {
    if (await loadDeptTree() !== false && visible.value)
      loadUserList()
    // 初始化已选中的用户
    checkedKeys.value = props.selectedUsers.map(u => String(u.id))
  }
})

watch(visible, (val) => {
  emit('update:show', val)
})

// 加载部门树
async function loadDeptTree() {
  const requestSeq = ++departmentRequestSeq
  try {
    if (props.reference) {
      const options = await loadOrganizationBranch(request, props.reference)
      if (requestSeq !== departmentRequestSeq)
        return false
      deptTreeOptions.value = options
      const deptMaps = buildDeptMaps(options)
      deptMetaMap.value = deptMaps.byId
      deptLabelMap.value = deptMaps.byLabel
      searchForm.deptId = deptTreeOptions.value[0]?.value || null
      return
    }
    const res = await request.get('/system/org/tree')
    if (res.code === 200 && res.data) {
      deptTreeOptions.value = formatDeptTree(res.data)
      const deptMaps = buildDeptMaps(res.data)
      deptMetaMap.value = deptMaps.byId
      deptLabelMap.value = deptMaps.byLabel
    }
  }
  catch (e) {
    if (requestSeq === departmentRequestSeq)
      lookupError.value = e?.message || String(e)
    console.error('加载部门树失败', e)
    return false
  }
}

// 格式化部门树
function formatDeptTree(depts) {
  return depts.map(dept => ({
    label: dept.name || dept.orgName,
    key: dept.id,
    value: dept.id,
    children: dept.children ? formatDeptTree(dept.children) : undefined,
  }))
}

function buildDeptMaps(depts, maps = { byId: new Map(), byLabel: new Map() }) {
  depts.forEach((dept) => {
    const label = dept.name || dept.orgName || ''
    const meta = {
      id: dept.id == null ? null : String(dept.id),
      label,
      leaderId: dept.leaderId == null ? null : String(dept.leaderId),
      leaderName: dept.leaderName || '',
    }
    if (meta.id)
      maps.byId.set(meta.id, meta)
    if (label)
      maps.byLabel.set(label, meta)
    if (dept.children?.length)
      buildDeptMaps(dept.children, maps)
  })
  return maps
}

function normalizeUser(user) {
  const reportedDeptName = user.deptName || user.orgName || ''
  const postName = user.postName || user.positionName || (Array.isArray(user.postNames) ? user.postNames.join('、') : '')
  const dept = deptMetaMap.value.get(String(user.deptId ?? user.orgId ?? user.createDept ?? ''))
    || deptLabelMap.value.get(reportedDeptName)
  return {
    ...user,
    name: user.name || user.realName || user.nickname || user.username,
    realName: user.realName || user.name || user.nickname,
    deptId: user.deptId ?? user.orgId ?? dept?.id ?? null,
    deptName: reportedDeptName || dept?.label || '',
    postName,
    leaderId: user.leaderId ?? dept?.leaderId ?? null,
    leaderName: user.leaderName || user.directLeaderName || dept?.leaderName || '',
  }
}

function isEligible(user) {
  if (props.requireDepartment && !String(user?.deptName || '').trim())
    return false
  if (props.requirePost && !String(user?.postName || '').trim())
    return false
  return true
}

// 加载用户列表
async function loadUserList() {
  const requestSeq = ++memberRequestSeq
  loading.value = true
  lookupError.value = ''
  try {
    if (props.reference) {
      const memberOrgId = searchForm.deptId
      const page = await loadMemberPage(request, props.reference, {
        orgId: memberOrgId,
        keyword: searchForm.keyword,
        afterUserId: memberCursors.value[pagination.page - 1],
        pageSize: pagination.pageSize,
      })
      if (requestSeq !== memberRequestSeq)
        return
      userList.value = page.rows.map(user => normalizeUser({ ...user, deptId: memberOrgId }))
      userList.value.forEach(user => selectionCache.set(String(user.id), user))
      hasNextMemberPage.value = page.hasNext
      return
    }
    const res = await request.get('/system/user/page', {
      params: {
        pageNum: pagination.page,
        pageSize: pagination.pageSize,
        keyword: searchForm.keyword || undefined,
        orgId: searchForm.deptId || undefined,
      },
    })
    if (res.code === 200 && res.data) {
      userList.value = (res.data.records || []).map(normalizeUser)
      pagination.total = res.data.total || 0
    }
  }
  catch (e) {
    if (requestSeq === memberRequestSeq) {
      lookupError.value = e?.message || String(e)
      userList.value = []
      hasNextMemberPage.value = false
    }
    console.error('加载用户列表失败', e)
  }
  finally {
    if (requestSeq === memberRequestSeq)
      loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  memberCursors.value = [undefined]
  loadUserList()
}

// 重置
function handleReset() {
  searchForm.keyword = ''
  searchForm.deptId = props.reference ? deptTreeOptions.value[0]?.value || null : null
  pagination.page = 1
  memberCursors.value = [undefined]
  loadUserList()
}

// 选择处理
function handleCheck(keys) {
  const ineligibleKeys = new Set(userList.value.filter(user => !isEligible(user)).map(user => String(user.id)))
  const eligibleKeys = keys.map(String).filter(key => !ineligibleKeys.has(key))
  if (props.multiple) {
    checkedKeys.value = eligibleKeys
  }
  else {
    // 单选模式
    checkedKeys.value = eligibleKeys.length > 0 ? [String(eligibleKeys[eligibleKeys.length - 1])] : []
  }
}

// 确认
function handleConfirm() {
  const userMap = new Map()
  ;[...props.selectedUsers, ...selectionCache.values(), ...userList.value].forEach((user) => {
    if (user?.id !== null && user?.id !== undefined)
      userMap.set(String(user.id), user)
  })
  const selectedUsers = checkedKeys.value
    .map(key => userMap.get(String(key)))
    .filter(user => user && isEligible(user))
  emit('confirm', props.multiple ? selectedUsers : selectedUsers[0])
  handleClose()
}

// 关闭
function handleClose() {
  memberRequestSeq++
  departmentRequestSeq++
  visible.value = false
  checkedKeys.value = []
  searchForm.keyword = ''
  searchForm.deptId = null
  pagination.page = 1
  memberCursors.value = [undefined]
  selectionCache.clear()
  lookupError.value = ''
}

async function loadDepartmentChildren(option) {
  try {
    option.children = await loadOrganizationBranch(request, props.reference, option.value)
    option.isLeaf = option.children.length === 0
    buildDeptMaps(option.children, { byId: deptMetaMap.value, byLabel: deptLabelMap.value })
  }
  catch (error) {
    lookupError.value = error?.message || String(error)
    throw error
  }
}

function changeMemberPage(direction) {
  if (direction > 0) {
    if (!hasNextMemberPage.value)
      return
    memberCursors.value[pagination.page] = userList.value.at(-1)?.id
  }
  pagination.page = Math.max(1, pagination.page + direction)
  loadUserList()
}
</script>

<style scoped>
.user-select-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
