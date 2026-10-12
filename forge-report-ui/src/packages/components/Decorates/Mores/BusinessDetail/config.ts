import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { BusinessDetailConfig } from './index'

export interface DetailField {
  label: string
  key: string
  span?: number
  type?: 'text' | 'tag' | 'money' | 'date' | 'image' | 'link' | 'progress'
  color?: string
  unit?: string
  urlTemplate?: string
  openTarget?: '_self' | '_blank'
}

export const option = {
  title: '业务详情',
  subtitle: '弹窗和下钻页面信息面板',
  columns: 2,
  api: {
    detailUrl: '',
    method: 'get' as 'get' | 'post',
    dataPath: 'data',
    paramMap: { id: 'id' } as Record<string, string>
  },
  fields: [
    { label: '项目名称', key: 'name', span: 2 },
    { label: '状态', key: 'status', type: 'tag', color: '#34d399' },
    { label: '负责人', key: 'owner' },
    { label: '金额', key: 'amount', type: 'money' },
    { label: '更新时间', key: 'updateTime', type: 'date' }
  ] as DetailField[],
  data: {
    name: '示例项目',
    status: '进行中',
    owner: '张三',
    amount: 128600,
    updateTime: '2026-05-17'
  },
  style: {
    accentColor: '#25d8ff',
    textColor: '#e7f4ff',
    mutedColor: '#7ea6c8',
    panelColor: 'rgba(4, 18, 38, 0.82)',
    borderColor: 'rgba(94, 234, 212, 0.24)',
    radius: 8
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = BusinessDetailConfig.key
  public attr = { ...chartInitConfig, w: 560, h: 320, zIndex: -1 }
  public chartConfig = cloneDeep(BusinessDetailConfig)
  public option = cloneDeep(option)
}
