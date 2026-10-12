import { extractFormCreateFieldRefs } from './formCreateToForge'
import { camelToSnake, normalizeFormDesignerSchema } from './formDesignerSchema'

export function extractDesignerFieldRefs(source = {}) {
  const refs = extractRawDesignerFieldRefs(source)
  return uniqueRefs(refs)
}

function extractRawDesignerFieldRefs(source = {}) {
  const refs = []
  if (source.formDesignerSchema || source.components)
    refs.push(...extractRawForgeRefs(source.formDesignerSchema || source))
  if (source.formCreateRule || source.rules)
    refs.push(...extractFormCreateFieldRefs(source.formCreateRule || source.rules))
  return refs
}

export function inspectDesignerFieldRefs(source = {}, fields = []) {
  const fieldCodes = new Set(fields.map(field => field.fieldCode || field.field).filter(Boolean))
  const refs = extractRawDesignerFieldRefs(source)
  const duplicates = refs.filter((ref, index, all) => all.indexOf(ref) !== index)
  const missing = refs.filter(ref => !fieldCodes.has(ref))
  const unused = Array.from(fieldCodes).filter(ref => !refs.includes(ref))
  return {
    refs: uniqueRefs(refs),
    duplicates: uniqueRefs(duplicates),
    missing: uniqueRefs(missing),
    unused,
    valid: missing.length === 0 && duplicates.length === 0,
  }
}

function extractRawForgeRefs(schema = {}) {
  const refs = []
  const walk = (components = []) => {
    components.forEach((component) => {
      if (component.fieldBinding?.mode === 'field' && component.fieldBinding?.fieldCode)
        refs.push(component.fieldBinding.fieldCode)
      if (Array.isArray(component.children))
        walk(component.children)
    })
  }
  walk(normalizeFormDesignerSchema(schema).components)
  return refs
}

export function repairFormDesignerFieldRefs(schema = {}, fields = [], strategy = 'mark') {
  const fieldCodes = new Set(fields.map(field => field.fieldCode || field.field).filter(Boolean))
  const normalized = normalizeFormDesignerSchema(schema)
  const seen = new Set()
  normalized.components = normalized.components
    .map(component => repairComponent(component, fieldCodes, seen, strategy))
    .filter(Boolean)
  return normalized
}

export function renameFormDesignerFieldRefs(schema = {}, options = {}) {
  const oldFieldCode = String(options.oldFieldCode || '').trim()
  const newFieldCode = String(options.newFieldCode || '').trim()
  const normalized = normalizeFormDesignerSchema(schema)
  if (!oldFieldCode || !newFieldCode || oldFieldCode === newFieldCode)
    return normalized
  const oldColumnName = options.oldColumnName || camelToSnake(oldFieldCode)
  const newColumnName = options.newColumnName || camelToSnake(newFieldCode)
  normalized.components = normalized.components.map(component => renameComponentFieldRefs(component, {
    oldFieldCode,
    newFieldCode,
    oldColumnName,
    newColumnName,
    oldFieldName: options.oldFieldName || '',
    newFieldName: options.newFieldName || '',
  }))
  return normalized
}

export function buildRepairActions(schema = {}, fields = []) {
  const fieldCodes = new Set(fields.map(field => field.fieldCode || field.field).filter(Boolean))
  const actions = []
  const seen = new Set()
  normalizeFormDesignerSchema(schema).components.forEach((component) => {
    collectRepairActions(component, fieldCodes, seen, actions)
  })
  return actions
}

function repairComponent(component = {}, fieldCodes, seen, strategy) {
  const binding = component.fieldBinding || {}
  if (binding.mode === 'field' && binding.fieldCode) {
    if (!fieldCodes.has(binding.fieldCode)) {
      if (strategy === 'remove')
        return null
      component.props = {
        ...(component.props || {}),
        _missingField: true,
      }
    }
    if (seen.has(binding.fieldCode)) {
      if (strategy === 'removeDuplicate')
        return null
      component.props = {
        ...(component.props || {}),
        _duplicateField: true,
      }
    }
    seen.add(binding.fieldCode)
  }
  if (Array.isArray(component.children)) {
    component.children = component.children
      .map(child => repairComponent(child, fieldCodes, seen, strategy))
      .filter(Boolean)
  }
  return component
}

function collectRepairActions(component = {}, fieldCodes, seen, actions) {
  const binding = component.fieldBinding || {}
  if (binding.mode === 'field' && binding.fieldCode) {
    if (!fieldCodes.has(binding.fieldCode)) {
      actions.push({
        type: 'missingField',
        componentId: component.id,
        fieldCode: binding.fieldCode,
        label: component.label,
        actions: ['removeComponent', 'rebindField', 'createField'],
      })
    }
    if (seen.has(binding.fieldCode)) {
      actions.push({
        type: 'duplicateField',
        componentId: component.id,
        fieldCode: binding.fieldCode,
        label: component.label,
        actions: ['removeComponent', 'allowReuse'],
      })
    }
    seen.add(binding.fieldCode)
  }
  if (Array.isArray(component.children)) {
    component.children.forEach(child => collectRepairActions(child, fieldCodes, seen, actions))
  }
}

function renameComponentFieldRefs(component = {}, context = {}) {
  const next = {
    ...component,
    fieldBinding: renameFieldBinding(component.fieldBinding || {}, context),
    props: renameComponentProps(component.props || {}, context),
  }
  if (next.fieldBinding?.fieldCode === context.newFieldCode && shouldSyncComponentLabel(next.label, context))
    next.label = context.newFieldName
  if (next.id === `cmp_${context.oldFieldCode}`)
    next.id = `cmp_${context.newFieldCode}`
  if (Array.isArray(component.children))
    next.children = component.children.map(child => renameComponentFieldRefs(child, context))
  return next
}

function renameFieldBinding(binding = {}, context = {}) {
  const next = { ...binding }
  if (next.fieldCode === context.oldFieldCode)
    next.fieldCode = context.newFieldCode
  if (context.oldColumnName && next.columnName === context.oldColumnName)
    next.columnName = context.newColumnName
  return next
}

function renameComponentProps(props = {}, context = {}) {
  const next = { ...props }
  if (next.fieldCode === context.oldFieldCode)
    next.fieldCode = context.newFieldCode
  if (context.oldColumnName && next.columnName === context.oldColumnName)
    next.columnName = context.newColumnName
  if (next.fieldBinding && typeof next.fieldBinding === 'object')
    next.fieldBinding = renameFieldBinding(next.fieldBinding, context)
  return next
}

function shouldSyncComponentLabel(label = '', context = {}) {
  if (!context.newFieldName)
    return false
  const current = String(label || '').trim()
  return !current
    || current === context.oldFieldName
    || current === context.oldFieldCode
    || current === context.oldColumnName
}

function uniqueRefs(refs = []) {
  return refs.filter((ref, index, all) => ref && all.indexOf(ref) === index)
}
