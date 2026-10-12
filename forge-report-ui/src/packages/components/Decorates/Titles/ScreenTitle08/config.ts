import BaseConfig, { option as baseOption } from '../ScreenTitle/config'
import { ScreenTitle08Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  dataset: '数字孪生指挥中心',
  subtitle: 'DIGITAL TWIN COMMAND',
  styleVariant: 'console',
  fontSize: 46,
  accentColor: '#7dd3fc',
  secondaryColor: '#38bdf8',
  backgroundColor: '#082f4988',
  borderColor: '#7dd3fc',
  letterSpacing: 8
}

export default class Config extends BaseConfig {
  public key = ScreenTitle08Config.key
  public chartConfig = cloneDeep(ScreenTitle08Config)
  public option = cloneDeep(option)
}
