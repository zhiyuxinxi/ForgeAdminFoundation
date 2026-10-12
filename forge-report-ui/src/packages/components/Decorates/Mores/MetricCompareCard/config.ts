import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { MetricCompareCardConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  title: '营业收入',
  dataset: 5820,
  unit: '万元',
  compareItems: [
    { label: '同比', value: 12.6, type: 'up', unit: '%' },
    { label: '环比', value: 4.8, type: 'up', unit: '%' }
  ],
  precision: 0,
  accentColor: '#25d8ff',
  upColor: '#47ffb2',
  downColor: '#ff6b6b',
  backgroundColor: '#061a3acc',
  borderColor: '#1c95ff',
  numberColor: '#f7fbff',
  labelColor: '#b8d7ff'
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = MetricCompareCardConfig.key
  public attr = { ...chartInitConfig, w: 360, h: 150, zIndex: -1 }
  public chartConfig = cloneDeep(MetricCompareCardConfig)
  public option = cloneDeep(option)
}
