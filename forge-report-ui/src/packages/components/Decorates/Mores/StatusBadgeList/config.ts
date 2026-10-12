import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { StatusBadgeListConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  dataset: [
    { label: '运行中', value: 126, color: '#47ffb2' },
    { label: '待机', value: 18, color: '#25d8ff' },
    { label: '告警', value: 7, color: '#ffcf5a' },
    { label: '停机', value: 3, color: '#ff6b6b' }
  ],
  columns: 4,
  unit: '台',
  backgroundColor: '#061a3a99',
  borderColor: '#1c95ff',
  textColor: '#dceeff',
  mutedColor: '#7ea6c8'
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = StatusBadgeListConfig.key
  public attr = { ...chartInitConfig, w: 520, h: 78, zIndex: -1 }
  public chartConfig = cloneDeep(StatusBadgeListConfig)
  public option = cloneDeep(option)
}
