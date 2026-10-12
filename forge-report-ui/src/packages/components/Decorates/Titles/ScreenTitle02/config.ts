import BaseConfig, { option as baseOption } from '../ScreenTitle/config'
import { ScreenTitle02Config } from './index'
import cloneDeep from 'lodash/cloneDeep'

export const option = {
  ...cloneDeep(baseOption),
  dataset: '数据综合态势中心',
  subtitle: 'DIGITAL OPERATION CENTER',
  titleMode: 'solid',
  fontColor: '#f8fdff',
  fontSize: 42,
  accentColor: '#00f2ff',
  secondaryColor: '#47ffb2',
  backgroundColor: '#09265388',
  borderColor: '#47ffb2',
  letterSpacing: 5
}

export default class Config extends BaseConfig {
  public key = ScreenTitle02Config.key
  public chartConfig = cloneDeep(ScreenTitle02Config)
  public option = cloneDeep(option)
}
