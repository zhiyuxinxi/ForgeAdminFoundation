import cloneDeep from 'lodash/cloneDeep'
import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { WorkflowStatusConfig } from './index'

export const option = {
  title: '处理进度',
  activeIndex: 1,
  dataSource: {
    url: '',
    method: 'get',
    dataPath: 'data',
    paramMap: {}
  },
  steps: [
    { label: '提交', desc: '已创建', status: 'done' },
    { label: '审核', desc: '处理中', status: 'active' },
    { label: '完成', desc: '待确认', status: 'todo' }
  ],
  style: {
    accentColor: '#25d8ff',
    doneColor: '#34d399',
    todoColor: '#6b8aaa',
    textColor: '#e7f4ff',
    panelColor: 'rgba(4, 18, 38, 0.76)',
    borderColor: 'rgba(94, 234, 212, 0.22)'
  }
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = WorkflowStatusConfig.key
  public attr = { ...chartInitConfig, w: 520, h: 150, zIndex: -1 }
  public chartConfig = cloneDeep(WorkflowStatusConfig)
  public option = cloneDeep(option)
}
