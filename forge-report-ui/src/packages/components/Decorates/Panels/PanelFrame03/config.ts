import BaseConfig, { option as baseOption } from '../PanelFrame/config'
import { PanelFrame03Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  title: '扫描模块',
  styleVariant: 'scan',
  accentColor: '#25d8ff',
  borderColor: '#1d70ff',
  backgroundColor: '#041a3088'
}

export default class Config extends BaseConfig {
  public key = PanelFrame03Config.key
  public chartConfig = cloneDeep(PanelFrame03Config)
  public option = cloneDeep(option)
}
