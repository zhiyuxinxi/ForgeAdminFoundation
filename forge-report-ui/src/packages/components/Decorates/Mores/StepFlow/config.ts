import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { StepFlowConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  dataset: [
    { title: '投料', status: 'done' },
    { title: '加工', status: 'active' },
    { title: '质检', status: 'pending' },
    { title: '入库', status: 'pending' }
  ],
  accentColor: '#25d8ff',
  doneColor: '#47ffb2',
  pendingColor: '#315477',
  textColor: '#dceeff',
  mutedColor: '#7ea6c8',
  backgroundColor: '#061a3a66'
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = StepFlowConfig.key
  public attr = { ...chartInitConfig, w: 560, h: 92, zIndex: -1 }
  public chartConfig = cloneDeep(StepFlowConfig)
  public option = cloneDeep(option)
}
