import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { DetailCardGroupConfig } from './index'

export const option = {
  columns: 3,
  dataSource: {
    url: '',
    method: 'get',
    dataPath: 'data',
    paramMap: {}
  },
  cards: [
    { label: '项目总数', value: 128, unit: '个', desc: '本月新增 12 个' },
    { label: '处理中', value: 36, unit: '项', desc: '较昨日 +4' },
    { label: '完成率', value: 86, unit: '%', desc: '目标 90%' }
  ],
  style: {
    accentColor: '#25d8ff',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.72)',
    borderColor: 'rgba(94, 234, 212, 0.22)'
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = DetailCardGroupConfig.key
  public attr = { ...chartInitConfig, w: 640, h: 180, zIndex: -1 }
  public chartConfig = cloneDeep(DetailCardGroupConfig)
  public option = cloneDeep(option)
}
