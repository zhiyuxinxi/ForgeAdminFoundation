import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { RankProgressListConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  dataset: [
    { name: '一号产线', value: 96.8 },
    { name: '二号产线', value: 89.5 },
    { name: '三号产线', value: 82.2 },
    { name: '四号产线', value: 76.4 },
    { name: '五号产线', value: 68.9 }
  ],
  unit: '%',
  max: 100,
  rowGap: 12,
  accentColor: '#25d8ff',
  secondColor: '#47ffb2',
  textColor: '#dceeff',
  mutedColor: '#7ea6c8',
  trackColor: '#163150',
  showIndex: true
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = RankProgressListConfig.key
  public attr = { ...chartInitConfig, w: 420, h: 280, zIndex: -1 }
  public chartConfig = cloneDeep(RankProgressListConfig)
  public option = cloneDeep(option)
}
