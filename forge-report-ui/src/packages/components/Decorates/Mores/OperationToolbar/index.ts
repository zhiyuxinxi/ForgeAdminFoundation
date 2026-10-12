import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const OperationToolbarConfig: ConfigType = {
  key: 'OperationToolbar',
  chartKey: 'VOperationToolbar',
  conKey: 'VCOperationToolbar',
  title: '操作工具栏',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'icon.png'
}
