import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { ProgressRingConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  title: '完成率',
  dataset: 78,
  unit: '%',
  max: 100,
  ringWidth: 12,
  accentColor: '#25d8ff',
  secondColor: '#47ffb2',
  trackColor: '#14304f',
  textColor: '#f7fbff',
  labelColor: '#b8d7ff',
  backgroundColor: '#061a3a99'
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = ProgressRingConfig.key
  public attr = { ...chartInitConfig, w: 180, h: 180, zIndex: -1 }
  public chartConfig = cloneDeep(ProgressRingConfig)
  public option = cloneDeep(option)
}
