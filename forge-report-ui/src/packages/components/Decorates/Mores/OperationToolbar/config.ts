import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { OperationToolbarConfig } from './index'

export type ToolbarActionType = 'refresh' | 'goPage' | 'openModal' | 'closeModal' | 'request' | 'link'

export interface ToolbarAction {
  label: string
  type: ToolbarActionType
  targetPageId?: string
  url?: string
  method?: 'get' | 'post' | 'put' | 'delete'
  confirm?: boolean
  confirmText?: string
  style?: 'primary' | 'success' | 'warning' | 'error' | 'info'
  openTarget?: '_self' | '_blank'
}

export const option = {
  title: '快捷操作',
  align: 'right' as 'left' | 'center' | 'right',
  actions: [
    { label: '刷新', type: 'refresh', style: 'info' },
    { label: '新增', type: 'openModal', style: 'primary' }
  ] as ToolbarAction[],
  style: {
    accentColor: '#25d8ff',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.74)',
    borderColor: 'rgba(94, 234, 212, 0.22)'
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = OperationToolbarConfig.key
  public attr = { ...chartInitConfig, w: 620, h: 72, zIndex: -1 }
  public chartConfig = cloneDeep(OperationToolbarConfig)
  public option = cloneDeep(option)
}
