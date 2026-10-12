function normalizeValue(value) {
  if (value === undefined || value === null)
    return ''
  return String(value)
}

export function buildFlowCategoryTreeOptions(treeData = []) {
  return (Array.isArray(treeData) ? treeData : []).map((item) => {
    const id = normalizeValue(item.id)
    const categoryCode = normalizeValue(item.categoryCode)
    const value = id || categoryCode
    const children = buildFlowCategoryTreeOptions(item.children || [])
    return {
      label: item.categoryName || categoryCode || id,
      value,
      key: value,
      categoryId: id,
      categoryCode,
      raw: item,
      children: children.length > 0 ? children : undefined,
    }
  })
}

export function flattenFlowCategoryOptions(options = []) {
  const result = []
  const source = Array.isArray(options) ? options : []
  source.forEach((item) => {
    result.push(item)
    if (Array.isArray(item.children))
      result.push(...flattenFlowCategoryOptions(item.children))
  })
  return result
}

export function findFlowCategoryOption(value, options = []) {
  const text = normalizeValue(value)
  if (!text)
    return null

  return flattenFlowCategoryOptions(options).find(item =>
    normalizeValue(item.value) === text
    || normalizeValue(item.categoryId) === text
    || normalizeValue(item.categoryCode) === text
    || normalizeValue(item.label) === text,
  ) || null
}

export function resolveFlowCategoryValue(value, options = []) {
  return findFlowCategoryOption(value, options)?.value || value
}

export function resolveFlowCategoryLabel(value, options = [], fallback = '') {
  const option = findFlowCategoryOption(value, options)
  if (option?.label)
    return option.label
  return fallback
}
