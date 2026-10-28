<template>
  <n-tabs v-model:value="activeTab" type="line" animated>
    <n-tab-pane name="approval" :tab="localizeLabel('审批待办', { 'en-US': 'Approvals' })">
      <TodoPage />
    </n-tab-pane>
    <n-tab-pane name="business" :tab="localizeLabel('业务任务', { 'en-US': 'Business tasks' })">
      <BusinessTaskInbox :task-id="route.query.taskId" />
    </n-tab-pane>
  </n-tabs>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BusinessTaskInbox from '@/components/task/BusinessTaskInbox.vue'
import { localizeLabel } from '@/locales'
import TodoPage from '@/views/flow/todo.vue'

const route = useRoute()
const activeTab = ref(route.query.tab === 'business' ? 'business' : 'approval')
watch(() => [route.query.tab, route.query.taskId], () => {
  if (route.query.tab === 'business' || route.query.taskId)
    activeTab.value = 'business'
})
</script>
