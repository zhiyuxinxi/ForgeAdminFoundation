import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { MiniTrendCardConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  title: '实时产量',
  dataset: 12850,
  unit: '件',
  trend: '+12.5%',
  points: [18, 28, 24, 42, 38, 56, 68, 61, 82],
  accentColor: '#25d8ff',
  secondColor: '#47ffb2',
  backgroundColor: '#061a3acc',
  numberColor: '#f7fbff',
  labelColor: '#b8d7ff'
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = MiniTrendCardConfig.key
  public attr = { ...chartInitConfig, w: 320, h: 140, zIndex: -1 }
  public chartConfig = cloneDeep(MiniTrendCardConfig)
  public option = cloneDeep(option)
}
