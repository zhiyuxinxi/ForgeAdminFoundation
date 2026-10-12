import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { ScreenTitleConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  dataset: '智慧运营指挥中心',
  subtitle: '',
  styleVariant: 'wing',
  titleMode: 'gradient',
  fontSize: 46,
  fontColor: '#f6fbff',
  fontFamily: '',
  fontWeight: 'bold',
  fontStyle: 'normal',
  letterSpacing: 6,
  gradientFrom: '#ffffff',
  gradientTo: '#28e8ff',
  accentColor: '#23d5ff',
  secondaryColor: '#1f62ff',
  backgroundColor: '#061a3a66',
  borderColor: '#28dfff',
  showBorder: true,
  showBackground: true,
  showDecorations: true,
  glow: true
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = ScreenTitleConfig.key
  public attr = { ...chartInitConfig, w: 900, h: 86, zIndex: 2 }
  public chartConfig = cloneDeep(ScreenTitleConfig)
  public option = cloneDeep(option)
}
