import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const DetailCardGroupConfig: ConfigType = {
  key: 'DetailCardGroup',
  chartKey: 'VDetailCardGroup',
  conKey: 'VCDetailCardGroup',
  title: '详情卡片组',
  category: ChatCategoryEnum.MORE,
  categoryName: ChatCategoryEnumName.MORE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'box04.png'
}
