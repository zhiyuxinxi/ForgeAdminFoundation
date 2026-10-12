import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const WorkflowStatusConfig: ConfigType = {
  key: 'WorkflowStatus',
  chartKey: 'VWorkflowStatus',
  conKey: 'VCWorkflowStatus',
  title: '流程状态',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'process.png'
}
