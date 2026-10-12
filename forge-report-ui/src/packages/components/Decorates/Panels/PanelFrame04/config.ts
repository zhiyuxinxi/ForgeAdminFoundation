import BaseConfig, { option as baseOption } from '../PanelFrame/config'
import { PanelFrame04Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  title: '网格模块',
  styleVariant: 'grid',
  accentColor: '#47ffb2',
  borderColor: '#00c2ff',
  backgroundColor: '#06251d8c'
}

export default class Config extends BaseConfig {
  public key = PanelFrame04Config.key
  public chartConfig = cloneDeep(PanelFrame04Config)
  public option = cloneDeep(option)
}
