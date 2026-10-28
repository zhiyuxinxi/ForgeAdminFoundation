import { resolveLegacyChildToolbarAction } from '@/compatibility/presale-child-toolbar-actions'
import { localizeLabel } from '@/locales'
import { buildBusinessActionInitialData, buildBusinessActionInputFormSchema } from './business-action-runtime'

function parseConfig(value) {
  if (typeof value !== 'string')
    return value && typeof value === 'object' ? value : {}
  try {
    return JSON.parse(value) || {}
  }
  catch {
    return {}
  }
}

export function resolveChildToolbarAction({ action = {}, child = {}, childrenData = {}, selectedKey = null } = {}) {
  const config = parseConfig(action.actionConfig)
  const resolvedAction = { ...action, relationKey: config.recordSelector?.relationKey || action.relationKey || config.relationKey || child.relationKey || child.key }
  const legacy = resolveLegacyChildToolbarAction(resolvedAction, null)
  const selector = { ...legacy?.recordSelector, ...config.recordSelector }
  const relationKey = String(config.recordSelector?.relationKey || action.relationKey || config.relationKey || selector.relationKey || child.relationKey || child.key || '').trim()
  const rows = Array.isArray(childrenData[relationKey]) ? childrenData[relationKey] : []
  const records = rows.filter(row => row?.id !== undefined && row?.id !== null && row?.id !== '')
  const selectedRecord = records.find(row => row.id === selectedKey) || null
  const legacyConfig = resolveLegacyChildToolbarAction(resolvedAction, selectedRecord)
  const inputSchema = Array.isArray(config.inputSchema)
    ? config.inputSchema
    : (Array.isArray(config.formSchema) ? undefined : legacyConfig?.inputSchema)
  const formSchema = Array.isArray(inputSchema)
    ? buildBusinessActionInputFormSchema(inputSchema)
    : (Array.isArray(config.formSchema) ? config.formSchema : [])
  const initialData = buildBusinessActionInitialData({ ...config, inputSchema }, selectedRecord || {})
  return {
    relationKey,
    records,
    selectedRecord,
    label: selector.label || localizeLabel('选择记录'),
    placeholder: selector.placeholder || localizeLabel('选择记录'),
    emptyMessage: selector.emptyMessage || localizeLabel('请先保存主记录和子表行'),
    options: records.map(row => ({
      value: row.id,
      label: String(selector.labelField ? (row[selector.labelField] ?? row.id) : (selector.formatLabel?.(row) ?? row.id)),
    })),
    formSchema,
    initialData,
  }
}
