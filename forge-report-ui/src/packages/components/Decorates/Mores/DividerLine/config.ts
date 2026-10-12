import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { DividerLineConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  direction: 'horizontal',
  lineStyle: 'solid',
  thickness: 2,
  accentColor: '#25d8ff',
  secondColor: '#47ffb2',
  glow: true,
  showNode: true
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = DividerLineConfig.key
  public attr = { ...chartInitConfig, w: 420, h: 26, zIndex: -1 }
  public chartConfig = cloneDeep(DividerLineConfig)
  public option = cloneDeep(option)
}
