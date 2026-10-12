import BaseConfig, { option as baseOption } from '../ScreenTitle/config'
import { ScreenTitle04Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  dataset: '生产运营监控中心',
  subtitle: 'MANUFACTURING OPERATION',
  styleVariant: 'blade',
  fontSize: 45,
  accentColor: '#ffcf5a',
  secondaryColor: '#ff6b35',
  gradientFrom: '#fff7d6',
  gradientTo: '#ffcf5a',
  backgroundColor: '#3a210666',
  borderColor: '#ffcf5a'
}

export default class Config extends BaseConfig {
  public key = ScreenTitle04Config.key
  public chartConfig = cloneDeep(ScreenTitle04Config)
  public option = cloneDeep(option)
}
