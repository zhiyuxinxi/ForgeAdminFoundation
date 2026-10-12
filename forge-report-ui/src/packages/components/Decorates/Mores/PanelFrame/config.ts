import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { PanelFrameConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  title: '模块标题',
  subtitle: '',
  unit: '',
  fontFamily: '',
  styleVariant: 'corner',
  accentColor: '#25d8ff',
  borderColor: '#1d70ff',
  backgroundColor: '#04163388',
  headerColor: '#dceeff',
  showTitle: true,
  showHeaderLine: true,
  showCorners: false
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = PanelFrameConfig.key
  public attr = { ...chartInitConfig, w: 506, h: 306, zIndex: -2 }
  public chartConfig = cloneDeep(PanelFrameConfig)
  public option = cloneDeep(option)
}
