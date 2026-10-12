import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const StatusBadgeListConfig: ConfigType = {
  key: 'StatusBadgeList',
  chartKey: 'VStatusBadgeList',
  conKey: 'VCStatusBadgeList',
  title: '状态标签组',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'ztbqz.png'
}
