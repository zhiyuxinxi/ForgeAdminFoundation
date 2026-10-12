import { cloneDeep } from 'lodash-es'
import { localizeLabel } from '@/locales'

export function useForm(initFormData = {}) {
  const formRef = ref(null)
  const formModel = ref(cloneDeep(initFormData))
  const rules = {
    required: {
      required: true,
      get message() { return localizeLabel('此为必填项') },
      trigger: ['blur', 'change'],
    },
  }
  const validation = () => {
    return formRef.value?.validate()
  }
  return [formRef, formModel, validation, rules]
}
