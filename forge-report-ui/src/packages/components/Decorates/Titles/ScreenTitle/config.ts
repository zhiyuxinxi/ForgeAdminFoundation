import BaseConfig, { option } from '../../Mores/ScreenTitle/config'
import { ScreenTitleConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export { option }

export default class Config extends BaseConfig {
  public key = ScreenTitleConfig.key
  public chartConfig = cloneDeep(ScreenTitleConfig)
  public option = cloneDeep(option)
}
