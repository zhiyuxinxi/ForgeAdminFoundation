import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const DrillBreadcrumbConfig: ConfigType = {
  key: 'DrillBreadcrumb',
  chartKey: 'VDrillBreadcrumb',
  conKey: 'VCDrillBreadcrumb',
  title: '下钻面包屑',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.STATIC,
  image: 'buzhou.png'
}
