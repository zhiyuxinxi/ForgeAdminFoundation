import { request } from '@/utils/http'

function data(response) {
  if (response?.code !== 200 || response.data == null)
    throw new Error(response?.msg || response?.message || 'Task request failed')
  return response.data
}

export async function assignedBusinessTasks(params) {
  const rows = data(await request.get('/api/task/assigned', { params }))
  if (!Array.isArray(rows))
    throw new Error('Invalid task list response')
  return rows
}

export async function getBusinessTask(id) {
  return data(await request.get(`/api/task/${encodeURIComponent(id)}`))
}

export async function startBusinessTask(id) {
  return data(await request.post(`/api/task/${encodeURIComponent(id)}/start`))
}

export async function completeBusinessTask(id, command) {
  return data(await request.post(`/api/task/${encodeURIComponent(id)}/complete`, command))
}

export async function businessTaskForm(id) {
  const form = data(await request.get(`/ai/business-tasks/${encodeURIComponent(id)}/form`))
  if (!Array.isArray(form.fields) || !form.values || typeof form.values !== 'object')
    throw new Error('Invalid task form response')
  return form
}

export async function businessTaskActions(configKey) {
  const rows = data(await request.get(`/ai/crud/${encodeURIComponent(configKey)}/task-actions`))
  if (!Array.isArray(rows))
    throw new Error('Invalid task action response')
  return rows
}

export async function openLinkedBusinessTask(action, row, rowKey = 'id') {
  const recordId = row?.[rowKey]
  if (recordId == null || !action?.configKey || !action?.bindingKey)
    throw new Error('Task action is missing its business record')
  const url = `/ai/crud/${encodeURIComponent(action.configKey)}/tasks/${encodeURIComponent(action.bindingKey)}/${encodeURIComponent(recordId)}`
  return data(await (row[action.taskIdField] ? request.get(url) : request.post(url)))
}
