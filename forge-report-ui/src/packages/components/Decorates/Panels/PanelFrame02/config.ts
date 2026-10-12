import BaseConfig, { option as baseOption } from '../PanelFrame/config'
import { PanelFrame02Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  title: '分析模块',
  styleVariant: 'cut',
  accentColor: '#47ffb2',
  borderColor: '#00c2ff',
  backgroundColor: '#061427aa',
  headerColor: '#f6fbff',
  showCorners: false
}

export default class Config extends BaseConfig {
  public key = PanelFrame02Config.key
  public chartConfig = cloneDeep(PanelFrame02Config)
  public option = cloneDeep(option)
}
