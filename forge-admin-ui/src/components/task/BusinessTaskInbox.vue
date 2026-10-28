<template>
  <section class="business-task-inbox">
    <n-space align="center" justify="space-between">
      <h2>{{ label('业务任务', 'Business tasks') }}</h2>
      <n-space>
        <n-select v-model:value="state" :options="stateOptions" style="width: 160px" :aria-label="label('任务状态', 'Task state')" @update:value="load()" />
        <n-button :loading="loading" @click="load()">
          {{ label('刷新', 'Refresh') }}
        </n-button>
      </n-space>
    </n-space>
    <n-alert v-if="listError" type="error" :bordered="false">
      {{ listError }}
    </n-alert>
    <n-spin :show="loading">
      <n-empty v-if="!tasks.length && !loading && !listError" :description="label('暂无任务', 'No tasks')" />
      <n-list v-else>
        <n-list-item v-for="task in tasks" :key="task.id">
          <n-space align="center" justify="space-between">
            <div>
              <strong>{{ task.title }}</strong>
              <p>{{ stateLabel(task.state) }}<span v-if="task.dueAt"> · {{ label('截止时间', 'Due') }} {{ task.dueAt }}</span></p>
            </div>
            <n-button :data-task-id="task.id" @click="open(task.id)">
              {{ label('查看任务', 'View task') }}
            </n-button>
          </n-space>
        </n-list-item>
      </n-list>
    </n-spin>
    <n-button v-if="hasMore" :disabled="loading" @click="load(true)">
      {{ label('加载更多', 'Load more') }}
    </n-button>

    <n-modal v-model:show="showDetail" preset="card" :closable="!busy" :mask-closable="!busy" :title="selected?.title || label('任务详情', 'Task details')" style="width: min(720px, 94vw)">
      <n-spin :show="detailLoading">
        <n-alert v-if="detailError" type="error" :bordered="false">
          {{ detailError }}
        </n-alert>
        <template v-if="selected">
          <p>{{ label('状态', 'State') }}: {{ stateLabel(selected.state) }}</p>
          <p>{{ label('执行人', 'Assignee') }}: {{ selected.assigneeName }}</p>
          <p v-if="selected.dueAt">
            {{ label('截止时间', 'Due') }}: {{ selected.dueAt }}
          </p>
          <p v-if="selected.completedAt">
            {{ label('完成时间', 'Completed') }}: {{ selected.completedAt }}
          </p>
          <p v-if="selected.cancellationReason">
            {{ label('取消原因', 'Cancellation reason') }}: {{ selected.cancellationReason }}
          </p>
          <p v-if="selected.sourceType === 'task.periodic'">
            {{ label('完成本周期的工作后，点击完成任务确认。', 'Confirm completion after finishing the work for this period.') }}
          </p>
          <n-alert v-if="formError" type="warning" :bordered="false">
            {{ formError }}
          </n-alert>
          <AiForm
            v-if="formReady"
            ref="formRef"
            v-model:value="values"
            :schema="visibleFields"
            :show-actions="false"
          />
          <n-space justify="end">
            <n-button :disabled="busy || detailLoading" @click="open(selected.id)">
              {{ label('刷新详情', 'Refresh details') }}
            </n-button>
            <n-button v-if="isAssignee && selected.state === 'ASSIGNED'" data-start type="primary" :loading="busy" :disabled="detailLoading" @click="start">
              {{ label('开始任务', 'Start task') }}
            </n-button>
            <n-button v-if="isAssignee && selected.state === 'IN_PROGRESS'" data-complete type="primary" :loading="busy" :disabled="detailLoading || !formReady" @click="complete">
              {{ label('完成任务', 'Complete task') }}
            </n-button>
          </n-space>
        </template>
      </n-spin>
    </n-modal>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { assignedBusinessTasks, businessTaskForm, completeBusinessTask, getBusinessTask, startBusinessTask } from '@/api/business-task'
import AiForm from '@/components/ai-form/AiForm.vue'
import { localizeLabel } from '@/locales'
import { useUserStore } from '@/store'

const props = defineProps({ taskId: { type: [String, Number], default: null } })
const user = useUserStore()
const label = (zh, en) => localizeLabel(zh, { 'en-US': en })
const states = { ASSIGNED: ['待执行', 'Assigned'], IN_PROGRESS: ['执行中', 'In progress'], COMPLETED: ['已完成', 'Completed'], CANCELLED: ['已取消', 'Cancelled'], DRAFT: ['待派发', 'Draft'] }
const stateLabel = value => states[value] ? label(...states[value]) : value
const stateOptions = computed(() => [{ label: label('全部状态', 'All states'), value: '' }, ...Object.keys(states).filter(value => value !== 'DRAFT').map(value => ({ value, label: stateLabel(value) }))])
const state = ref('')
const tasks = ref([])
const loading = ref(false)
const listError = ref('')
const hasMore = ref(false)
const selected = ref(null)
const showDetail = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const formError = ref('')
const formReady = ref(false)
const fields = ref([])
const values = ref({})
const formRef = ref(null)
const busy = ref(false)
const isAssignee = computed(() => user.userId != null && String(selected.value?.assigneeId) === String(user.userId))
const editable = computed(() => isAssignee.value && selected.value?.state === 'IN_PROGRESS')
const visibleFields = computed(() => fields.value.map(field => ({ ...field, disabled: !editable.value, props: { ...field.props, disabled: !editable.value } })))
let listEpoch = 0
let detailEpoch = 0
let pendingCompletion = null

async function load(append = false) {
  const epoch = ++listEpoch
  loading.value = true
  listError.value = ''
  try {
    const rows = await assignedBusinessTasks({ state: state.value || undefined, afterId: append ? tasks.value.at(-1)?.id : undefined, limit: 50 })
    if (epoch !== listEpoch)
      return
    tasks.value = append ? [...tasks.value, ...rows] : rows
    hasMore.value = rows.length === 50
  }
  catch (error) {
    if (epoch === listEpoch)
      listError.value = error.message || label('任务加载失败', 'Unable to load tasks')
  }
  finally {
    if (epoch === listEpoch)
      loading.value = false
  }
}

async function open(id) {
  const epoch = ++detailEpoch
  showDetail.value = true
  detailLoading.value = true
  detailError.value = ''
  formError.value = ''
  formReady.value = false
  selected.value = null
  fields.value = []
  values.value = {}
  pendingCompletion = null
  try {
    const task = await getBusinessTask(id)
    if (epoch !== detailEpoch)
      return
    selected.value = task
    if (task.sourceType === 'task.periodic') {
      formReady.value = true
      return
    }
    if (task.sourceType !== 'lowcode.record') {
      formError.value = label('请从来源业务页面填写该任务的执行结果', 'Complete this task from its business page')
      return
    }
    try {
      const form = await businessTaskForm(id)
      if (epoch !== detailEpoch)
        return
      fields.value = form.fields
      values.value = form.values
      formReady.value = true
    }
    catch (error) {
      if (epoch === detailEpoch)
        formError.value = error.message || label('结果表单加载失败', 'Unable to load the result form')
    }
  }
  catch (error) {
    if (epoch === detailEpoch)
      detailError.value = error.message || label('任务详情加载失败', 'Unable to load task details')
  }
  finally {
    if (epoch === detailEpoch)
      detailLoading.value = false
  }
}

async function start() {
  if (busy.value)
    return
  busy.value = true
  detailError.value = ''
  const epoch = detailEpoch
  const id = selected.value.id
  try {
    const task = await startBusinessTask(id)
    if (epoch === detailEpoch)
      selected.value = task
    await load()
  }
  catch (error) {
    if (epoch === detailEpoch)
      detailError.value = error.message || label('开始任务失败', 'Unable to start the task')
  }
  finally {
    busy.value = false
  }
}

async function complete() {
  if (busy.value || !formReady.value)
    return
  busy.value = true
  detailError.value = ''
  const epoch = detailEpoch
  const id = selected.value.id
  try {
    await formRef.value?.validate()
    if (epoch !== detailEpoch)
      return
    const result = {}
    for (const field of fields.value) {
      const value = values.value[field.field]
      if (value == null || value === '')
        throw new Error(label('请填写全部任务结果', 'Complete all task result fields'))
      result[field.field] = value
    }
    const resultJson = JSON.stringify(result)
    if (pendingCompletion?.resultJson !== resultJson)
      pendingCompletion = { requestKey: `${Date.now()}-${Array.from(globalThis.crypto.getRandomValues(new Uint32Array(4))).join('-')}`, resultJson }
    const task = await completeBusinessTask(id, pendingCompletion)
    if (epoch === detailEpoch)
      selected.value = task
    await load()
  }
  catch (error) {
    if (epoch === detailEpoch)
      detailError.value = error.message || label('完成失败，请检查结果后重试', 'Unable to complete; check the result and retry')
  }
  finally {
    busy.value = false
  }
}

watch(() => props.taskId, (id) => {
  if (id)
    open(id)
})
onMounted(() => {
  load()
  if (props.taskId)
    open(props.taskId)
})
</script>

<style scoped>
.business-task-inbox {
  padding: 16px;
}
.business-task-inbox h2 {
  margin: 0;
  font-size: 18px;
}
.business-task-inbox p {
  margin: 6px 0;
  color: #64748b;
}
</style>
