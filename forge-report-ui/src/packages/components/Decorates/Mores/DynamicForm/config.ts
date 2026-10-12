import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { DynamicFormConfig } from './index'

export interface FormField {
  label: string
  field: string
  type: 'input' | 'textarea' | 'select' | 'date' | 'number'
  required?: boolean
  options?: Array<{ label: string; value: string | number }>
}

export const option = {
  title: '业务表单',
  submitUrl: '',
  submitMethod: 'post' as 'post' | 'put',
  fields: [
    { label: '名称', field: 'name', type: 'input', required: true },
    { label: '类型', field: 'type', type: 'select', options: [{ label: '默认', value: 'default' }] },
    { label: '日期', field: 'date', type: 'date' }
  ] as FormField[],
  style: {
    accentColor: '#25d8ff',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.82)',
    borderColor: 'rgba(94, 234, 212, 0.22)'
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = DynamicFormConfig.key
  public attr = { ...chartInitConfig, w: 460, h: 360, zIndex: -1 }
  public chartConfig = cloneDeep(DynamicFormConfig)
  public option = cloneDeep(option)
}
