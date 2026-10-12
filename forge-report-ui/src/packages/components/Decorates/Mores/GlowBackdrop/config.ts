import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { GlowBackdropConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  variant: 'reactor',
  accentColor: '#25d8ff',
  secondColor: '#47ffb2',
  thirdColor: '#ffcf5a',
  backgroundColor: '#02081700',
  opacity: 0.9,
  rotate: 0,
  animate: true
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = GlowBackdropConfig.key
  public attr = { ...chartInitConfig, w: 720, h: 420, zIndex: -2 }
  public chartConfig = cloneDeep(GlowBackdropConfig)
  public option = cloneDeep(option)
}
