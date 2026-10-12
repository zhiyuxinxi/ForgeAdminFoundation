import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { KpiCardConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  title: '核心指标',
  dataset: 12850.6,
  unit: '件',
  precision: 1,
  trendValue: 12.5,
  trendType: 'up',
  trendLabel: '同比',
  iconText: 'KPI',
  accentColor: '#25d8ff',
  warningColor: '#ffcf5a',
  dangerColor: '#ff6b6b',
  backgroundColor: '#061a3acc',
  borderColor: '#1c95ff',
  numberColor: '#f7fbff',
  labelColor: '#b8d7ff',
  showTrend: true
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = KpiCardConfig.key
  public attr = { ...chartInitConfig, w: 320, h: 96, zIndex: -1 }
  public chartConfig = cloneDeep(KpiCardConfig)
  public option = cloneDeep(option)
}
