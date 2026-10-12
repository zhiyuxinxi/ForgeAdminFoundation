import { request } from '@/utils'

export function getWorkspaceSummary() {
  return request.get('/api/workspace/summary')
}

export function getWorkspaceTodoCount() {
  return request.get('/api/workspace/todo-count')
}
