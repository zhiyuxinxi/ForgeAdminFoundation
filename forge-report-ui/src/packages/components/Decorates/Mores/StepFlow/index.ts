import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const StepFlowConfig: ConfigType = {
  key: 'StepFlow',
  chartKey: 'VStepFlow',
  conKey: 'VCStepFlow',
  title: '步骤流程',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'buzhou.png'
}
