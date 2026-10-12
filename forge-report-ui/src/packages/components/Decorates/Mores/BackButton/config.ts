import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { BackButtonConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  text: '返回上一级',
  subText: 'BACK',
  theme: 'glass' as 'glass' | 'neon' | 'solid' | 'minimal',
  closeModalFirst: true,
  fallbackHome: true,
  accentColor: '#25d8ff',
  textColor: '#f7fbff',
  mutedColor: '#8fb6d6',
  backgroundColor: 'rgba(5, 18, 36, 0.72)',
  borderColor: 'rgba(94, 234, 212, 0.42)',
  radius: 999,
  iconSize: 18,
  fontSize: 14
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = BackButtonConfig.key
  public attr = { ...chartInitConfig, w: 170, h: 44, zIndex: -1 }
  public chartConfig = cloneDeep(BackButtonConfig)
  public option = cloneDeep(option)
}
