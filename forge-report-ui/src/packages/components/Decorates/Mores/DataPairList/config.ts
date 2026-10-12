import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { DataPairListConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  dataset: [
    { label: '设备编号', value: 'CNC-01' },
    { label: '负责人', value: '张工' },
    { label: '所属区域', value: '一号车间' },
    { label: '更新时间', value: '10:42:18' }
  ],
  columns: 2,
  rowGap: 10,
  accentColor: '#25d8ff',
  backgroundColor: '#061a3a88',
  labelColor: '#7ea6c8',
  valueColor: '#f7fbff'
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = DataPairListConfig.key
  public attr = { ...chartInitConfig, w: 420, h: 160, zIndex: -1 }
  public chartConfig = cloneDeep(DataPairListConfig)
  public option = cloneDeep(option)
}
