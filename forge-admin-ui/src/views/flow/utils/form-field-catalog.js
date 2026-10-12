export function buildLocalFormFieldCatalog(source) {
  const root = parseSchema(source)
  const fields = []
  collectFields(root, fields)
  return mergeByField(fields)
}

function parseSchema(source) {
  if (Array.isArray(source))
    return source
  if (source && typeof source === 'object')
    return source
  if (typeof source === 'string' && source.trim()) {
    try {
      return JSON.parse(source)
    }
    catch {
      return []
    }
  }
  return []
}

function collectFields(node, fields) {
  if (!node)
    return
  if (Array.isArray(node)) {
    node.forEach(child => collectFields(child, fields))
    return
  }
  if (typeof node !== 'object')
    return

  const field = resolveField(node)
  if (field && !field.startsWith('ref_')) {
    fields.push({
      field,
      label: resolveLabel(node, field),
      componentType: textValue(node, 'type') || textValue(node, 'component') || textValue(node, 'componentKey') || '',
      dataType: inferFieldDataType(node),
      required: resolveRequired(node),
      optionSource: resolveOptionSource(node),
      source: 'model-inline',
    })
  }

  Object.entries(node).forEach(([key, value]) => {
    if (['props', '_fc_drag_tag'].includes(key))
      return
    collectFields(value, fields)
  })
}

function resolveField(node) {
  const candidates = [
    textValue(node, 'field'),
    textValue(node, 'fieldCode'),
    textValue(node, 'fieldName'),
    textValue(node.props, 'field'),
    textValue(node.props, 'fieldCode'),
    textValue(node.props, 'prop'),
    textValue(node.fieldBinding, 'fieldCode'),
    textValue(node._forge?.fieldBinding, 'fieldCode'),
    textValue(node, 'key'),
    textValue(node, 'name'),
  ].filter(Boolean)
  return candidates.find(item => !item.startsWith('ref_')) || candidates[0] || ''
}

function resolveLabel(node, fallback) {
  const label = textValue(node, 'label')
    || textValue(node, 'title')
    || textValue(node, 'fieldName')
    || textValue(node.props, 'label')
    || textValue(node.props, 'title')
    || textValue(node.props, 'fieldName')
  return label || fallback
}

function resolveRequired(node) {
  if (typeof node.required === 'boolean')
    return node.required
  if (Array.isArray(node.validate))
    return node.validate.some(item => !!item?.required)
  if (Array.isArray(node.rules))
    return node.rules.some(item => !!item?.required)
  return false
}

function resolveOptionSource(node) {
  return textValue(node, 'optionSource')
    || textValue(node.props, 'optionSource')
    || textValue(node.props, 'dictType')
    || ''
}

function inferFieldDataType(node) {
  const type = String(
    textValue(node, 'dataType')
    || textValue(node, 'type')
    || textValue(node, 'component')
    || textValue(node, 'componentType')
    || textValue(node, 'componentKey')
    || '',
  ).toLowerCase()
  if (['inputnumber', 'number', 'integer', 'decimal', 'slider', 'rate'].some(key => type.includes(key)))
    return 'number'
  if (['switch', 'checkbox', 'boolean'].some(key => type.includes(key)))
    return 'boolean'
  if (['date', 'time'].some(key => type.includes(key)))
    return 'datetime'
  if (['select', 'radio', 'cascader', 'tree', 'enum'].some(key => type.includes(key)))
    return 'enum'
  return 'string'
}

function mergeByField(fields) {
  const merged = new Map()
  fields.forEach((item) => {
    if (item?.field && !merged.has(item.field))
      merged.set(item.field, item)
  })
  return Array.from(merged.values())
}

function textValue(source, key) {
  const value = source?.[key]
  if (value === undefined || value === null)
    return ''
  const text = String(value).trim()
  return text || ''
}
