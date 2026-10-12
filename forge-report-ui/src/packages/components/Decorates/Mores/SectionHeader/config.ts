import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { SectionHeaderConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  title: '模块标题',
  subtitle: 'SECTION OVERVIEW',
  unit: '',
  accentColor: '#25d8ff',
  secondColor: '#47ffb2',
  textColor: '#f7fbff',
  mutedColor: '#7ea6c8',
  backgroundColor: '#061a3a66',
  showBottomLine: true
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = SectionHeaderConfig.key
  public attr = { ...chartInitConfig, w: 460, h: 46, zIndex: -1 }
  public chartConfig = cloneDeep(SectionHeaderConfig)
  public option = cloneDeep(option)
}
