const FLOW_TASK_LIST_PATHS = new Set(['/flow/todo', '/flow/done', '/flow/started', '/flow/cc'])

export function isFlowTaskListPath(path) {
  return FLOW_TASK_LIST_PATHS.has(path)
}
