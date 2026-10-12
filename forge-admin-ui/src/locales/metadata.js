import { localizeLabel } from './index'

// Only display properties are copied. IDs, values, defaults, handlers and record data are untouched.
const displayProperties = ['label', 'title', 'placeholder', 'description', 'help', 'tooltip', 'message', 'requiredMessage', 'startPlaceholder', 'endPlaceholder', 'submitText', 'resetText', 'cancelText', 'addButtonText', 'exportButtonText', 'uploadText', 'tabTitle', 'selectorTitle']
const childProperties = ['children', 'actions', 'options', 'rules', 'validate', 'validation', 'props', 'columns', 'fields', 'tabs', 'panes', 'sections', 'formSchema', 'searchSchema', 'detailFields']

export function localizeMetadata(metadata) {
  if (Array.isArray(metadata))
    return metadata.map(localizeMetadata)
  if (!metadata || typeof metadata !== 'object')
    return metadata
  const result = { ...metadata }
  for (const key of displayProperties) {
    if (typeof metadata[key] === 'string')
      result[key] = localizeLabel(metadata[key], metadata[`${key}I18n`])
  }
  for (const key of childProperties) {
    if (metadata[key] && typeof metadata[key] === 'object')
      result[key] = localizeMetadata(metadata[key])
  }
  return result
}
