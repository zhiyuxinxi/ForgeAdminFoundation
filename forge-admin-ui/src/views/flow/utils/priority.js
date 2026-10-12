export const FLOW_PRIORITY_LABEL_FALLBACK = {
  0: '低',
  1: '普通',
  2: '高',
  3: '紧急',
}

export function resolveFlowPriorityLevel(priority) {
  if (priority === undefined || priority === null || priority === '')
    return null

  const value = Number(priority)
  if (!Number.isFinite(value))
    return null

  if (value >= 0 && value <= 3)
    return value
  if (value >= 80)
    return 3
  if (value >= 60)
    return 2
  if (value >= 40)
    return 1
  return 0
}

export function shouldShowFlowPriority(priority) {
  const level = resolveFlowPriorityLevel(priority)
  return level !== null && level !== 1
}

export function getFlowPriorityClass(priority) {
  const level = resolveFlowPriorityLevel(priority)
  const classMap = {
    0: 'low',
    2: 'high',
    3: 'urgent',
  }
  return classMap[level] || ''
}

export function isUrgentFlowPriority(priority) {
  return resolveFlowPriorityLevel(priority) === 3
}
