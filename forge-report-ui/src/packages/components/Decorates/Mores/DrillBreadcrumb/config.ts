import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { DrillBreadcrumbConfig } from './index'

export const option = {
  items: [
    { label: '首页', pageId: '' },
    { label: '二级页面', pageId: '' }
  ],
  useContextBreadcrumbs: true,
  style: {
    accentColor: '#25d8ff',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.62)',
    borderColor: 'rgba(94, 234, 212, 0.22)'
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = DrillBreadcrumbConfig.key
  public attr = { ...chartInitConfig, w: 520, h: 58, zIndex: -1 }
  public chartConfig = cloneDeep(DrillBreadcrumbConfig)
  public option = cloneDeep(option)
}
