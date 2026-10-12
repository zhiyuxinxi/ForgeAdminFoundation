<script setup>
import { localizeLabel } from '@/locales'

defineProps({
  tasks: { type: Array, default: () => [] },
})

function taskStatus(status) {
  const labels = {
    running: ['处理中', 'In progress'],
    completed: ['已完成', 'Completed'],
    canceled: ['已取消', 'Cancelled'],
    unknown: ['状态未知', 'Unknown status'],
  }
  const [chinese, english] = labels[status] || labels.unknown
  return localizeLabel(chinese, { 'en-US': english })
}
</script>

<template>
  <ul v-if="tasks.length" class="node-task-list">
    <li v-for="task in tasks" :key="task.taskId" :data-task-id="task.taskId">
      <div>
        <strong>{{ task.assigneeName || task.assigneeId || localizeLabel('未指派', { 'en-US': 'Unassigned' }) }}</strong>
        <span>{{ taskStatus(task.status) }}</span>
      </div>
      <div>{{ localizeLabel('任务ID', { 'en-US': 'Task ID' }) }}: {{ task.taskId }}</div>
      <div v-if="task.startTime">
        {{ localizeLabel('开始', { 'en-US': 'Started' }) }}: {{ task.startTime }}
      </div>
      <div v-if="task.endTime">
        {{ localizeLabel('完成', { 'en-US': 'Ended' }) }}: {{ task.endTime }}
      </div>
    </li>
  </ul>
</template>

<style scoped>
.node-task-list {
  margin: 8px 0;
  padding: 0;
  list-style: none;
  max-height: 260px;
  overflow-y: auto;
}
.node-task-list li {
  padding: 6px 0;
  border-bottom: 1px solid #e5e7eb;
  overflow-wrap: anywhere;
  font-size: 12px;
}
.node-task-list li > div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
</style>
