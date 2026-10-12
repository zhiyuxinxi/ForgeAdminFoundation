export function buildDesignerEditSchema(zone = {}, fieldMap = new Map()) {
  const refs = resolveDesignerZoneRefs(zone, fieldMap)
  const settings = zone.props?.fieldSettings || {}
  const fields = refs.map((fieldCode) => {
    const field = fieldMap.get(fieldCode) || {}
    return buildDesignerRuntimeField(field, settings[fieldCode] || {}, 'form')
  })
  const layout = hydrateDesignerFormLayout(
    zone.props?.formLayout || [],
    new Map(fields.map(field => [field.field, field])),
  )
  return layout.length ? layout : fields
}

export function buildDesignerRuntimeField(field = {}, setting = {}, mode = 'form') {
  const fieldCode = field.field || field.fieldCode || ''
  const type = normalizeDesignerRuntimeFieldType(setting.componentType || field.componentType || field.type || field.dataType)
  const runtimeField = {
    field: fieldCode,
    label: setting.label || field.label || field.fieldName || fieldCode,
    type,
    placeholder: setting.placeholder || field.placeholder || (mode === 'search' ? `请输入${field.label || fieldCode}` : `请输入${field.label || fieldCode}`),
    span: Number(setting.span || field.span || 1),
    clearable: setting.clearable ?? field.clearable ?? true,
    options: setting.options || field.options || [],
    dictType: setting.dictType || field.dictType || '',
    props: {
      ...(field.props || {}),
      ...(setting.props || {}),
    },
  }
  ;[
    'labelWidth',
    'required',
    'requiredMessage',
    'rules',
    'trigger',
    'readonly',
    'disabled',
    'defaultValue',
    'componentStyle',
    'componentClass',
    'formItemStyle',
    'formItemClass',
    'showLabel',
  ].forEach((key) => {
    const value = setting[key] !== undefined ? setting[key] : field[key]
    if (value !== undefined)
      runtimeField[key] = value
  })
  return runtimeField
}

export function hydrateDesignerFormLayout(layout = [], fieldMap = new Map(), usedFields = new Set()) {
  return (Array.isArray(layout) ? layout : [])
    .map(node => hydrateDesignerFormLayoutNode(node, fieldMap, usedFields))
    .filter(Boolean)
}

function resolveDesignerZoneRefs(zone = {}, fieldMap = new Map()) {
  const refs = Array.isArray(zone.fieldRefs) && zone.fieldRefs.length
    ? zone.fieldRefs
    : Array.from(fieldMap.keys())
  return refs.filter(fieldCode => fieldMap.has(fieldCode))
}

function hydrateDesignerFormLayoutNode(node = {}, fieldMap = new Map(), usedFields = new Set()) {
  if (!node || typeof node !== 'object')
    return null
  if (node.nodeType === 'field') {
    const field = fieldMap.get(node.field)
    if (!field)
      return null
    usedFields.add(node.field)
    return {
      ...field,
      nodeType: 'field',
      key: node.key || field.field,
      span: node.span || field.span,
      gridStyle: node.gridStyle || field.gridStyle,
    }
  }
  const children = hydrateDesignerFormLayout(node.children || [], fieldMap, usedFields)
  if (!children.length && !isStandaloneDesignerLayoutNode(node))
    return null
  return {
    ...node,
    children,
  }
}

function isStandaloneDesignerLayoutNode(node = {}) {
  return ['divider', 'groupTitle', 'button'].includes(node.nodeType || node.componentKey || node.type)
}

function normalizeDesignerRuntimeFieldType(type = '') {
  if (['int', 'bigint', 'decimal', 'number', 'inputNumber'].includes(type))
    return 'number'
  if (['datetime', 'date', 'time'].includes(type))
    return type
  if (['textarea', 'select', 'dictSelect', 'checkbox', 'radio', 'switch', 'treeSelect', 'userSelect'].includes(type))
    return type
  return 'input'
}
