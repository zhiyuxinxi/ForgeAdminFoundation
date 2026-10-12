import BaseConfig, { option } from '../../Mores/PanelFrame/config'
import { PanelFrameConfig } from './index'
import cloneDeep from 'lodash/cloneDeep'

export { option }

export default class Config extends BaseConfig {
  public key = PanelFrameConfig.key
  public chartConfig = cloneDeep(PanelFrameConfig)
  public option = cloneDeep(option)
}
