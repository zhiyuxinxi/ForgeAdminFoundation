export function shouldUseBusinessCodeComponentForm(context = {}) {
  if (context?.configured !== true || context?.formType !== 'business-code')
    return false

  if (context?.formRef?.taskFormRenderMode === 'schema')
    return false

  return Boolean(context?.formUrl || context?.formRef?.formUrl)
}
