import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { KpiGroupConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  dataset: [
    { title: '今日产量', value: 12850, unit: '件', trend: '+12.5%' },
    { title: '计划完成率', value: 98.2, unit: '%', trend: '+2.1%' },
    { title: '合格率', value: 99.5, unit: '%', trend: '+0.3%' },
    { title: '设备效率', value: 87.6, unit: '%', trend: '+1.8%' }
  ],
  columns: 4,
  accentColor: '#25d8ff',
  secondColor: '#47ffb2',
  backgroundColor: '#061a3acc',
  borderColor: '#1c95ff',
  numberColor: '#f7fbff',
  labelColor: '#b8d7ff',
  mutedColor: '#7ea6c8'
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = KpiGroupConfig.key
  public attr = { ...chartInitConfig, w: 900, h: 112, zIndex: -1 }
  public chartConfig = cloneDeep(KpiGroupConfig)
  public option = cloneDeep(option)
}
