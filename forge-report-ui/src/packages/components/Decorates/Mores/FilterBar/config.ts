import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { FilterBarConfig } from './index'

export interface FilterField {
  label: string
  field: string
  type: 'input' | 'select' | 'date'
  options?: Array<{ label: string; value: string | number }>
}

export const option = {
  fields: [
    { label: '区域', field: 'regionCode', type: 'select', options: [{ label: '全部', value: '' }, { label: 'A 区', value: 'A' }] },
    { label: '日期', field: 'date', type: 'date' }
  ] as FilterField[],
  style: {
    accentColor: '#25d8ff',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.76)',
    borderColor: 'rgba(94, 234, 212, 0.22)'
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = FilterBarConfig.key
  public attr = { ...chartInitConfig, w: 620, h: 84, zIndex: -1 }
  public chartConfig = cloneDeep(FilterBarConfig)
  public option = cloneDeep(option)
}
