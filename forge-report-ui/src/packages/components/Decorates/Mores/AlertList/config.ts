import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { AlertListConfig } from './index'

export const option = {
  title: '实时告警',
  dataSource: {
    url: '',
    method: 'get',
    dataPath: 'data',
    paramMap: {}
  },
  items: [
    { level: 'critical', title: '设备离线', time: '19:21', desc: 'A 区 3 号设备超过 5 分钟无心跳' },
    { level: 'warning', title: '库存预警', time: '18:46', desc: '关键物资低于安全库存' },
    { level: 'info', title: '任务提醒', time: '18:10', desc: '巡检任务待处理' }
  ],
  style: {
    accentColor: '#25d8ff',
    criticalColor: '#fb7185',
    warningColor: '#fbbf24',
    infoColor: '#60a5fa',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.78)',
    borderColor: 'rgba(94, 234, 212, 0.22)'
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = AlertListConfig.key
  public attr = { ...chartInitConfig, w: 420, h: 320, zIndex: -1 }
  public chartConfig = cloneDeep(AlertListConfig)
  public option = cloneDeep(option)
}
