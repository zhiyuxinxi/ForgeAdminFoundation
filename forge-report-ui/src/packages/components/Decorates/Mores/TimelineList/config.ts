import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { TimelineListConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  dataset: [
    { time: '10:23', title: 'CNC-01 温度过高', level: '高', status: 'danger' },
    { time: '10:25', title: 'Robot-03 通信恢复', level: '中', status: 'warning' },
    { time: '10:31', title: '一号产线完成换型', level: '低', status: 'normal' },
    { time: '10:40', title: 'Press-02 压力异常', level: '高', status: 'danger' }
  ],
  accentColor: '#25d8ff',
  warningColor: '#ffcf5a',
  dangerColor: '#ff6b6b',
  textColor: '#dceeff',
  mutedColor: '#7ea6c8',
  backgroundColor: '#061a3a88',
  rowGap: 12
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = TimelineListConfig.key
  public attr = { ...chartInitConfig, w: 440, h: 280, zIndex: -1 }
  public chartConfig = cloneDeep(TimelineListConfig)
  public option = cloneDeep(option)
}
