import { computed, ref } from 'vue'
import { businessFlowAppConfig } from '@/api/business-app'

export function useCodeAppMetadata(objectCode) {
  const metadata = ref({})
  const loading = ref(false)
  const error = ref('')

  const fields = computed(() => normalizeFields(metadata.value.fields))
  const fieldMap = computed(() => {
    const map = new Map()
    fields.value.forEach((field) => {
      fieldAliases(field.field).forEach(alias => map.set(alias, field))
    })
    return map
  })
  const viewSchema = computed(() => metadata.value.viewSchema || {})
  const formSchemaFields = computed(() => resolveFormSchemaFields(metadata.value.formDesignerSchema))
  const formSchemaLabels = computed(() => resolveFormSchemaFieldLabels(metadata.value.formDesignerSchema))
  const formSchemaConfigured = computed(() => hasFormSchemaFieldConfig(metadata.value.formDesignerSchema))
  const formSchemaFieldSet = computed(() => new Set(formSchemaFields.value.flatMap(field => fieldAliases(field))))

  async function load() {
    const code = String(objectCode || '').trim()
    if (!code)
      return null
    loading.value = true
    error.value = ''
    try {
      const res = await businessFlowAppConfig(code)
      if (res.code !== 200)
        throw new Error(res.message || '代码应用配置加载失败')
      metadata.value = res.data?.options?.codeAppMetadata || res.data?.formAssets?.codeAppMetadata || {}
      return metadata.value
    }
    catch (e) {
      error.value = e?.message || '代码应用配置加载失败'
      metadata.value = {}
      return null
    }
    finally {
      loading.value = false
    }
  }

  function viewFields(viewKey, fallback = []) {
    const key = String(viewKey || '').toUpperCase()
    if (key === 'FORM')
      return formFields(fallback)
    const configured = resolveViewSchemaFields(key)
    const source = Array.isArray(configured) ? configured : fallback
    const allowed = fields.value.length ? source.filter(field => isPublicField(fieldMap.value.get(field))) : source
    return fields.value.length ? allowed : (allowed.length ? allowed : fallback)
  }

  function formFields(fallback = []) {
    const configured = formSchemaFields.value
    const source = formSchemaConfigured.value ? configured : fallback
    const allowed = fields.value.length ? source.filter(field => isPublicFormField(fieldMap.value.get(field))) : source
    return fields.value.length ? allowed : (allowed.length ? allowed : fallback)
  }

  function resolveViewSchemaFields(key) {
    if (key === 'LIST') {
      const columns = Array.isArray(viewSchema.value?.list?.columns) ? viewSchema.value.list.columns : []
      const fields = columns.filter(item => item?.visible !== false).map(item => item.fieldCode || item.field)
      if (columns.length)
        return normalizeStringList(fields)
    }
    if (key === 'DETAIL') {
      const sections = Array.isArray(viewSchema.value?.detail?.sections) ? viewSchema.value.detail.sections : []
      const configuredItems = sections.flatMap(section => Array.isArray(section.fields) ? section.fields : [])
      const fields = sections
        .filter(section => section?.visible !== false)
        .flatMap(section => Array.isArray(section.fields) ? section.fields : [])
        .filter(item => item?.visible !== false)
        .map(item => item.fieldCode || item.field)
      if (configuredItems.length)
        return normalizeStringList(fields)
    }
    const legacyViews = metadata.value.views || {}
    const legacyFields = legacyViews?.[key]?.fields || legacyViews?.[key.toLowerCase()]?.fields
    return Array.isArray(legacyFields) ? normalizeStringList(legacyFields) : null
  }

  function fieldLabel(field, fallback = '') {
    return fieldMap.value.get(field)?.label || fallback || field
  }

  function formFieldLabel(field, fallback = '') {
    for (const alias of fieldAliases(field)) {
      const label = formSchemaLabels.value.get(alias)
      if (label)
        return label
    }
    return fieldLabel(field, fallback)
  }

  function viewFieldLabel(viewKey, field, fallback = '') {
    const item = findViewFieldConfig(viewSchema.value, viewKey, field)
    return item?.label || fieldLabel(field, fallback)
  }

  function isFieldVisible(field, fallback = true) {
    const config = fieldMap.value.get(field)
    if (!config)
      return fallback
    return isPublicField(config)
  }

  function isFormFieldVisible(field, fallback = true) {
    const config = fieldMap.value.get(field)
    if (!config)
      return fallback
    if (!isPublicFormField(config))
      return false
    return formSchemaConfigured.value ? fieldAliases(field).some(alias => formSchemaFieldSet.value.has(alias)) : true
  }

  return {
    metadata,
    fields,
    fieldMap,
    viewSchema,
    loading,
    error,
    load,
    viewFields,
    formFields,
    fieldLabel,
    formFieldLabel,
    viewFieldLabel,
    isFieldVisible,
    isFormFieldVisible,
  }
}

function normalizeFields(source = []) {
  return (Array.isArray(source) ? source : [])
    .map((field) => {
      const code = String(field?.field || field?.fieldCode || field?.code || '').trim()
      if (!code)
        return null
      return {
        ...field,
        field: code,
        fieldCode: code,
        label: field.label || field.fieldLabel || field.fieldName || code,
        visible: field.visible !== false,
        internal: field.internal === true,
        systemField: field.systemField === true,
      }
    })
    .filter(Boolean)
}

function normalizeStringList(source = []) {
  return Array.from(new Set((Array.isArray(source) ? source : [])
    .map(value => String(value || '').trim())
    .filter(Boolean)))
}

function isPublicField(field) {
  return Boolean(field) && field.visible !== false && field.internal !== true && field.systemField !== true
}

function isPublicFormField(field) {
  return isPublicField(field) && field.formVisible !== false
}

function resolveFormSchemaFields(schema = {}) {
  const result = []
  const seen = new Set()
  function visitSchema(value = {}) {
    if (!value || typeof value !== 'object')
      return
    visitComponents(value.components || [])
    ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitSchema(form?.schema || form))
    ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitSchema(asset?.schema || asset))
  }
  function visitComponents(components = []) {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
      if (component.fieldBinding?.mode === 'field' && fieldCode && component.visibility?.hidden !== true && !seen.has(fieldCode)) {
        seen.add(fieldCode)
        result.push(fieldCode)
      }
      if (Array.isArray(component.children))
        visitComponents(component.children)
    })
  }
  visitSchema(schema)
  return result
}

function resolveFormSchemaFieldLabels(schema = {}) {
  const labels = new Map()
  function visitSchema(value = {}) {
    if (!value || typeof value !== 'object')
      return
    visitComponents(value.components || [])
    ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitSchema(form?.schema || form))
    ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitSchema(asset?.schema || asset))
  }
  function visitComponents(components = []) {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
      const label = String(component.label || '').trim()
      if (component.fieldBinding?.mode === 'field' && fieldCode && label) {
        fieldAliases(fieldCode).forEach((alias) => {
          if (!labels.has(alias))
            labels.set(alias, label)
        })
      }
      if (Array.isArray(component.children))
        visitComponents(component.children)
    })
  }
  visitSchema(schema)
  return labels
}

function hasFormSchemaFieldConfig(schema = {}) {
  let configured = false
  function visitSchema(value = {}) {
    if (configured || !value || typeof value !== 'object')
      return
    configured = hasFieldComponent(value.components || [])
    if (configured) {
      return
    }
    ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitSchema(form?.schema || form))
    ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitSchema(asset?.schema || asset))
  }
  visitSchema(schema)
  return configured
}

function hasFieldComponent(components = []) {
  return (Array.isArray(components) ? components : []).some((component) => {
    if (!component || typeof component !== 'object')
      return false
    if (component.fieldBinding?.mode === 'field' && component.fieldBinding?.fieldCode)
      return true
    return hasFieldComponent(component.children || [])
  })
}

function findViewFieldConfig(viewSchema = {}, viewKey, field) {
  const key = String(viewKey || '').toUpperCase()
  const targetAliases = new Set(fieldAliases(field))
  if (key === 'LIST') {
    const columns = Array.isArray(viewSchema?.list?.columns) ? viewSchema.list.columns : []
    return columns.find(item => targetAliases.has(item?.fieldCode || item?.field))
  }
  if (key === 'DETAIL') {
    const sections = Array.isArray(viewSchema?.detail?.sections) ? viewSchema.detail.sections : []
    return sections
      .flatMap(section => Array.isArray(section.fields) ? section.fields : [])
      .find(item => targetAliases.has(item?.fieldCode || item?.field))
  }
  return null
}

function fieldAliases(field) {
  const value = String(field || '').trim()
  if (!value)
    return []
  return Array.from(new Set([value, snakeToCamel(value), camelToSnake(value)])).filter(Boolean)
}

function snakeToCamel(value) {
  if (!value.includes('_')) {
    return value
  }
  return value.replace(/_([a-z0-9])/gi, (_, ch) => ch.toUpperCase())
}

function camelToSnake(value) {
  return value.replace(/[A-Z]/g, ch => `_${ch.toLowerCase()}`)
}
