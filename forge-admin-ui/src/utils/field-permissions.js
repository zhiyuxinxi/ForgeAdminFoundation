export function normalizeFieldPermissions(source, options = {}) {
  const list = parsePermissionList(source)
  if (!Array.isArray(list))
    return []

  return list
    .map((item) => {
      if (!item || typeof item !== 'object')
        return null
      const field = String(item.field || item.fieldCode || item.code || item.name || '').trim()
      if (!field)
        return null
      const readable = readPermissionBoolean(item.readable, readPermissionBoolean(item.visible, true))
      const writable = readable && !options.readOnly && readPermissionBoolean(item.writable, readPermissionBoolean(item.editable, true))
      return {
        ...item,
        field,
        fieldCode: field,
        visible: readable,
        editable: writable,
        readable,
        writable,
        required: writable && readPermissionBoolean(item.required, false),
      }
    })
    .filter(Boolean)
}

export function createFieldPermissionMap(source, options = {}) {
  const map = new Map()
  for (const item of normalizeFieldPermissions(source, options)) {
    for (const key of permissionFieldAliases(item.field))
      map.set(key, item)
  }
  return map
}

export function pickFirstNonEmptyFieldPermissions(sources = [], options = {}) {
  const list = Array.isArray(sources) ? sources : [sources]
  for (const source of list) {
    const permissions = normalizeFieldPermissions(source, options)
    if (permissions.length)
      return permissions
  }
  return []
}

function parsePermissionList(source) {
  if (typeof source === 'string') {
    const text = source.trim()
    if (!text)
      return []
    try {
      return parsePermissionList(JSON.parse(text))
    }
    catch {
      return []
    }
  }
  if (Array.isArray(source))
    return source
  if (source && typeof source === 'object') {
    if (Array.isArray(source.formFieldPermissions))
      return source.formFieldPermissions
    if (Array.isArray(source.fieldPermissions))
      return source.fieldPermissions
    if (Array.isArray(source.fields))
      return source.fields
    return parsePermissionSelections(source)
  }
  return []
}

function parsePermissionSelections(source = {}) {
  const readableFields = readFieldList(source.visibleFields, source.readableFields, source.visible, source.readable)
  const writableFields = readFieldList(source.writableFields, source.editableFields, source.writable, source.editable)
  const requiredFields = readFieldList(source.requiredFields, source.required)
  const fields = new Set([...readableFields, ...writableFields, ...requiredFields])
  if (!fields.size)
    return []
  const hasReadableList = readableFields.length > 0
  const readableSet = new Set(readableFields)
  const writableSet = new Set(writableFields)
  const requiredSet = new Set(requiredFields)
  return Array.from(fields).map(field => ({
    field,
    readable: hasReadableList ? readableSet.has(field) : true,
    writable: writableSet.has(field) || requiredSet.has(field),
    required: requiredSet.has(field),
  }))
}

function readFieldList(...values) {
  for (const value of values) {
    if (Array.isArray(value))
      return value.map(item => String(item || '').trim()).filter(Boolean)
    if (typeof value === 'string') {
      const text = value.trim()
      if (!text)
        continue
      try {
        const parsed = JSON.parse(text)
        if (Array.isArray(parsed))
          return parsed.map(item => String(item || '').trim()).filter(Boolean)
      }
      catch {
        return text.split(',').map(item => item.trim()).filter(Boolean)
      }
    }
  }
  return []
}

function permissionFieldAliases(field) {
  const value = String(field || '').trim()
  if (!value)
    return []
  const aliases = new Set([value, snakeToCamel(value), camelToSnake(value)])
  return Array.from(aliases).filter(Boolean)
}

function snakeToCamel(value) {
  if (!value.includes('_'))
    return value
  return value.replace(/_([a-z0-9])/gi, (_, ch) => ch.toUpperCase())
}

function camelToSnake(value) {
  return value.replace(/[A-Z]/g, ch => `_${ch.toLowerCase()}`)
}

function readPermissionBoolean(value, fallback) {
  if (value === undefined || value === null || value === '')
    return fallback
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  const text = String(value).trim().toLowerCase()
  if (['true', '1', 'yes', 'y'].includes(text))
    return true
  if (['false', '0', 'no', 'n'].includes(text))
    return false
  return fallback
}
